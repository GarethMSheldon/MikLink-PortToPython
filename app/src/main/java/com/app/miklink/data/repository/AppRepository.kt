package com.app.miklink.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.network.*
import com.app.miklink.utils.UiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.Credentials
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

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val clientDao: ClientDao,
    val probeConfigDao: ProbeConfigDao,
    val testProfileDao: TestProfileDao,
    val reportDao: ReportDao,
    private val retrofitBuilder: Retrofit.Builder,
    private val baseOkHttpClient: OkHttpClient
) {

    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private suspend fun findWifiNetwork(): Network? = withContext(Dispatchers.IO) {
        // Prefer active WIFI network; fall back scanning all networks
        val active = connectivityManager.activeNetwork
        val activeCaps = active?.let { connectivityManager.getNetworkCapabilities(it) }
        if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return@withContext active
        connectivityManager.allNetworks.firstOrNull { net ->
            connectivityManager.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private suspend fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
        val protocol = if (probe.isHttps) "https://" else "http://"
        val baseUrl = "$protocol${probe.ipAddress}/"

        val wifiNetwork = findWifiNetwork()

        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val req = original.newBuilder()
                .header("Authorization", Credentials.basic(probe.username, probe.password))
                .build()
            chain.proceed(req)
        }

        val clientBuilder = baseOkHttpClient.newBuilder()
            .addInterceptor(authInterceptor)

        // Note: loggingInterceptor già presente in baseOkHttpClient, non duplicare

        if (wifiNetwork != null) {
            clientBuilder.socketFactory(wifiNetwork.socketFactory)
        }

        val client = clientBuilder.build()

        return retrofitBuilder
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(MikroTikApiService::class.java)
    }

    suspend fun resolveTargetIp(probe: ProbeConfig, target: String, interfaceName: String): String {
        if (target.equals("DHCP_GATEWAY", ignoreCase = true)) {
            return getDhcpGateway(probe, interfaceName) ?: target
        }
        return target
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): UiState<T> {
        return withContext(Dispatchers.IO) {
            try {
                UiState.Success(apiCall.invoke())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    internal suspend fun getDhcpGateway(probe: ProbeConfig, interfaceName: String): String? {
        return try {
            val api = buildServiceFor(probe)
            api.getDhcpClientStatus(InterfaceNameRequest(interfaceName)).firstOrNull()?.gateway
        } catch (e: Exception) {
            null
        }
    }

    fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flow {
        while (true) {
            val isOnline = try {
                val api = buildServiceFor(probe)
                api.getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
            } catch (e: HttpException) {
                false
            } catch (e: Exception) {
                false
            }
            emit(isOnline)
            delay(15000)
        }
    }

    suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult = withContext(Dispatchers.IO) {
        try {
            val api = buildServiceFor(probe)
            val resourceRequest = ProplistRequest(listOf("board-name"))
            val interfaceRequest = ProplistRequest(listOf("name"))
            val boardName = api.getSystemResource(resourceRequest).firstOrNull()?.boardName ?: "Unknown Board"
            val interfaces = api.getEthernetInterfaces(interfaceRequest).map { it.name }
            ProbeCheckResult.Success(boardName, interfaces)
        } catch (e: Exception) {
            ProbeCheckResult.Error(e.message ?: "An unknown error occurred while connecting to the probe.")
        }
    }

    // --- CONFIG RETE PER CLIENTE ---

    data class NetworkConfigFeedback(
        val mode: String,
        val interfaceName: String,
        val address: String?,
        val gateway: String?,
        val dns: String?,
        val message: String
    )

    suspend fun applyClientNetworkConfig(
        probe: ProbeConfig,
        client: Client,
        override: Client? = null // usa un Client temporaneo per override per-singolo-test
    ): UiState<NetworkConfigFeedback> = safeApiCall {
        val effective = override ?: client
        val api = buildServiceFor(probe)
        val iface = probe.testInterface

        // Helper: rimuovi IP statici su interfaccia
        suspend fun removeStaticAddressesOnInterface() {
            val addresses = api.getIpAddresses(ProplistRequest(listOf(".id", "address", "interface")))
            addresses.filter { it.iface == iface }.forEach { entry ->
                entry.id?.let { api.removeIpAddress(NumbersRequest(it)) }
            }
        }
        // Helper: rimuovi default route duplicate
        suspend fun removeDefaultRoutes() {
            val routes = api.getRoutes(ProplistRequest(listOf(".id", "dst-address", "gateway")))
            routes.filter { it.dstAddress == "0.0.0.0/0" }.forEach { r -> r.id?.let { api.removeRoute(NumbersRequest(it)) } }
        }

        // Gestione DHCP/STATIC
        val dhcps = api.getDhcpClientStatus(InterfaceNameRequest(iface))
        val dhcpId = dhcps.firstOrNull()?.id

        if (effective.networkMode.equals("DHCP", true)) {
            // DHCP: disable → enable → attendi bound
            if (dhcpId != null) {
                // Disable existing DHCP client first
                api.disableDhcpClient(NumbersRequest(dhcpId))
                delay(500) // Small delay
                // Enable it again
                api.enableDhcpClient(NumbersRequest(dhcpId))
            } else {
                // Create new DHCP client
                api.addDhcpClient(DhcpClientAdd(`interface` = iface))
            }

            // Attendi lease (max 6 secondi)
            var lease: DhcpClientStatus? = null
            repeat(6) {
                val cur = api.getDhcpClientStatus(InterfaceNameRequest(iface)).firstOrNull()
                if (cur?.status.equals("bound", true)) { lease = cur; return@repeat }
                delay(1000)
            }
            val bound = lease ?: api.getDhcpClientStatus(InterfaceNameRequest(iface)).firstOrNull()
            NetworkConfigFeedback(
                mode = "DHCP",
                interfaceName = iface,
                address = bound?.address,
                gateway = bound?.gateway,
                dns = bound?.dns,
                message = if (bound?.status == "bound") "DHCP lease acquisita" else "DHCP non bound (verificare server DHCP)"
            )
        } else {
            // STATIC
            // Disabilita DHCP se presente
            if (dhcpId != null) api.disableDhcpClient(NumbersRequest(dhcpId))
            removeStaticAddressesOnInterface()
            removeDefaultRoutes()

            val cidr = effective.staticCidr ?: buildString {
                val ip = effective.staticIp ?: ""
                val mask = effective.staticSubnet ?: ""
                if (ip.isNotBlank() && mask.isNotBlank()) append("$ip/$mask")
            }
            require(!cidr.isNullOrBlank()) { "Static CIDR non configurato" }

            api.addIpAddress(IpAddressAdd(address = cidr, `interface` = iface))
            val gw = effective.staticGateway ?: error("Gateway statico mancante")
            api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw))

            NetworkConfigFeedback(
                mode = "STATIC",
                interfaceName = iface,
                address = cidr,
                gateway = gw,
                dns = null,
                message = "Indirizzo statico configurato"
            )
        }
    }

    suspend fun getCurrentInterfaceIpConfig(probe: ProbeConfig): UiState<NetworkConfigFeedback> = safeApiCall {
        val api = buildServiceFor(probe)
        val iface = probe.testInterface
        val dhcp = api.getDhcpClientStatus(InterfaceNameRequest(iface)).firstOrNull()
        if (dhcp != null && dhcp.status?.equals("bound", true) == true) {
            NetworkConfigFeedback("DHCP", iface, dhcp.address, dhcp.gateway, dhcp.dns, "Lease attiva")
        } else {
            val addr = api.getIpAddresses(ProplistRequest(listOf("address", "interface"))).firstOrNull { it.iface == iface }
            val route = api.getRoutes(ProplistRequest(listOf("dst-address", "gateway"))).firstOrNull { it.dstAddress == "0.0.0.0/0" }
            NetworkConfigFeedback("STATIC", iface, addr?.address, route?.gateway, null, "Statico rilevato")
        }
    }

    // --- TEST FUNCTIONS ---

    suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult> = safeApiCall {
        val api = buildServiceFor(probe)
        api.runCableTest(CableTestRequest(interfaceName)).first()
    }

    suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse> = safeApiCall {
        val api = buildServiceFor(probe)
        api.getLinkStatus(MonitorRequest(numbers = interfaceName, once = true)).first()
    }

    suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>> = safeApiCall {
        val api = buildServiceFor(probe)
        val request = NeighborRequest(
            query = listOf("interface=$interfaceName"),
            proplist = listOf("identity", "interface-name", "system-caps-enabled", "discovered-by", "vlan-id", "voice-vlan-id", "poe-class")
        )
        api.getIpNeighbors(request)
    }

    suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String): UiState<PingResult> = safeApiCall {
        val resolvedTarget = resolveTargetIp(probe, target, interfaceName)
        if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
            throw IllegalStateException("DHCP gateway not resolved for interface $interfaceName")
        }
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = resolvedTarget)).first()
    }

    suspend fun runTraceroute(
        probe: ProbeConfig,
        target: String,
        interfaceName: String,
        maxHops: Int = 30,
        timeoutMs: Int = 3000
    ): UiState<List<TracerouteHop>> = safeApiCall {
        val resolvedTarget = resolveTargetIp(probe, target, interfaceName)
        if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
            throw IllegalStateException("DHCP gateway not resolved for interface $interfaceName")
        }
        val api = buildServiceFor(probe)
        api.runTraceroute(TracerouteRequest(address = resolvedTarget, maxHops = maxHops.toString(), timeout = "${timeoutMs}ms"))
    }

    // --- PROBE MONITORING ---

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> =
        probeConfigDao.getAllProbes().flatMapLatest { probes ->
            if (probes.isEmpty()) return@flatMapLatest flowOf(emptyList())
            tickerFlow(10_000L).map {
                withContext(Dispatchers.IO) {
                    probes.map { probe ->
                        val isOnline = try {
                            val api = buildServiceFor(probe)
                            val result = api.getSystemResource(ProplistRequest(listOf("board-name")))
                            result.isNotEmpty()
                        } catch (e: Exception) {
                            android.util.Log.w("AppRepository", "Probe '${probe.name}' offline: ${e.message}")
                            false
                        }
                        ProbeStatusInfo(probe, isOnline)
                    }
                }
            }
        }
}

// Simple ticker flow
private fun tickerFlow(periodMs: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMs)
    }
}

// Probe status monitoring
data class ProbeStatusInfo(val probe: com.app.miklink.data.db.model.ProbeConfig, val isOnline: Boolean)
