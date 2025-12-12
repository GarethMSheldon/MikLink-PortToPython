package com.app.miklink.core.data.repository.probe

import com.app.miklink.core.data.local.room.v1.dao.ProbeConfigDao
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.core.data.remote.mikrotik.dto.SystemResource
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.data.repository.UserPreferencesRepository
import com.app.miklink.data.repositoryimpl.mikrotik.ProbeStatusRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

/**
 * Contract test per ProbeStatusRepository.
 * Verifica il comportamento di monitoraggio stato online/offline delle sonde.
 */
class ProbeStatusRepositoryContractTest {

    private val mockProbeConfigDao = mockk<ProbeConfigDao>()
    private val mockServiceProvider = mockk<MikroTikServiceProvider>()
    private val mockApiService = mockk<MikroTikApiService>()
    private val mockUserPreferencesRepository = mockk<UserPreferencesRepository>()
    private val repository: ProbeStatusRepository = ProbeStatusRepositoryImpl(
        mockProbeConfigDao,
        mockServiceProvider,
        mockUserPreferencesRepository
    )

    private val testProbe = ProbeConfig(
        probeId = 1,
        ipAddress = "192.168.1.1",
        username = "admin",
        password = "password",
        testInterface = "ether1",
        isOnline = false,
        modelName = null,
        tdrSupported = false,
        isHttps = false
    )

    @Test
    fun `observeProbeStatus returns true when probe is online`() = runTest {
        // Given: Probe online (API risponde con successo)
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = "Test Board")
        )

        // When
        val result = repository.observeProbeStatus(testProbe).first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `observeProbeStatus returns false when probe is offline`() = runTest {
        // Given: Probe offline (API lancia eccezione)
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)
        every { mockServiceProvider.build(testProbe) } returns mockApiService
        coEvery { mockApiService.getSystemResource(any<ProplistRequest>()) } throws HttpException(
            mockk(relaxed = true)
        )

        // When
        val result = repository.observeProbeStatus(testProbe).first()

        // Then
        assertFalse(result)
    }

    @Test
    fun `observeAllProbesWithStatus returns empty list when no probes exist`() = runTest {
        // Given: Nessuna sonda configurata
        every { mockProbeConfigDao.getAllProbes() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)

        // When
        val result = repository.observeAllProbesWithStatus().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeAllProbesWithStatus returns list with online status`() = runTest {
        // Given: Due sonde, una online e una offline
        val probe1 = testProbe.copy(probeId = 1, ipAddress = "192.168.1.1")
        val probe2 = testProbe.copy(probeId = 2, ipAddress = "192.168.1.2")
        val mockApiService1 = mockk<MikroTikApiService>()
        val mockApiService2 = mockk<MikroTikApiService>()
        
        every { mockProbeConfigDao.getAllProbes() } returns flowOf(listOf(probe1, probe2))
        every { mockUserPreferencesRepository.probePollingInterval } returns flowOf(100L)
        every { mockServiceProvider.build(probe1) } returns mockApiService1
        every { mockServiceProvider.build(probe2) } returns mockApiService2
        coEvery { mockApiService1.getSystemResource(any<ProplistRequest>()) } returns listOf(
            SystemResource(boardName = "Board1")
        )
        coEvery { mockApiService2.getSystemResource(any<ProplistRequest>()) } throws HttpException(
            mockk(relaxed = true)
        )

        // When
        val result = repository.observeAllProbesWithStatus().first()

        // Then
        assertEquals(2, result.size)
        assertEquals(probe1, result[0].probe)
        assertTrue(result[0].isOnline)
        assertEquals(probe2, result[1].probe)
        assertFalse(result[1].isOnline)
    }
}

