package com.app.miklink.ui.probe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProbeListViewModel @Inject constructor(
    probeStatusRepository: ProbeStatusRepository
) : ViewModel() {

    val probes: StateFlow<List<ProbeStatusInfo>> = probeStatusRepository.observeAllProbesWithStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
