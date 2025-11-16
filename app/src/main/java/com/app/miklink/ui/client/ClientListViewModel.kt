package com.app.miklink.ui.client

import android.print.PrintDocumentAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val clientDao: ClientDao,
    private val reportDao: ReportDao,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pdfStatus = MutableStateFlow("")
    val pdfStatus = _pdfStatus.asStateFlow()

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            clientDao.delete(client)
        }
    }

    // Nuove API usate dalla UI per la stampa
    suspend fun generateHtmlForClientId(clientId: Long): String? {
        val reports = reportDao.getReportsForClient(clientId).firstOrNull() ?: emptyList()
        val client = clientDao.getClientById(clientId).firstOrNull()
        if (reports.isEmpty()) return null
        return pdfGenerator.generateHtmlFromReports(reports, client)
    }

    fun createPrintAdapter(html: String, jobName: String): PrintDocumentAdapter =
        pdfGenerator.createPrintAdapter(html, jobName)
}
