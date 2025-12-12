package com.app.miklink.core.data.repository

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.Report
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.PingResult
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.utils.UiState
import kotlinx.coroutines.flow.Flow

/**
 * Bridge interface for the application repository. Implemented by legacy implementation
 * during migration. Define only signatures used by UI/ViewModels to keep DI stable.
 * 
 * @deprecated S5-S7: La maggior parte dei metodi sono stati migrati a repository dedicati.
 * Questa interfaccia resta principalmente per compatibilità durante la transizione.
 * 
 * Metodi migrati:
 * - Test methods → RunTestUseCase + Step implementations (S5)
 * - Network config → NetworkConfigRepository (S6)
 * - Probe status/monitoring → ProbeStatusRepository, ProbeConnectivityRepository (S7)
 */
interface AppRepository {
    /**
     * @deprecated S7: Usa ProbeConfigDao.getSingleProbe() direttamente o tramite ProbeRepository
     */
    @Deprecated("Use ProbeConfigDao.getSingleProbe() or ProbeRepository instead", ReplaceWith("probeConfigDao.getSingleProbe()"))
    val currentProbe: Flow<ProbeConfig?>

    /**
     * @deprecated S6: Usa NetworkConfigRepository.applyClientNetworkConfig()
     */
    @Deprecated("Use NetworkConfigRepository.applyClientNetworkConfig() instead", ReplaceWith("NetworkConfigRepository.applyClientNetworkConfig(probe, client, override)"))
    suspend fun applyClientNetworkConfig(probe: ProbeConfig, client: Client, override: Client? = null): UiState<NetworkConfigFeedback>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + CableTestStep
     */
    @Deprecated("Use RunTestUseCase + CableTestStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + LinkStatusStep
     */
    @Deprecated("Use RunTestUseCase + LinkStatusStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + NeighborDiscoveryStep
     */
    @Deprecated("Use RunTestUseCase + NeighborDiscoveryStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>>

    /**
     * @deprecated S6: Usa PingTargetResolver.resolve() invece
     */
    @Deprecated("Use PingTargetResolver.resolve() instead", ReplaceWith("PingTargetResolver.resolve(probe, client, profile, target)"))
    suspend fun resolveTargetIp(probe: ProbeConfig, target: String, interfaceName: String): String

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + PingStep
     */
    @Deprecated("Use RunTestUseCase + PingStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String, count: Int = 4): UiState<List<PingResult>>

    /**
     * @deprecated S5: Sostituito da RunTestUseCase + SpeedTestStep
     */
    @Deprecated("Use RunTestUseCase + SpeedTestStep instead", ReplaceWith("RunTestUseCase.execute(plan)"))
    suspend fun runSpeedTest(probe: ProbeConfig, client: Client): UiState<SpeedTestResult>

    /**
     * @deprecated S7: Usa ProbeStatusRepository.observeAllProbesWithStatus()
     */
    @Deprecated("Use ProbeStatusRepository.observeAllProbesWithStatus() instead", ReplaceWith("ProbeStatusRepository.observeAllProbesWithStatus()"))
    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>

    /**
     * @deprecated S7: Usa ProbeStatusRepository.observeProbeStatus(probe)
     */
    @Deprecated("Use ProbeStatusRepository.observeProbeStatus(probe) instead", ReplaceWith("ProbeStatusRepository.observeProbeStatus(probe)"))
    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>

    /**
     * @deprecated S7: Usa ProbeConnectivityRepository.checkProbeConnection(probe)
     */
    @Deprecated("Use ProbeConnectivityRepository.checkProbeConnection(probe) instead", ReplaceWith("ProbeConnectivityRepository.checkProbeConnection(probe)"))
    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult

}

data class ProbeStatusInfo(val probe: ProbeConfig, val isOnline: Boolean)

sealed class ProbeCheckResult {
    data class Success(val boardName: String, val interfaces: List<String>) : ProbeCheckResult()
    data class Error(val message: String) : ProbeCheckResult()
}

data class NetworkConfigFeedback(
    val mode: String,
    val interfaceName: String,
    val address: String?,
    val gateway: String?,
    val dns: String?,
    val message: String
)
