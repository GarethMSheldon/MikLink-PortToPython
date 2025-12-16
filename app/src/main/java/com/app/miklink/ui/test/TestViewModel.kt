/*
 * Purpose: Orchestrate test execution UI state and delegate report persistence through domain use cases.
 * Inputs: SavedStateHandle navigation args (clientId, profileId, socketName) and RunTestUseCase events.
 * Outputs: UiState/TestSection/log flows for the UI and persisted reports via SaveTestReportUseCase.
 * Notes: Keeps UI free from repository details; persistence policy (Socket-ID increment) lives in the use case; log buffer is UI-only.
 */
package com.app.miklink.ui.test

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.test.logging.ExecutionLogBuffer
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class TestViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val runTestUseCase: RunTestUseCase,
    private val saveTestReportUseCase: SaveTestReportUseCase
) : ViewModel() {

    // Logs removed from UI; keep Execution state only

    private val _uiState = MutableStateFlow<UiState<TestReport>>(UiState.Idle)
    val uiState: StateFlow<UiState<TestReport>> = _uiState.asStateFlow()

    private val _sections = MutableStateFlow<List<TestSection>>(emptyList())
    val sections: StateFlow<List<TestSection>> = _sections.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val logBuffer = ExecutionLogBuffer()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun startTest() {
        if (_isRunning.value) return

        val plan = buildPlan() ?: return

        viewModelScope.launch {
            _sections.value = emptyList()
            _uiState.value = UiState.Loading
            _isRunning.value = true
            logBuffer.clear()
            _logs.value = emptyList()

            runTestUseCase.execute(plan)
                .catch { throwable ->
                    handleFailure(throwable.message)
                }
                .collect { event ->
                    when (event) {
                        is TestEvent.Progress -> appendLog("[${event.progress.currentStep}] ${event.progress.message}")
                        is TestEvent.LogLine -> appendLog(event.message)
                        is TestEvent.SectionsUpdated -> _sections.value = mapSections(event.sections)
                        is TestEvent.Completed -> handleCompletion(plan, event.outcome)
                        is TestEvent.Failed -> handleFailure(event.error.message)
                    }
                }
        }
    }

    fun saveReportToDb(report: TestReport) {
        viewModelScope.launch {
            saveTestReportUseCase(report, incrementClientCounter = true)
        }
    }

    private fun handleCompletion(plan: TestPlan, outcome: TestOutcome) {
        _isRunning.value = false
        _sections.value = mapSections(outcome.sections)
        val report = buildReport(plan, outcome)
        _uiState.value = UiState.Success(report)
    }

    private fun handleFailure(message: String?) {
        _isRunning.value = false
        val errorMessage = message ?: "Errore sconosciuto"
        _uiState.value = UiState.Error(errorMessage)
    }

    private fun buildPlan(): TestPlan? {
        val clientId = readId("clientId")
        val profileId = readId("profileId")
        val socketNameRaw = savedStateHandle.get<String>("socketName") ?: ""
        val socketName = try {
            Uri.decode(socketNameRaw)
        } catch (_: Exception) {
            socketNameRaw
        }

        if (clientId <= 0 || profileId <= 0) {
            _uiState.value = UiState.Error("Parametri di navigazione non validi.")
            return null
        }

        return TestPlan(
            clientId = clientId,
            profileId = profileId,
            socketId = socketName,
            notes = null
        )
    }

    private fun buildReport(plan: TestPlan, outcome: TestOutcome): TestReport {
        return TestReport(
            reportId = 0L,
            clientId = plan.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = plan.socketId,
            notes = plan.notes,
            probeName = null,
            profileName = null,
            overallStatus = outcome.overallStatus,
            resultFormatVersion = 1,
            resultsJson = outcome.rawResultsJson ?: "{}"
        )
    }

    private fun mapSections(results: List<TestSectionResult>): List<TestSection> {
        return results.map { result ->
            val type = mapSectionType(result.type)
            TestSection(
                category = mapCategory(type),
                type = type,
                title = result.title.ifBlank { type.name },
                status = result.status,
                details = result.details.map { (label, value) -> TestDetail(label = label, value = value) }
            )
        }
    }

    private fun mapSectionType(rawType: String): TestSectionType {
        val normalized = rawType.trim().uppercase(Locale.ROOT)
        return TestSectionType.values().firstOrNull { it.name == normalized } ?: TestSectionType.NETWORK
    }

    private fun mapCategory(type: TestSectionType): TestSectionCategory {
        return when (type) {
            TestSectionType.NETWORK, TestSectionType.LLDP -> TestSectionCategory.INFO
            else -> TestSectionCategory.TEST
        }
    }

    private fun appendLog(line: String) {
        logBuffer.append(line)
        _logs.value = logBuffer.snapshot()
    }

    private fun readId(key: String): Long {
        return savedStateHandle.get<Long>(key)
            ?: savedStateHandle.get<String>(key)?.toLongOrNull()
            ?: -1L
    }

    // appendLog removed: raw execution logs are not surfaced to the UI anymore
}
