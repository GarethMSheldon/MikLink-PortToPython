/*
 * Purpose: Persist a test report and apply the Socket-ID increment policy for successful run-test flows.
 * Inputs: TestReport to save and a flag indicating whether the client counter should be updated.
 * Outputs: Database identifier of the saved report.
 * Notes: Duplication/import flows should call the repository directly to avoid unintended increments.
 */
package com.app.miklink.core.domain.usecase.report

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.domain.model.TestReport
import javax.inject.Inject

interface SaveTestReportUseCase {
    suspend operator fun invoke(report: TestReport, incrementClientCounter: Boolean = true): Long
}

class SaveTestReportUseCaseImpl @Inject constructor(
    private val reportRepository: ReportRepository,
    private val clientRepository: ClientRepository
) : SaveTestReportUseCase {
    override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long {
        val id = reportRepository.saveReport(report)

        val shouldIncrement = incrementClientCounter && report.overallStatus.equals("PASS", ignoreCase = true)
        if (shouldIncrement) {
            val clientId = report.clientId
            if (clientId != null) {
                val client = clientRepository.getClient(clientId)
                if (client != null) {
                    val updated = client.copy(nextIdNumber = client.nextIdNumber + 1)
                    clientRepository.updateClient(updated)
                }
            }
        }

        return id
    }
}
