package com.app.miklink.ui.history

import android.print.PrintDocumentAdapter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Report
import com.app.miklink.data.pdf.PdfGenerator
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportDao: ReportDao,
    private val clientDao: ClientDao,
    private val pdfGenerator: PdfGenerator, // Injected dependency
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle
) : ViewModel(), ReportDetailScreenStateProvider {

    private val reportId: Long = savedStateHandle.get<Long>("reportId") ?: -1L

    override val report: StateFlow<Report?> = reportDao.getReportById(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val socketName = MutableStateFlow("")
    override val notes = MutableStateFlow("")

    private val _parsedResults = MutableStateFlow<ParsedResults?>(null)
    override val parsedResults: StateFlow<ParsedResults?> = _parsedResults.asStateFlow()

    private val _pdfStatus = MutableStateFlow("")
    override val pdfStatus: StateFlow<String> = _pdfStatus.asStateFlow()

    init {
        viewModelScope.launch {
            report.collectLatest { currentReport ->
                if (currentReport != null) {
                    socketName.value = currentReport.socketName ?: ""
                    notes.value = currentReport.notes ?: ""
                    _parsedResults.value = parseResults(currentReport.resultsJson)
                }
            }
        }
    }

    override fun updateReportDetails() {
        viewModelScope.launch {
            report.value?.let {
                val updatedReport = it.copy(socketName = socketName.value, notes = notes.value)
                reportDao.update(updatedReport)
            }
        }
    }

    // Sospeso: recupera client e costruisce HTML
    suspend fun generateHtmlForCurrentReport(): String? {
        val currentReport = report.value ?: return null
        val client = currentReport.clientId?.let { id -> clientDao.getClientById(id).firstOrNull() }
        return pdfGenerator.generateHtmlFromReports(listOf(currentReport), client)
    }

    fun createPrintAdapter(html: String, jobName: String): PrintDocumentAdapter =
        pdfGenerator.createPrintAdapter(html, jobName)

    override fun exportReportToPdf() {
        // No-op: la stampa è demandata alla UI
        _pdfStatus.value = ""
    }

    private fun parseResults(json: String): ParsedResults? {
        return try {
            moshi.adapter(ParsedResults::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
