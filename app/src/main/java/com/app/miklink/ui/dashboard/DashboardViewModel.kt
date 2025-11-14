package com.app.miklink.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.app.miklink.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientDao: ClientDao,
    probeConfigDao: ProbeConfigDao,
    testProfileDao: TestProfileDao,
    private val reportDao: ReportDao, // Injected ReportDao
    private val repository: AppRepository
) : ViewModel() {

    // Data sources
    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val probes: StateFlow<List<ProbeConfig>> = probeConfigDao.getAllProbes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User selections
    val selectedClient = MutableStateFlow<Client?>(null)
    val selectedProbe = MutableStateFlow<ProbeConfig?>(null)
    val selectedProfile = MutableStateFlow<TestProfile?>(null)
    val socketName = MutableStateFlow("")

    val isProbeOnline: StateFlow<Boolean> = selectedProbe.flatMapLatest { probe ->
        if (probe == null) {
            flowOf(false)
        } else {
            repository.observeProbeStatus(probe)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        // Observe selected client to auto-increment socket ID
        viewModelScope.launch {
            selectedClient.collect { client ->
                if (client != null) {
                    val lastReport = reportDao.getLastReportForClient(client.clientId)
                    val nextNumber = if (lastReport == null) {
                        1
                    } else {
                        // Safely handle nullable socketName
                        val lastNumber = lastReport.socketName?.removePrefix(client.socketPrefix)?.toIntOrNull() ?: 0
                        lastNumber + 1
                    }
                    // Format with 3-digit padding
                    socketName.value = "${client.socketPrefix}${String.format("%03d", nextNumber)}"
                } else {
                    socketName.value = ""
                }
            }
        }
    }
}