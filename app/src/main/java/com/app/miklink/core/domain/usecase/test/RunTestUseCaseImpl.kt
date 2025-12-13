package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import java.util.LinkedHashMap

/**
 * Implementazione di RunTestUseCase.
 * Orchestra tutti gli step necessari per eseguire un test completo.
 * 
 * Ordine degli step (replicato da TestViewModel):
 * 1. Network Config
 * 2. Link Status
 * 3. TDR
 * 4. LLDP
 * 5. Ping
 * 6. Speed Test
 */
class RunTestUseCaseImpl @Inject constructor(
    private val clientRepository: ClientRepository,
    private val probeRepository: ProbeRepository,
    private val testProfileRepository: TestProfileRepository,
    private val networkConfigStep: NetworkConfigStep,
    private val linkStatusStep: LinkStatusStep,
    private val cableTestStep: CableTestStep,
    private val neighborDiscoveryStep: NeighborDiscoveryStep,
    private val pingStep: PingStep,
    private val speedTestStep: SpeedTestStep,
    private val moshi: Moshi
) : RunTestUseCase {

    private val rawResultsAdapter: JsonAdapter<RawTestResults> = moshi.adapter(RawTestResults::class.java)

    override fun execute(plan: TestPlan): Flow<TestEvent> = flow {
        // 1. Carica entità
        val client = clientRepository.getClient(plan.clientId)
            ?: throw IllegalStateException("Client not found: ${plan.clientId}")
        val probe = probeRepository.getProbe(plan.probeId)
            ?: throw IllegalStateException("Probe not found: ${plan.probeId}")
        val profile = testProfileRepository.getProfile(plan.profileId)
            ?: throw IllegalStateException("Profile not found: ${plan.profileId}")

        val context = TestExecutionContext(
            client = client,
            probeConfig = probe,
            profile = profile,
            socketId = plan.socketId,
            notes = plan.notes
        )

        emit(TestEvent.LogLine("--- INIZIO TEST ---"))
        emit(TestEvent.Progress(TestProgress("Inizializzazione", 0, "Caricamento dati...")))

        val sections = buildInitialSections(profile, probe)
        val rawSteps = mutableListOf<RawStep>()
        var overallStatus = "PASS"

        suspend fun emitSectionsSnapshot() {
            emit(TestEvent.SectionsUpdated(sectionsSnapshot(sections)))
        }

        fun setSectionStatus(
            type: String,
            status: String,
            details: Map<String, String> = emptyMap(),
            title: String? = null
        ) {
            val index = sections.indexOfFirst { it.type == type }
            if (index >= 0) {
                val current = sections[index]
                sections[index] = current.copy(
                    title = title ?: current.title,
                    status = status,
                    details = details.toMap()
                )
            } else {
                sections.add(
                    TestSectionResult(
                        type = type,
                        title = title ?: type,
                        status = status,
                        details = details.toMap()
                    )
                )
            }
        }

        fun recordStep(
            name: String,
            title: String,
            status: String,
            details: Map<String, String> = emptyMap(),
            rawData: Map<String, Any?>? = null,
            error: String? = null
        ) {
            setSectionStatus(name, status, details, title)
            rawSteps.add(RawStep(name = name, status = status, data = rawData, error = error))
        }

        emitSectionsSnapshot()

        try {
            // 1) Network Config
            emit(TestEvent.LogLine("Applicazione configurazione rete..."))
            emit(TestEvent.Progress(TestProgress("Network Config", 10, "Configurazione rete in corso...")))

            setSectionStatus(SECTION_NETWORK, STATUS_RUNNING)
            emitSectionsSnapshot()
            
            val networkResult = networkConfigStep.run(context)
            when (networkResult) {
                is StepResult.Success -> {
                    val feedback = networkResult.data as NetworkConfigFeedback
                    recordStep(
                        name = SECTION_NETWORK,
                        title = "Network",
                        status = STATUS_PASS,
                        details = networkDetails(feedback),
                        rawData = networkRaw(feedback)
                    )
                    emitSectionsSnapshot()
                    emit(TestEvent.LogLine("Rete configurata con successo"))
                }
                is StepResult.Failed -> {
                    overallStatus = "FAIL"
                    val errorMessage = networkResult.error.message
                    recordStep(
                        name = SECTION_NETWORK,
                        title = "Network",
                        status = STATUS_FAIL,
                        details = mapOf("error" to (errorMessage ?: "Unknown error")),
                        error = errorMessage
                    )
                    emitSectionsSnapshot()
                    emit(TestEvent.LogLine("Configurazione rete fallita: $errorMessage"))
                }
                is StepResult.Skipped -> {
                    recordStep(
                        name = SECTION_NETWORK,
                        title = "Network",
                        status = STATUS_SKIP,
                        details = mapOf("reason" to networkResult.reason),
                        rawData = mapOf("reason" to networkResult.reason)
                    )
                    emitSectionsSnapshot()
                }
            }

            // 2) Link Status
            if (profile.runLinkStatus) {
                emit(TestEvent.LogLine("Esecuzione Test Stato Link..."))
                emit(TestEvent.Progress(TestProgress("Link Status", 30, "Verifica stato link...")))

                setSectionStatus(SECTION_LINK, STATUS_RUNNING)
                emitSectionsSnapshot()

                val linkResult = linkStatusStep.run(context)
                when (linkResult) {
                    is StepResult.Success -> {
                        val monitor = linkResult.data as MonitorResponse
                        recordStep(
                            name = SECTION_LINK,
                            title = "Link",
                            status = STATUS_PASS,
                            details = linkDetails(monitor),
                            rawData = linkRaw(monitor)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Link Status: OK"))
                    }
                    is StepResult.Failed -> {
                        overallStatus = "FAIL"
                        val errorMessage = linkResult.error.message
                        recordStep(
                            name = SECTION_LINK,
                            title = "Link",
                            status = STATUS_FAIL,
                            details = mapOf("error" to (errorMessage ?: "Errore sconosciuto")),
                            error = errorMessage
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Link Status: FALLITO"))
                        // Stop immediato su errore link
                        emit(TestEvent.Failed(linkResult.error))
                        return@flow
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = SECTION_LINK,
                            title = "Link",
                            status = STATUS_SKIP,
                            details = mapOf("reason" to linkResult.reason),
                            rawData = mapOf("reason" to linkResult.reason)
                        )
                        emitSectionsSnapshot()
                    }
                }
            } else {
                recordStep(
                    name = SECTION_LINK,
                    title = "Link",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED),
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSectionsSnapshot()
            }

            // 3) TDR
            if (profile.runTdr && probe.tdrSupported) {
                emit(TestEvent.LogLine("Esecuzione TDR (Cable-Test)..."))
                emit(TestEvent.Progress(TestProgress("TDR", 50, "Test cavo in corso...")))

                setSectionStatus(SECTION_TDR, STATUS_RUNNING)
                emitSectionsSnapshot()

                val tdrResult = cableTestStep.run(context)
                when (tdrResult) {
                    is StepResult.Success -> {
                        val cableTest = tdrResult.data as CableTestResult
                        recordStep(
                            name = SECTION_TDR,
                            title = "TDR",
                            status = STATUS_PASS,
                            details = tdrDetails(cableTest),
                            rawData = tdrRaw(cableTest)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("TDR: SUCCESSO"))
                    }
                    is StepResult.Failed -> {
                        // Non bloccare il test se TDR fallisce per incompatibilità hardware
                        val isFatal = tdrResult.error is TestError.Unsupported
                        if (!isFatal) overallStatus = "FAIL"
                        val status = if (isFatal) STATUS_SKIP else STATUS_FAIL
                        val message = tdrResult.error.message
                        val details = if (isFatal) {
                            mapOf(
                                "reason" to TestSkipReason.HARDWARE_UNSUPPORTED,
                                "error" to (message ?: "Errore sconosciuto")
                            )
                        } else {
                            mapOf("error" to (message ?: "Errore sconosciuto"))
                        }
                        recordStep(
                            name = SECTION_TDR,
                            title = "TDR",
                            status = status,
                            details = details,
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("TDR: ${if (isFatal) "NON SUPPORTATO" else "FALLITO"}"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = SECTION_TDR,
                            title = "TDR",
                            status = STATUS_SKIP,
                            details = mapOf("reason" to tdrResult.reason),
                            rawData = mapOf("reason" to tdrResult.reason)
                        )
                        emitSectionsSnapshot()
                    }
                }
            } else if (profile.runTdr && !probe.tdrSupported) {
                emit(TestEvent.LogLine("TDR: SALTATO (hardware non supporta TDR)"))
                recordStep(
                    name = SECTION_TDR,
                    title = "TDR",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.HARDWARE_UNSUPPORTED),
                    rawData = mapOf("reason" to TestSkipReason.HARDWARE_UNSUPPORTED)
                )
                emitSectionsSnapshot()
            } else {
                recordStep(
                    name = SECTION_TDR,
                    title = "TDR",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED),
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSectionsSnapshot()
            }

            // 4) LLDP
            if (profile.runLldp) {
                emit(TestEvent.LogLine("Esecuzione discovery LLDP/CDP..."))
                emit(TestEvent.Progress(TestProgress("LLDP", 60, "Discovery neighbor...")))

                setSectionStatus(SECTION_LLDP, STATUS_RUNNING)
                emitSectionsSnapshot()

                val lldpResult = neighborDiscoveryStep.run(context)
                when (lldpResult) {
                    is StepResult.Success -> {
                        val neighbors = lldpResult.data as List<*>
                        recordStep(
                            name = SECTION_LLDP,
                            title = "LLDP/CDP",
                            status = STATUS_PASS,
                            details = lldpDetails(neighbors),
                            rawData = lldpRaw(neighbors)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("LLDP/CDP: Rilevato neighbor"))
                    }
                    is StepResult.Failed -> {
                        val message = lldpResult.error.message
                        recordStep(
                            name = SECTION_LLDP,
                            title = "LLDP/CDP",
                            status = "INFO",
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("LLDP/CDP: FALLITO (${lldpResult.error.message})"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = SECTION_LLDP,
                            title = "LLDP/CDP",
                            status = STATUS_SKIP,
                            details = mapOf("reason" to lldpResult.reason),
                            rawData = mapOf("reason" to lldpResult.reason)
                        )
                        emitSectionsSnapshot()
                    }
                }
            } else {
                recordStep(
                    name = SECTION_LLDP,
                    title = "LLDP/CDP",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED),
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSectionsSnapshot()
            }

            // 5) Ping
            if (profile.runPing) {
                emit(TestEvent.LogLine("Esecuzione Ping..."))
                emit(TestEvent.Progress(TestProgress("Ping", 70, "Test ping in corso...")))

                setSectionStatus(SECTION_PING, STATUS_RUNNING)
                emitSectionsSnapshot()

                val pingResult = pingStep.run(context)
                when (pingResult) {
                    is StepResult.Success -> {
                        val outcomes = pingResult.data as List<*>
                        recordStep(
                            name = SECTION_PING,
                            title = "Ping",
                            status = STATUS_PASS,
                            details = pingDetails(outcomes),
                            rawData = pingRaw(outcomes)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Ping: SUCCESSO"))
                    }
                    is StepResult.Failed -> {
                        overallStatus = "FAIL"
                        val message = pingResult.error.message
                        recordStep(
                            name = SECTION_PING,
                            title = "Ping",
                            status = STATUS_FAIL,
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Ping: FALLITO"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = SECTION_PING,
                            title = "Ping",
                            status = STATUS_SKIP,
                            details = mapOf("reason" to pingResult.reason),
                            rawData = mapOf("reason" to pingResult.reason)
                        )
                        emitSectionsSnapshot()
                    }
                }
            } else {
                recordStep(
                    name = SECTION_PING,
                    title = "Ping",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED),
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSectionsSnapshot()
            }

            // 6) Speed Test
            if (profile.runSpeedTest) {
                emit(TestEvent.LogLine("Esecuzione Speed Test..."))
                emit(TestEvent.Progress(TestProgress("Speed Test", 90, "Speed test in corso...")))

                setSectionStatus(SECTION_SPEED, STATUS_RUNNING)
                emitSectionsSnapshot()

                val speedResult = speedTestStep.run(context)
                when (speedResult) {
                    is StepResult.Success -> {
                        val speed = speedResult.data as SpeedTestResult
                        recordStep(
                            name = SECTION_SPEED,
                            title = "Speed Test",
                            status = STATUS_PASS,
                            details = speedDetails(speed, client.speedTestServerAddress),
                            rawData = speedRaw(speed, client.speedTestServerAddress)
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Speed Test: COMPLETATO"))
                    }
                    is StepResult.Failed -> {
                        // Non fail l'intero test per uno speed test fallito (opzionale)
                        val message = speedResult.error.message
                        recordStep(
                            name = SECTION_SPEED,
                            title = "Speed Test",
                            status = STATUS_FAIL,
                            details = mapOf("error" to (message ?: "Errore sconosciuto")),
                            rawData = mapOf("error" to message),
                            error = message
                        )
                        emitSectionsSnapshot()
                        emit(TestEvent.LogLine("Speed Test: FALLITO"))
                    }
                    is StepResult.Skipped -> {
                        recordStep(
                            name = SECTION_SPEED,
                            title = "Speed Test",
                            status = STATUS_SKIP,
                            details = mapOf("reason" to speedResult.reason),
                            rawData = mapOf("reason" to speedResult.reason)
                        )
                        emitSectionsSnapshot()
                    }
                }
            } else {
                recordStep(
                    name = SECTION_SPEED,
                    title = "Speed Test",
                    status = STATUS_SKIP,
                    details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED),
                    rawData = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
                )
                emitSectionsSnapshot()
            }

            emit(TestEvent.LogLine("--- TEST COMPLETATO ---"))
            emit(TestEvent.Progress(TestProgress("Completato", 100, "Test completato")))

            val outcome = TestOutcome(
                overallStatus = overallStatus,
                sections = sections,
                rawResultsJson = buildRawResults(plan, rawSteps)
            )

            emit(TestEvent.Completed(outcome))

        } catch (e: Exception) {
            emit(TestEvent.LogLine("ERRORE IRREVERSIBILE: ${e.message}"))
            emit(TestEvent.Failed(TestError.Unexpected(e.message ?: "Unknown error", e)))
        }
    }

    private fun buildRawResults(plan: TestPlan, rawSteps: List<RawStep>): String {
        val payload = RawTestResults(
            timestamp = System.currentTimeMillis(),
            plan = RawPlan(
                clientId = plan.clientId,
                probeId = plan.probeId,
                profileId = plan.profileId,
                socketId = plan.socketId
            ),
            steps = rawSteps
        )
        return try {
            rawResultsAdapter.toJson(payload)
        } catch (_: Exception) {
            "{}"
        }
    }

    private fun networkDetails(feedback: NetworkConfigFeedback): Map<String, String> =
        linkedMapOf(
            "mode" to feedback.mode,
            "interface" to feedback.interfaceName,
            "address" to (feedback.address ?: "-"),
            "gateway" to (feedback.gateway ?: "-"),
            "dns" to (feedback.dns ?: "-"),
            "message" to feedback.message
        )

    private fun networkRaw(feedback: NetworkConfigFeedback): Map<String, Any?> =
        linkedMapOf(
            "mode" to feedback.mode,
            "interface" to feedback.interfaceName,
            "address" to feedback.address,
            "gateway" to feedback.gateway,
            "dns" to feedback.dns,
            "message" to feedback.message
        )

    private fun linkDetails(response: MonitorResponse): Map<String, String> =
        linkedMapOf(
            "status" to response.status,
            "rate" to (response.rate ?: "-")
        )

    private fun linkRaw(response: MonitorResponse): Map<String, Any?> =
        linkedMapOf(
            "status" to response.status,
            "rate" to response.rate
        )

    private fun tdrDetails(result: CableTestResult): Map<String, String> =
        linkedMapOf(
            "status" to result.status,
            "pairs" to (result.cablePairs?.size?.toString() ?: "0")
        )

    private fun tdrRaw(result: CableTestResult): Map<String, Any?> =
        linkedMapOf(
            "status" to result.status,
            "cablePairs" to result.cablePairs
        )

    private fun lldpDetails(neighbors: List<*>): Map<String, String> {
        val first = neighbors.firstOrNull() as? NeighborDetail
        return linkedMapOf(
            "count" to neighbors.size.toString(),
            "identity" to (first?.identity ?: "-"),
            "interface" to (first?.interfaceName ?: "-"),
            "protocol" to (first?.discoveredBy ?: "-")
        )
    }

    private fun lldpRaw(neighbors: List<*>): Map<String, Any?> =
        linkedMapOf(
            "neighbors" to neighbors
        )

    private fun pingDetails(results: List<*>): Map<String, String> {
        val outcomes = results.filterIsInstance<PingTargetOutcome>()
        if (outcomes.isEmpty()) return mapOf("status" to "Nessun target valido")
        val summary = outcomes.joinToString("; ") { outcome ->
            val status = when {
                outcome.error != null -> "ERR"
                outcome.packetLoss == null -> "SKIP"
                outcome.packetLoss.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.let { it > 0.0 } == true -> "LOSS"
                else -> "OK"
            }
            "${outcome.target}:$status"
        }
        return linkedMapOf("targets" to summary)
    }

    private fun pingRaw(results: List<*>): Map<String, Any?> =
        linkedMapOf(
            "targets" to results
        )

    private fun speedDetails(speed: SpeedTestResult, serverAddress: String?): Map<String, String> =
        linkedMapOf(
            "server" to (serverAddress ?: "-"),
            "tcpDownload" to (speed.tcpDownload ?: "-"),
            "tcpUpload" to (speed.tcpUpload ?: "-"),
            "udpDownload" to (speed.udpDownload ?: "-"),
            "udpUpload" to (speed.udpUpload ?: "-"),
            "ping" to (speed.ping ?: "-"),
            "jitter" to (speed.jitter ?: "-"),
            "loss" to (speed.loss ?: "-"),
            "warning" to (speed.warning ?: "")
        ).filterValues { it.isNotEmpty() }

    private fun speedRaw(speed: SpeedTestResult, serverAddress: String?): Map<String, Any?> =
        linkedMapOf(
            "server" to serverAddress,
            "status" to speed.status,
            "ping" to speed.ping,
            "jitter" to speed.jitter,
            "loss" to speed.loss,
            "tcpDownload" to speed.tcpDownload,
            "tcpUpload" to speed.tcpUpload,
            "udpDownload" to speed.udpDownload,
            "udpUpload" to speed.udpUpload,
            "warning" to speed.warning
        )
}

private data class RawPlan(
    val clientId: Long,
    val probeId: Long,
    val profileId: Long,
    val socketId: String?
)

private data class RawStep(
    val name: String,
    val status: String,
    val data: Map<String, Any?>? = null,
    val error: String? = null
)

private data class RawTestResults(
    val timestamp: Long,
    val plan: RawPlan,
    val steps: List<RawStep>
)

private const val STATUS_PENDING = "PENDING"
private const val STATUS_RUNNING = "RUNNING"
private const val STATUS_PASS = "PASS"
private const val STATUS_FAIL = "FAIL"
private const val STATUS_SKIP = "SKIP"

private const val SECTION_NETWORK = "NETWORK"
private const val SECTION_LINK = "LINK"
private const val SECTION_TDR = "TDR"
private const val SECTION_LLDP = "LLDP"
private const val SECTION_PING = "PING"
private const val SECTION_SPEED = "SPEED"

private fun buildInitialSections(profile: TestProfile, probe: ProbeConfig): MutableList<TestSectionResult> {
    val sections = mutableListOf<TestSectionResult>()
    sections += TestSectionResult(type = SECTION_NETWORK, title = "Network", status = STATUS_PENDING)

    sections += if (profile.runLinkStatus) {
        TestSectionResult(type = SECTION_LINK, title = "Link", status = STATUS_PENDING)
    } else {
        TestSectionResult(
            type = SECTION_LINK,
            title = "Link",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
        )
    }

    sections += when {
        profile.runTdr && probe.tdrSupported -> TestSectionResult(type = SECTION_TDR, title = "TDR", status = STATUS_PENDING)
        profile.runTdr && !probe.tdrSupported -> TestSectionResult(
            type = SECTION_TDR,
            title = "TDR",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.HARDWARE_UNSUPPORTED)
        )
        else -> TestSectionResult(
            type = SECTION_TDR,
            title = "TDR",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
        )
    }

    sections += if (profile.runLldp) {
        TestSectionResult(type = SECTION_LLDP, title = "LLDP/CDP", status = STATUS_PENDING)
    } else {
        TestSectionResult(
            type = SECTION_LLDP,
            title = "LLDP/CDP",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
        )
    }

    sections += if (profile.runPing) {
        TestSectionResult(type = SECTION_PING, title = "Ping", status = STATUS_PENDING)
    } else {
        TestSectionResult(
            type = SECTION_PING,
            title = "Ping",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
        )
    }

    sections += if (profile.runSpeedTest) {
        TestSectionResult(type = SECTION_SPEED, title = "Speed Test", status = STATUS_PENDING)
    } else {
        TestSectionResult(
            type = SECTION_SPEED,
            title = "Speed Test",
            status = STATUS_SKIP,
            details = mapOf("reason" to TestSkipReason.PROFILE_DISABLED)
        )
    }

    return sections
}

private fun sectionsSnapshot(source: List<TestSectionResult>): List<TestSectionResult> {
    return source.map { section ->
        section.copy(details = LinkedHashMap(section.details))
    }
}

