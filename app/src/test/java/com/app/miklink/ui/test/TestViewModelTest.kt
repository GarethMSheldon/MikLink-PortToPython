/*
 * Purpose: Validate TestViewModel maps domain events to UI state flows for sections, reports, and logs.
 * Inputs: Synthetic TestEvent streams emitted via a fake RunTestUseCase and SavedStateHandle navigation args.
 * Outputs: Assertions on section status progression, UiState transitions, and log accumulation.
 */
package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestOutcome
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.model.TestProgress
import com.app.miklink.core.domain.test.model.TestSectionResult
import com.app.miklink.core.domain.usecase.test.RunTestUseCase
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.usecase.report.SaveTestReportUseCase
import com.app.miklink.testsupport.MainDispatcherRule
import com.app.miklink.utils.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reportRepository = object : SaveTestReportUseCase {
        private val state = kotlinx.coroutines.flow.MutableStateFlow<List<TestReport>>(emptyList())

        override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long {
            val id = (state.value.maxOfOrNull { it.reportId } ?: 0L) + 1L
            val r = report.copy(reportId = id)
            state.value = state.value + r
            return id
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sections state updates progressively from domain events`() = runTest {
        val events = MutableSharedFlow<TestEvent>()
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = events
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "clientId" to 1L,
                "profileId" to 1L,
                "socketName" to "A1"
            )
        )

        val viewModel = TestViewModel(savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        advanceUntilIdle()

        val pendingSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "PENDING"),
            TestSectionResult(type = "LINK", title = "Link", status = "PENDING")
        )
        events.emit(TestEvent.SectionsUpdated(pendingSections))
        advanceUntilIdle()

        assertEquals(2, viewModel.sections.value.size)
        assertTrue(viewModel.sections.value.all { it.status == "PENDING" })

        val runningSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "RUNNING"),
            TestSectionResult(type = "LINK", title = "Link", status = "PENDING")
        )
        events.emit(TestEvent.SectionsUpdated(runningSections))
        advanceUntilIdle()

        val currentStatuses = viewModel.sections.value.associate { it.type.name to it.status }
        assertEquals("RUNNING", currentStatuses["NETWORK"])
        assertEquals("PENDING", currentStatuses["LINK"])

        val finalSections = listOf(
            TestSectionResult(type = "NETWORK", title = "Network", status = "PASS"),
            TestSectionResult(type = "LINK", title = "Link", status = "FAIL", details = mapOf("error" to "link down"))
        )
        events.emit(TestEvent.SectionsUpdated(finalSections))
        advanceUntilIdle()

        events.emit(
            TestEvent.Completed(
                TestOutcome(
                    overallStatus = "FAIL",
                    sections = finalSections,
                    rawResultsJson = "{}"
                )
            )
        )
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is UiState.Success<TestReport>)
        val sectionsStatuses = viewModel.sections.value.map { it.status }
        assertEquals(listOf("PASS", "FAIL"), sectionsStatuses)

        viewModel.viewModelScope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `logs are collected from progress and log line events`() = runTest {
        val events = MutableSharedFlow<TestEvent>()
        val useCase = object : RunTestUseCase {
            override fun execute(plan: TestPlan): Flow<TestEvent> = events
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "clientId" to 1L,
                "profileId" to 1L,
                "socketName" to "A1"
            )
        )

        val viewModel = TestViewModel(savedStateHandle, useCase, reportRepository)

        viewModel.startTest()
        advanceUntilIdle()

        events.emit(TestEvent.Progress(TestProgress("Init", 0, "starting setup")))
        events.emit(TestEvent.LogLine("Sanitized log line"))
        advanceUntilIdle()

        assertEquals(listOf("[Init] starting setup", "Sanitized log line"), viewModel.logs.value)

        viewModel.viewModelScope.cancel()
    }
}
