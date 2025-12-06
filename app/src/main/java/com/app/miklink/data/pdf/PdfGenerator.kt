package com.app.miklink.data.pdf

import android.content.Context
import android.print.PrintDocumentAdapter
import android.webkit.WebView
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {

    private data class ColumnFlags(
        val ping: Boolean,
        val tdr: Boolean,
        val speed: Boolean,
        val showCpuWarning: Boolean
    )

    private fun scanColumnsAndCpu(reports: List<Report>): ColumnFlags {
        var hasPing = false
        var hasTdr = false
        var hasSpeed = false
        var cpuWarn = false

        reports.forEach { r ->
            val parsed = parseResults(r.resultsJson)
            if (!hasPing) hasPing = (parsed?.ping?.isNotEmpty() == true)
            if (!hasTdr) hasTdr = (parsed?.tdr?.isNotEmpty() == true)
            if (!hasSpeed) hasSpeed = (parsed?.speedTest != null)

            parsed?.speedTest?.let { st ->
                val probe = listOf(
                    st.status, st.ping, st.jitter, st.loss, st.tcpDownload, st.tcpUpload, st.udpDownload, st.udpUpload, st.warning
                ).joinToString(" ") { it ?: "" }
                if (!cpuWarn) {
                    cpuWarn = probe.contains("local-cpu-load:100%", ignoreCase = true) ||
                            probe.contains("remote-cpu-load:100%", ignoreCase = true)
                }
            }
        }
        return ColumnFlags(ping = hasPing, tdr = hasTdr, speed = hasSpeed, showCpuWarning = cpuWarn)
    }

    private fun buildTableHeaders(flags: ColumnFlags): String {
        val base = listOf("Presa", "Data/Ora", "Stato", "Neighbor")
        val dynamic = buildList {
            if (flags.ping) add("Ping")
            if (flags.tdr) add("TDR")
            if (flags.speed) add("Speed Test")
        }
        return (base + dynamic).joinToString(separator = "") { "<th>${it}</th>" }
    }

    private fun cleanCpuStrings(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val patterns = listOf(
            Pattern.compile("local-cpu-load:[^,)<]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("remote-cpu-load:[^,)<]+", Pattern.CASE_INSENSITIVE)
        )
        var out = input
        patterns.forEach { p -> out = p.matcher(out ?: "").replaceAll("") }
        return out?.replace(Regex("[ ]{2,}"), " ")?.trim()?.trim(',') ?: ""
    }

    private fun buildTableRows(
        reports: List<Report>,
        flags: ColumnFlags
    ): String {
        val sb = StringBuilder()
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        reports.forEach { report ->
            val parsed = parseResults(report.resultsJson)
            val presa = report.socketName.orEmpty()
            val datetime = df.format(Date(report.timestamp))
            val status = report.overallStatus
            val statusClass = if (status.equals("PASS", true)) "status-pass" else "status-fail"
            val neighbor = parsed?.lldp?.firstOrNull()?.let { n ->
                listOfNotNull(n.identity, n.interfaceName).joinToString(" / ")
            }.orEmpty()

            val pingValue = if (flags.ping) {
                parsed?.ping?.lastOrNull()?.let { p ->
                    val avg = p.avgRtt ?: ""
                    val loss = p.packetLoss ?: ""
                    listOfNotNull(if (avg.isNotBlank()) "avg ${avg}" else null, if (loss.isNotBlank()) "loss ${loss}%" else null)
                        .joinToString(" • ")
                }.orEmpty()
            } else null

            val tdrValue = if (flags.tdr) {
                parsed?.tdr?.firstOrNull()?.status.orEmpty()
            } else null

            val speedValue = if (flags.speed) {
                parsed?.speedTest?.let { st ->
                    val d = cleanCpuStrings(st.tcpDownload)
                    val u = cleanCpuStrings(st.tcpUpload)
                    listOfNotNull(if (d.isNotBlank()) d else null, if (u.isNotBlank()) u else null)
                        .joinToString(" / ")
                }.orEmpty()
            } else null

            sb.append("<tr>")
            sb.append("<td>").append(presa).append("</td>")
            sb.append("<td>").append(datetime).append("</td>")
            sb.append("<td class=\"status-cell\"><span class=\"").append(statusClass).append("\">").append(status).append("</span></td>")
            sb.append("<td>").append(neighbor).append("</td>")
            if (flags.ping) sb.append("<td>").append(pingValue).append("</td>")
            if (flags.tdr) sb.append("<td>").append(tdrValue).append("</td>")
            if (flags.speed) sb.append("<td>").append(speedValue).append("</td>")
            sb.append("</tr>")
        }
        return sb.toString()
    }

    private fun loadAssetText(fileName: String): String {
        context.assets.open(fileName).use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                return reader.readText()
            }
        }
    }

    private fun buildDashboardHtml(reports: List<Report>, client: Client?, reportTitle: String? = null): String {
        var template = loadAssetText("project_report_template.html")
        
        // Replace title if provided for proper PDF filename
        if (!reportTitle.isNullOrBlank()) {
            template = template.replace("<title>Report Certificazione Rete</title>", "<title>$reportTitle</title>")
        }
        
        val reportsSorted = reports.sortedBy { it.timestamp }
        val flags = scanColumnsAndCpu(reportsSorted)

        val total = reportsSorted.size
        val passed = reportsSorted.count { it.overallStatus.equals("PASS", true) }
        val failed = total - passed
        
        // Extract Probe Info from first report
        val firstReport = reportsSorted.firstOrNull()
        val parsedFirst = firstReport?.let { parseResults(it.resultsJson) }
        val lldpInfo = parsedFirst?.lldp?.firstOrNull()
        val probeModel = lldpInfo?.systemDescription ?: "N/A"
        val probeInterface = lldpInfo?.portId ?: lldpInfo?.interfaceName ?: "N/A"

        return template
            .replace("{{CLIENT_NAME}}", client?.companyName ?: "N/A")
            .replace("{{CLIENT_LOCATION}}", client?.location ?: "-")
            .replace("{{NETWORK_MODE}}", client?.networkMode ?: "Auto")
            .replace("{{MIN_LINK_RATE}}", client?.minLinkRate ?: "-")
            .replace("{{REPORT_ID}}", firstReport?.reportId?.toString() ?: "N/A")
            .replace("{{REPORT_DATE}}", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            .replace("{{PROBE_NAME}}", firstReport?.probeName ?: "Sonda")
            .replace("{{PROBE_MODEL}}", probeModel)
            .replace("{{PROBE_INTERFACE}}", probeInterface)
            .replace("{{TEST_PROFILE}}", firstReport?.profileName ?: "Standard")
            .replace("{{TOTAL_TESTS}}", total.toString())
            .replace("{{PASSED_TESTS}}", passed.toString())
            .replace("{{FAILED_TESTS}}", failed.toString())
            .replace("{{TABLE_HEADERS}}", buildTableHeaders(flags))
            .replace("{{TABLE_ROWS}}", buildTableRows(reportsSorted, flags))
            .replace("{{CLIENT_NOTES}}", if (!client?.notes.isNullOrBlank()) "<div class=\"notes-section\"><div class=\"notes-title\">Note Cliente</div><div class=\"notes-content\">${client.notes}</div></div>" else "")
            .replace("{{GENERATION_TIMESTAMP}}", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
            .replace(
                "{{FOOTER_WARNING}}",
                if (flags.showCpuWarning) {
                    "<div class=\"warn\">⚠️ Rilevato carico CPU 100% (locale/remota) durante alcuni Speed Test. I valori potrebbero essere sottostimati.</div>"
                } else ""
            )
    }

    // TASK 1 - Funzione pubblica: genera la stringa HTML finale dal dominio
    fun generateHtmlFromReports(reports: List<Report>, client: Client?, reportTitle: String? = null): String {
        return buildDashboardHtml(reports, client, reportTitle)
    }

    // TASK 1 - Funzione pubblica: crea SOLO l'adapter di stampa
    // NOTA: usa Activity Context per garantire funzionalità di stampa
    suspend fun createPrintAdapter(context: Context, htmlContent: String, jobName: String): PrintDocumentAdapter {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            android.util.Log.d("PdfGenerator", "Creating print adapter with jobName: $jobName")
            
            // Use Activity Context (critical for print functionality)
            val webView = WebView(context)
            
            // CRITICAL: Disable hardware acceleration to prevent renderer crashes on some devices (Xiaomi/MIUI)
            webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            
            webView.settings.apply {
                javaScriptEnabled = false
                loadWithOverviewMode = true
                useWideViewPort = true
                domStorageEnabled = true
            }
            
            // Track page load completion
            val pageLoaded = java.util.concurrent.atomic.AtomicBoolean(false)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    android.util.Log.d("PdfGenerator", "WebView page finished loading")
                    pageLoaded.set(true)
                }
                
                // Handle both deprecated and new error callbacks for compatibility
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    android.util.Log.e("PdfGenerator", "WebView error (legacy): $errorCode - $description")
                    // Don't call super to avoid default error page
                }
                
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    android.util.Log.e("PdfGenerator", "WebView error: ${error?.errorCode} - ${error?.description}")
                    // Don't call super to avoid default error page
                }
            }
            
            // Load HTML content
            android.util.Log.d("PdfGenerator", "Loading HTML content (${htmlContent.length} chars)")
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            
            // Wait for page load
            val startTime = System.currentTimeMillis()
            val timeout = 5000L
            while (!pageLoaded.get() && (System.currentTimeMillis() - startTime) < timeout) {
                kotlinx.coroutines.delay(50)
            }
            
            if (!pageLoaded.get()) {
                android.util.Log.w("PdfGenerator", "WebView load timeout after ${System.currentTimeMillis() - startTime}ms")
            } else {
                val loadTime = System.currentTimeMillis() - startTime
                android.util.Log.d("PdfGenerator", "WebView loaded in ${loadTime}ms, waiting 500ms for rendering")
                kotlinx.coroutines.delay(500) // Extra rendering delay
            }
            
            // Create the base print adapter
            val webAdapter = webView.createPrintDocumentAdapter(jobName)
            
            // Wrap the adapter to keep WebView alive and handle cleanup
            object : PrintDocumentAdapter() {
                // Keep a strong reference to WebView to prevent GC during the print process
                private val keptWebView = webView
                
                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: android.print.PrintDocumentAdapter.LayoutResultCallback,
                    extras: android.os.Bundle?
                ) {
                    webAdapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                }
                
                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: android.print.PrintDocumentAdapter.WriteResultCallback?
                ) {
                    try {
                        webAdapter.onWrite(pages, destination, cancellationSignal, callback)
                    } catch (e: Exception) {
                        android.util.Log.e("PdfGenerator", "Error in onWrite", e)
                        callback?.onWriteFailed(e.message)
                    }
                }
                
                override fun onFinish() {
                    webAdapter.onFinish()
                    // Clean up WebView only after printing is finished
                    try { keptWebView.destroy() } catch (_: Throwable) {}
                    super.onFinish()
                }
            }
        }
    }

    // Utility: word wrap per test unitari e potenziale uso futuro
    internal fun wrapText(text: String, maxWidth: Float, paint: android.graphics.Paint): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth && currentLine.isNotEmpty()) {
                // chiudi linea corrente e riparti con la parola
                lines.add(currentLine)
                currentLine = word
            } else if (width > maxWidth && currentLine.isEmpty()) {
                // parola singola più lunga della riga: mettila da sola
                lines.add(word)
                currentLine = ""
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    /**
     * `internal` per test unitari.
     * Parsa il JSON dei risultati del report in un oggetto strutturato.
     */
    internal fun parseResults(json: String): ParsedResults? {
        if (json.isBlank()) return null

        // 1) Tentativo di parsing diretto
        var parsed: ParsedResults? = null
        try {
            parsed = moshi.adapter(ParsedResults::class.java).fromJson(json)
            // Se già completo, restituisco subito
            if (parsed != null && parsed.ping != null && parsed.tdr != null) return parsed
        } catch (_: Exception) {
            // Ignora: passeremo alla normalizzazione legacy
        }

        // 2) Fallback: normalizzazione compatibilità legacy
        return try {
            val mapType = com.squareup.moshi.Types.newParameterizedType(
                Map::class.java, String::class.java, Any::class.java
            )
            val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, Any?>> = moshi.adapter(mapType)
            val root = mapAdapter.fromJson(json)

            if (root == null) return parsed

            var pingList: MutableList<com.app.miklink.data.network.PingResult>? = null

            // Colleziona chiavi ping_* se ping è mancante o vuoto
            if (parsed?.ping.isNullOrEmpty()) {
                val listType = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java, com.app.miklink.data.network.PingResult::class.java
                )
                val pingListAdapter: com.squareup.moshi.JsonAdapter<List<com.app.miklink.data.network.PingResult>> = moshi.adapter(listType)
                root.forEach { (key, value) ->
                    if (key.startsWith("ping_")) {
                        var items = pingListAdapter.fromJsonValue(value) ?: emptyList()
                        if (items.isEmpty()) {
                            // Fallback manuale: mappa lista di mappe -> PingResult
                            val rawList = (value as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
                            if (rawList.isNotEmpty()) {
                                items = rawList.map { m ->
                                    com.app.miklink.data.network.PingResult(
                                        avgRtt = m["avg-rtt"] as? String,
                                        host = m["host"] as? String,
                                        maxRtt = m["max-rtt"] as? String,
                                        minRtt = m["min-rtt"] as? String,
                                        packetLoss = m["packet-loss"] as? String,
                                        received = m["received"] as? String,
                                        sent = m["sent"] as? String,
                                        seq = m["seq"] as? String,
                                        size = m["size"] as? String,
                                        time = m["time"] as? String,
                                        ttl = m["ttl"] as? String
                                    )
                                }
                            }
                        }
                        if (items.isNotEmpty()) {
                            if (pingList == null) pingList = mutableListOf()
                            pingList!!.addAll(items)
                        }
                    }
                }
            }

            // TDR: se manca o è oggetto singolo, wrappa
            var tdrList: List<com.app.miklink.data.network.CableTestResult>? = parsed?.tdr
            if (tdrList == null) {
                val tdrVal = root["tdr"]
                when (tdrVal) {
                    is Map<*, *> -> {
                        val tdrAdapter: com.squareup.moshi.JsonAdapter<com.app.miklink.data.network.CableTestResult> =
                            moshi.adapter(com.app.miklink.data.network.CableTestResult::class.java)
                        val single = tdrAdapter.fromJsonValue(tdrVal)
                        if (single != null) tdrList = listOf(single)
                    }
                    is List<*> -> {
                        val listType = com.squareup.moshi.Types.newParameterizedType(
                            List::class.java, com.app.miklink.data.network.CableTestResult::class.java
                        )
                        val listAdapter: com.squareup.moshi.JsonAdapter<List<com.app.miklink.data.network.CableTestResult>> = moshi.adapter(listType)
                        tdrList = listAdapter.fromJsonValue(tdrVal)
                    }
                }
            }

            if (pingList == null && tdrList == null) parsed else ParsedResults(
                tdr = tdrList ?: parsed?.tdr,
                link = parsed?.link,
                lldp = parsed?.lldp,
                ping = pingList ?: parsed?.ping,
                speedTest = parsed?.speedTest
            )
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error parsing results JSON (legacy)", e)
            parsed
        }
    }
}
