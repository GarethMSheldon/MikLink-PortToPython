package com.app.miklink.data.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.network.*
import com.app.miklink.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

// Result wrapper for the probe check
sealed class ProbeCheckResult {
    data class Success(val boardName: String, val interfaces: List<String>) : ProbeCheckResult()
    data class Error(val message: String) : ProbeCheckResult()
}

data class ProbeStatusInfo(val probe: ProbeConfig, val isOnline: Boolean)

@Singleton
class AppRepository @Inject constructor(
    private val application: Application,
    private val clientDao: ClientDao,
    val probeConfigDao: ProbeConfigDao,
    private val testProfileDao: TestProfileDao,
    private val reportDao: ReportDao,
    private val retrofitBuilder: Retrofit.Builder,
    private val baseOkHttpClient: OkHttpClient,
    private val authInterceptor: AuthInterceptor
) {

    private suspend fun findWifiNetwork(): Network? = withContext(Dispatchers.IO) {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { capabilities ->
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return@withContext network
                }
            }
        }
        return@withContext null
    }

    private suspend fun getDynamicApiService(probe: ProbeConfig): MikroTikApiService {
        authInterceptor.setCredentials(probe.username, probe.password)

        val wifiNetwork = findWifiNetwork()
            ?: throw IllegalStateException("WiFi is not connected. Cannot create a dynamic API service.")

        val dynamicClient = baseOkHttpClient.newBuilder()
            .socketFactory(wifiNetwork.socketFactory)
            .build()

        val protocol = if (probe.isHttps) "https://" else "http://"
        val baseUrl = "$protocol${probe.ipAddress}/"

        return retrofitBuilder
            .baseUrl(baseUrl)
            .client(dynamicClient)
            .build()
            .create(MikroTikApiService::class.java)
    }

    private suspend fun <T> safeApiCall(probe: ProbeConfig, apiCall: suspend (MikroTikApiService) -> T): UiState<T> {
        return withContext(Dispatchers.IO) {
            try {
                val service = getDynamicApiService(probe)
                UiState.Success(apiCall.invoke(service))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    suspend fun getDhcpGateway(probe: ProbeConfig, interfaceName: String): String? {
        return try {
            getDynamicApiService(probe).getDhcpClientStatus(InterfaceNameRequest(interfaceName))?.firstOrNull()?.gateway
        } catch (e: Exception) {
            null
        }
    }

    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flow {
        while (true) {
            val isOnline = try {
                getDynamicApiService(probe)
                    .getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
            } catch (e: Exception) {
                false // Catches HttpException, IllegalStateException, etc.
            }
            emit(isOnline)
            delay(15000) // Check every 15 seconds
        }
    }

    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult = withContext(Dispatchers.IO) {
        try {
            val service = getDynamicApiService(probe)
            val resourceRequest = ProplistRequest(listOf("board-name"))
            val interfaceRequest = ProplistRequest(listOf("name"))
            val boardName = service.getSystemResource(resourceRequest).firstOrNull()?.boardName ?: "Unknown Board"
            val interfaces = service.getEthernetInterfaces(interfaceRequest).map { it.name }
            ProbeCheckResult.Success(boardName, interfaces)
        } catch (e: Exception) {
            ProbeCheckResult.Error(e.message ?: "An unknown error occurred while connecting to the probe.")
        }
    }

    private suspend fun isProbeOnlineOnce(probe: ProbeConfig): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                withTimeout(5000L) {
                    getDynamicApiService(probe)
                        .getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> {
        return probeConfigDao.getAllProbes().flatMapLatest { probes ->
            if (probes.isEmpty()) {
                flowOf(emptyList())
            } else {
                flow {
                    while (true) {
                        val statuses = withContext(Dispatchers.IO) {
                            probes.map { probe ->
                                ProbeStatusInfo(probe, isProbeOnlineOnce(probe))
                            }
                        }
                        emit(statuses)
                        delay(10000) // Poll every 10 seconds
                    }
                }
            }
        }
    }

    // --- TEST FUNCTIONS ---

    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult> = safeApiCall(probe) {
        it.runCableTest(CableTestRequest(interfaceName)).first()
    }

    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse> = safeApiCall(probe) {
        it.getLinkStatus(MonitorRequest(numbers = interfaceName, once = true)).first()
    }

    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>> = safeApiCall(probe) {
        val request = NeighborRequest(
            query = listOf("interface=$interfaceName"),
            proplist = listOf("identity", "interface-name", "system-caps-enabled", "discovered-by", "vlan-id", "voice-vlan-id", "poe-class")
        )
        it.getIpNeighbors(request)
    }

    suspend fun runPing(probe: ProbeConfig, target: String): UiState<PingResult> = safeApiCall(probe) {
        it.runPing(PingRequest(address = target)).first()
    }

    suspend fun runTraceroute(probe: ProbeConfig, target: String): UiState<TracerouteResult> = safeApiCall(probe) {
        it.runTraceroute(TracerouteRequest(address = target)).first()
    }

    // --- NETWORK CONFIGURATION ---

    suspend fun addVlan(probe: ProbeConfig, name: String, vlanId: String, interfaceName: String): UiState<String> = safeApiCall(probe) {
        val response = it.addVlan(VlanRequest(name, vlanId, interfaceName))
        response.first()["ret"] ?: throw IllegalStateException("Could not get VLAN ID from response")
    }

    suspend fun removeVlan(probe: ProbeConfig, vlanId: String): UiState<Unit> = safeApiCall(probe) {
        it.removeVlan(RemoveRequest(vlanId))
    }

    suspend fun addIpAddress(probe: ProbeConfig, address: String, interfaceName: String): UiState<String> = safeApiCall(probe) {
        val response = it.addIpAddress(IpAddressRequest(address, interfaceName))
        response.first()["ret"] ?: throw IllegalStateException("Could not get IP Address ID from response")
    }

    suspend fun removeIpAddress(probe: ProbeConfig, ipId: String): UiState<Unit> = safeApiCall(probe) {
        it.removeIpAddress(RemoveRequest(ipId))
    }
}