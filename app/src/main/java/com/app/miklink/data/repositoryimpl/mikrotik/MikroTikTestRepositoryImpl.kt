/*
 * Purpose: MikroTik test repository implementation that binds Retrofit calls to the Wi-Fi network and returns domain data.
 * Inputs: Probe configuration plus per-call parameters (interface names, targets, credentials).
 * Outputs: Domain test models derived from MikroTik REST endpoints.
 * Notes: DTO usage stays internal; mapping is centralized in data/remote/mikrotik/mapper to keep ports clean.
 */
package com.app.miklink.data.repositoryimpl.mikrotik

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.data.remote.mikrotik.dto.CableTestRequest
import com.app.miklink.data.remote.mikrotik.dto.MonitorRequest
import com.app.miklink.data.remote.mikrotik.dto.PingRequest
import com.app.miklink.data.remote.mikrotik.dto.SpeedTestRequest
import com.app.miklink.data.remote.mikrotik.infra.MikroTikServiceFactory
import com.app.miklink.data.remote.mikrotik.mapper.toDomain
import com.app.miklink.data.remote.mikrotik.mapper.toLinkStatusData
import com.app.miklink.data.remote.mikrotik.mapper.toMeasurement
import com.app.miklink.data.remote.mikrotik.mapper.toSummary
import com.app.miklink.data.remote.mikrotik.service.MikroTikApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementazione di MikroTikTestRepository che usa MikroTikApiService.
 * Replica la logica di AppRepository_legacy per costruire il service con WiFi network binding.
 */
class MikroTikTestRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serviceFactory: MikroTikServiceFactory
) : MikroTikTestRepository {

    @Suppress("DEPRECATION")
    private fun findWifiNetwork(): Network? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
        val wifiNetwork = findWifiNetwork()
        return serviceFactory.createService(probe, wifiNetwork?.socketFactory)
    }

    override suspend fun monitorEthernet(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): LinkStatusData = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val results = api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = once))
        val latest = results.lastOrNull() ?: throw IllegalStateException("No link status returned")
        latest.toLinkStatusData()
    }

    override suspend fun cableTest(
        probe: ProbeConfig,
        interfaceName: String,
        once: Boolean
    ): CableTestSummary = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val results = api.runCableTest(CableTestRequest(numbers = interfaceName, duration = "5s"))
        val validResult = results.lastOrNull {
            it.cablePairs != null || it.status.lowercase() in listOf("ok", "open", "link-ok", "running")
        } ?: throw IllegalStateException("No valid cable test results found")
        validResult.toSummary()
    }

    override suspend fun ping(
        probe: ProbeConfig,
        target: String,
        interfaceName: String?,
        count: Int
    ): List<PingMeasurement> = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = target, `interface` = interfaceName, count = count.toString()))
            .map { it.toMeasurement() }
    }

    override suspend fun neighbors(
        probe: ProbeConfig,
        interfaceName: String
    ): List<NeighborData> = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        api.getIpNeighbors(interfaceName).map { it.toDomain() }
    }

    override suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String?,
        password: String?,
        duration: String
    ): SpeedTestData = withContext(Dispatchers.IO) {
        val api = buildServiceFor(probe)
        val requestBody = SpeedTestRequest(
            address = serverAddress,
            user = username ?: "admin",
            password = password ?: "",
            testDuration = duration
        )
        val response = api.runSpeedTest(requestBody)
        if (response.isSuccessful) {
            val body = response.body()
            val result = body?.lastOrNull { it.status == "done" } ?: body?.lastOrNull()
            result?.toDomain(serverAddress) ?: throw IllegalStateException("Empty speed test response")
        } else {
            when (response.code()) {
                400 -> throw IllegalArgumentException("Bad request: ${response.message()}")
                401, 403 -> throw SecurityException("Authentication failed")
                else -> throw HttpException(response)
            }
        }
    }
}
