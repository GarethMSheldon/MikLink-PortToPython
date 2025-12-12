package com.app.miklink.data.repositoryimpl.mikrotik

import com.app.miklink.core.data.local.room.v1.dao.ProbeConfigDao
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.dto.ProplistRequest
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.ProbeStatusInfo
import com.app.miklink.core.data.repository.probe.ProbeStatusRepository
import com.app.miklink.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione di ProbeStatusRepository.
 *
 * Usa MikroTikServiceProvider per costruire il service e chiama l'API MikroTik
 * per verificare lo stato online/offline delle sonde.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProbeStatusRepositoryImpl @Inject constructor(
    private val probeConfigDao: ProbeConfigDao,
    private val serviceProvider: MikroTikServiceProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ProbeStatusRepository {

    override fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> =
        userPreferencesRepository.probePollingInterval
            .flatMapLatest { interval ->
                flow {
                    while (true) {
                        val isOnline = try {
                            val api = serviceProvider.build(probe)
                            api.getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
                        } catch (_: HttpException) {
                            false
                        } catch (_: Exception) {
                            false
                        }
                        emit(isOnline)
                        delay(interval)
                    }
                }
            }

    override fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> =
        combine(probeConfigDao.getAllProbes(), userPreferencesRepository.probePollingInterval) { probes, interval ->
            Pair(probes, interval)
        }.flatMapLatest { (probes, interval) ->
            if (probes.isEmpty()) return@flatMapLatest flowOf(emptyList())
            tickerFlow(interval).map {
                withContext(Dispatchers.IO) {
                    probes.map { probe ->
                        val isOnline = try {
                            val api = serviceProvider.build(probe)
                            val result = api.getSystemResource(ProplistRequest(listOf("board-name")))
                            result.isNotEmpty()
                        } catch (e: Exception) {
                            android.util.Log.w("ProbeStatusRepository", "Sonda @ ${probe.ipAddress} offline: ${e.message}")
                            false
                        }
                        ProbeStatusInfo(probe, isOnline)
                    }
                }
            }
        }
}

// Simple ticker flow helper
private fun tickerFlow(periodMs: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMs)
    }
}

