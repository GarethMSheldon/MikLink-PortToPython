package com.app.miklink.data.pdf

import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.db.model.TestProfile
import java.io.File

/**
 * Interface contract for PDF generation implementations.
 * Keep the contract small and testable so different generators (iText, HTML, etc.)
 * can be swapped with DI and covered by unit tests.
 */
interface PdfGenerator {
    fun generatePdfReport(rawReports: List<Report>, client: Client?, config: PdfExportConfig): File?

    fun generateSingleTestPdf(report: Report, client: Client?, profile: TestProfile?, reportTitle: String): File?
}
