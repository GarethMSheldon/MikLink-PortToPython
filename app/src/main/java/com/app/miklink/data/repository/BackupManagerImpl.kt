package com.app.miklink.data.repository

import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManagerImpl @Inject constructor(
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val clientRepository: ClientRepository,
    private val reportRepository: ReportRepository,
    private val moshi: Moshi,
    private val txRunner: com.app.miklink.data.repository.TransactionRunner
) : BackupManager {

    override suspend fun exportConfigToJson(): String {
        val probe = probeRepository.getProbeConfig()
        val profiles = testProfileRepository.observeAllProfiles().first()
        val clients = clientRepository.observeAllClients().first()
        val reports = reportRepository.observeAllReports().first()
        fun clientKeyFor(client: com.app.miklink.core.domain.model.Client): String {
            val name = client.companyName.trim().lowercase().replace("\\s+".toRegex(), "_")
            val loc = (client.location ?: "").trim().lowercase().replace("\\s+".toRegex(), "_")
            return "$name|$loc"
        }

        val backupClients = clients.map { client ->
            com.app.miklink.data.repository.BackupClient(
                companyName = client.companyName,
                location = client.location,
                notes = client.notes,
                networkMode = client.networkMode,
                staticIp = client.staticIp,
                staticSubnet = client.staticSubnet,
                staticGateway = client.staticGateway,
                staticCidr = client.staticCidr,
                minLinkRate = client.minLinkRate,
                socketPrefix = client.socketPrefix,
                socketSuffix = client.socketSuffix,
                socketSeparator = client.socketSeparator,
                socketNumberPadding = client.socketNumberPadding,
                nextIdNumber = client.nextIdNumber,
                speedTestServerAddress = client.speedTestServerAddress,
                speedTestServerUser = client.speedTestServerUser,
                speedTestServerPassword = client.speedTestServerPassword
            ,
            clientKey = clientKeyFor(client)
            )
        }
        // Map existing clientId -> clientKey for reports
        val clientIdToKey = clients.associateBy({ it.clientId }, { clientKeyFor(it) })

        val backupReports = reports.map { report ->
            com.app.miklink.data.repository.BackupReport(
                timestamp = report.timestamp,
                socketName = report.socketName,
                notes = report.notes,
                probeName = report.probeName,
                profileName = report.profileName,
                overallStatus = report.overallStatus,
                resultFormatVersion = report.resultFormatVersion,
                resultsJson = report.resultsJson,
                clientKey = report.clientId?.let { clientIdToKey[it] }
            )
        }
        val backupData = com.app.miklink.data.repository.BackupData(
            probe = probe,
            clients = backupClients,
            profiles = profiles,
            reports = backupReports
        )
        val adapter = moshi.adapter(BackupData::class.java)
        return adapter.toJson(backupData)
    }

    override suspend fun importConfigFromJson(json: String): Result<Unit> {
        val adapter = moshi.adapter(BackupData::class.java)
        val backupData = try { adapter.fromJson(json) } catch (e: Exception) { null }
        if (backupData == null) return Result.failure(Exception("JSON malformato"))
        // Delegate to a method that imports the domain backup data atomically
        return importBackupData(backupData)
    }

    override suspend fun importBackupData(backupData: BackupData): Result<Unit> {
        // Basic validation
        if (backupData.probe != null) {
            if (backupData.probe.ipAddress.isBlank() || backupData.probe.username.isBlank()) {
                return Result.failure(Exception("Dati sonda incompleti"))
            }
        }
        if (backupData.profiles.any { it.profileName.isBlank() }) {
            return Result.failure(Exception("Dati profilo incompleti"))
        }
        if (backupData.clients.any { it.companyName.isBlank() }) {
            return Result.failure(Exception("Dati client incompleti"))
        }
        if (backupData.reports.any { it.resultsJson.isBlank() }) {
            return Result.failure(Exception("Dati report incompleti"))
        }

        // Pre-export backup (so we can restore if needed)
        val currentBackupJson = exportConfigToJson()

        // Run import inside transaction
        return try {
            txRunner.runInTransaction {
                // Delete all existing profiles
                testProfileRepository.observeAllProfiles().first().forEach { profile ->
                    testProfileRepository.deleteProfile(profile)
                }


                // Delete all existing clients
                clientRepository.observeAllClients().first().forEach { client ->
                    clientRepository.deleteClient(client)
                }

                // Delete all existing reports
                reportRepository.observeAllReports().first().forEach { report ->
                    reportRepository.deleteReport(report)
                }

                // Save singleton probe if present (keep existing if null)
                backupData.probe?.let { probeRepository.saveProbeConfig(it) }

                // Insert all clients and build clientKey -> newId map
                val clientKeyToNewId = mutableMapOf<String, Long>()
                backupData.clients.forEach { client ->
                    val newId = clientRepository.insertClient(
                        com.app.miklink.core.domain.model.Client(
                            clientId = 0L,
                            companyName = client.companyName,
                            location = client.location,
                            notes = client.notes,
                            networkMode = client.networkMode,
                            staticIp = client.staticIp,
                            staticSubnet = client.staticSubnet,
                            staticGateway = client.staticGateway,
                            staticCidr = client.staticCidr,
                            minLinkRate = client.minLinkRate,
                            socketPrefix = client.socketPrefix,
                            socketSuffix = client.socketSuffix,
                            socketSeparator = client.socketSeparator,
                            socketNumberPadding = client.socketNumberPadding,
                            nextIdNumber = client.nextIdNumber,
                            speedTestServerAddress = client.speedTestServerAddress,
                            speedTestServerUser = client.speedTestServerUser,
                            speedTestServerPassword = client.speedTestServerPassword
                        )
                    )
                    clientKeyToNewId[client.clientKey] = newId
                }

                // Insert all profiles
                backupData.profiles.forEach { profile ->
                    testProfileRepository.insertProfile(profile)
                }

                // Insert all reports and map clientKey -> new clientId if available
                backupData.reports.forEach { r ->
                    val clientId = r.clientKey?.let { clientKeyToNewId[it] }
                    reportRepository.saveReport(
                        com.app.miklink.core.domain.model.TestReport(
                            reportId = 0L,
                            clientId = clientId,
                            timestamp = r.timestamp,
                            socketName = r.socketName,
                            notes = r.notes,
                            probeName = r.probeName,
                            profileName = r.profileName,
                            overallStatus = r.overallStatus,
                            resultFormatVersion = r.resultFormatVersion,
                            resultsJson = r.resultsJson
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Optional automatic rollback by trying to restore previous backup
            try {
                val adapter = moshi.adapter(BackupData::class.java)
                val originalBackup = adapter.fromJson(currentBackupJson)
                if (originalBackup != null) {
                    txRunner.runInTransaction {
                        testProfileRepository.observeAllProfiles().first().forEach { profile ->
                            testProfileRepository.deleteProfile(profile)
                        }
                        originalBackup.probe?.let { probeRepository.saveProbeConfig(it) }
                        originalBackup.profiles.forEach { profile ->
                            testProfileRepository.insertProfile(profile)
                        }
                    }
                }
            } catch (ex: Exception) {
                // swallow second-level rollback exception but log if needed
            }
            Result.failure(e)
        }
    }
}
