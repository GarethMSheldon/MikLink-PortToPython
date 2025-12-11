package com.app.miklink.data.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.squareup.moshi.Moshi
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.ExportColumn
import com.app.miklink.data.pdf.PdfPageOrientation
import com.app.miklink.data.pdf.PdfExportConfig
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class PdfGeneratorSnapshotTest {

    // Helper to extract plain text from pdf bytes using iText
    private fun extractTextFromPdf(file: File): String {
        val reader = PdfReader(FileInputStream(file))
        val pdf = PdfDocument(reader)
        val sb = StringBuilder()
        val numPages = pdf.numberOfPages
        for (i in 1..numPages) {
            val page = pdf.getPage(i)
            val strategy = com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy()
            sb.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page, strategy))
            sb.append("\n")
        }
        pdf.close()
        return sb.toString()
    }

    @Test
    fun `generated PDF contains valid ASCII strings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val moshi = Moshi.Builder().build()
        val generator = PdfGeneratorIText(context, moshi)
        // create a minimal report; use a simple PdfExportConfig
        val config = PdfExportConfig(
            title = "TestReport",
            includeEmptyTests = true,
            columns = listOf(ExportColumn.DATE, ExportColumn.STATUS),
            showSignatures = false,
            signatureLeftLabel = "",
            signatureRightLabel = "",
            orientation = PdfPageOrientation.PORTRAIT,
            hideEmptyColumns = false
        )
        val report = Report(
            reportId = 0L,
            clientId = null,
            timestamp = System.currentTimeMillis(),
            socketName = "",
            notes = "",
            probeName = "TestProbe",
            profileName = "Default",
            overallStatus = "PASS",
            resultsJson = "{\"dummy\":true, \"ping\":[1,2,3]}"
        )
        val client = Client(
            clientId = 0L,
            companyName = "TestClient",
            location = "",
            notes = "",
            networkMode = "DHCP",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "1G",
            socketPrefix = "",
            socketSuffix = "",
            socketSeparator = "",
            socketNumberPadding = 3,
            nextIdNumber = 1,
            lastFloor = null,
            lastRoom = null,
            speedTestServerAddress = null,
            speedTestServerUser = null,
            speedTestServerPassword = null
        )
        val file = generator.generatePdfReport(listOf(report), client = client, config = config)
        assertNotNull("PDF generator returned null file", file)
        val text = extractTextFromPdf(file!!)
        assertTrue(text.contains("Dettaglio Test") || text.contains("Dettaglio"))
        // no control characters (0x00-0x1F, excluding newline/tab)
        val controlChars = text.any { ch -> ch.code in 0x00..0x08 || ch.code in 0x0B..0x0C || ch.code in 0x0E..0x1F }
        assertFalse("PDF contains unexpected control characters", controlChars)
    }
}
