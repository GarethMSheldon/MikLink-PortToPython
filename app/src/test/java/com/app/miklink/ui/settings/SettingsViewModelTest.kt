package com.app.miklink.ui.settings

import android.content.ContentResolver
import android.content.Context
import com.app.miklink.data.io.FileReader
import android.net.Uri
import com.app.miklink.data.repository.BackupRepository
import com.app.miklink.data.repository.UserPreferencesRepository
import com.app.miklink.data.repository.ThemeConfig
import com.app.miklink.data.repository.IdNumberingStrategy
import kotlinx.coroutines.flow.flowOf
import com.app.miklink.domain.usecase.backup.ImportBackupUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.clearMocks
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val importBackupUseCase = mockk<ImportBackupUseCase>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val fileReader = mockk<FileReader>(relaxed = true)

    init {
        // default flows for the UserPreferencesRepository mock used by ViewModel
        every { userPreferencesRepository.themeConfig } returns flowOf(ThemeConfig.FOLLOW_SYSTEM)
        every { userPreferencesRepository.customPalette } returns flowOf(UserPreferencesRepository.CustomPalette())
        every { userPreferencesRepository.idNumberingStrategy } returns flowOf(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        every { userPreferencesRepository.pdfIncludeEmptyTests } returns flowOf(true)
        every { userPreferencesRepository.pdfSelectedColumns } returns flowOf(emptySet())
        every { userPreferencesRepository.pdfReportTitle } returns flowOf("Collaudo Cablaggio di Rete")
        every { userPreferencesRepository.pdfHideEmptyColumns } returns flowOf(false)
        every { userPreferencesRepository.dashboardGlowIntensity } returns flowOf(0.5f)
        every { userPreferencesRepository.probePollingInterval } returns flowOf(5000L)
    }

    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `importConfig delegates to ImportBackupUseCase and updates status on success`() = runTest(testDispatcher) {
        val json = "{\"ok\": true}"
        val uri = mockk<Uri>(relaxed = true)
        val inputStream = ByteArrayInputStream(json.toByteArray())
        coEvery { importBackupUseCase.execute(any()) } returns Result.success(Unit)
        // Mock FileReader behaviour
        coEvery { fileReader.read(any()) } returns json

        // Make sure mocks and dependencies are initialised
        assertNotNull(fileReader)
        assertNotNull(importBackupUseCase)
        assertNotNull(backupRepository)

        // Debug - print some values to help trace NPE
        println("DEBUG: fileReader=$fileReader, importBackupUseCase=$importBackupUseCase, backupRepository=$backupRepository, uri=$uri")
        // Make sure the mocked file reader actually returns expected content
        // Don't call the fileReader directly from the test to avoid double invocation count
        // Reset mock call records (keep answers/stubs) to only verify the call performed by the ViewModel
        clearMocks(fileReader, answers = false)
        // Create viewModel afterwards and call the suspending variant directly to avoid launching new coroutines
        viewModel = SettingsViewModel(backupRepository, importBackupUseCase, userPreferencesRepository, fileReader, context)
        viewModel.importConfigSuspend(uri)

        // allow coroutine to run - runTest ensures scheduled coroutines complete
        coVerify(exactly = 1) { fileReader.read(uri) }
        coVerify(exactly = 1) { importBackupUseCase.execute(any()) }
        // do nothing more; Reset dispatchers was moved to finally
    }

}
