package com.app.miklink.data.network

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.POST

// --- DTOs (Data Transfer Objects) ---

// Richieste Generiche
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)
// Correzione filtro: RouterOS REST usa '?interface' (senza punto)
data class InterfaceNameRequest(@Json(name = "?interface") val interfaceName: String)
// Numeri (id) per operazioni enable/disable/remove

data class NumbersRequest(@Json(name = "numbers") val numbers: String)

// Risorse Sonda
data class SystemResource(@Json(name = "board-name") val boardName: String)
data class EthernetInterface(val name: String)

// DHCP client info esteso

data class DhcpClientStatus(
    @Json(name = ".id") val id: String? = null,
    val disabled: String? = null,
    val status: String? = null, // bound, searching
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null
)

// IP address entries

data class IpAddressEntry(
    @Json(name = ".id") val id: String? = null,
    val address: String? = null,
    @Json(name = "interface") val iface: String? = null
)

data class IpAddressAdd(@Json(name = "address") val address: String, @Json(name = "interface") val `interface`: String)

// Routes

data class RouteEntry(
    @Json(name = ".id") val id: String? = null,
    @Json(name = "dst-address") val dstAddress: String? = null,
    val gateway: String? = null
)

data class RouteAdd(
    @Json(name = "dst-address") val dstAddress: String,
    val gateway: String
)

// DHCP client add

data class DhcpClientAdd(@Json(name = "interface") val `interface`: String, @Json(name = "use-peer-dns") val usePeerDns: String = "yes")

// Risultati Test

data class CableTestRequest(@Json(name = "numbers") val numbers: String)
data class CableTestResult(@Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>, val status: String)


data class MonitorRequest(@Json(name = "numbers") val numbers: String, @Json(name = "once") val once: Boolean = true)
data class MonitorResponse(val status: String, val rate: String?)


data class NeighborRequest(@Json(name = "?.query") val query: List<String>, @Json(name = ".proplist") val proplist: List<String>)
data class NeighborDetail(
    val identity: String?,
    @Json(name = "interface-name") val interfaceName: String?,
    @Json(name = "system-caps-enabled") val systemCaps: String?,
    @Json(name = "discovered-by") val discoveredBy: String?,
    @Json(name = "vlan-id") val vlanId: String? = null,
    @Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @Json(name = "poe-class") val poeClass: String? = null
)


data class PingRequest(val address: String, val count: String = "4")
data class PingResult(@Json(name = "avg-rtt") val avgRtt: String?)

// Traceroute

data class TracerouteRequest(
    val address: String,
    @Json(name = "max-hops") val maxHops: String = "30",
    val timeout: String = "3000ms"
)

data class TracerouteHop(
    val hop: String? = null,
    val host: String? = null,
    @Json(name = "avg-rtt") val avgRtt: String? = null
)

interface MikroTikApiService {

    @POST("/rest/system/resource/print")
    suspend fun getSystemResource(@Body request: ProplistRequest): List<SystemResource>

    @POST("/rest/interface/ethernet/print")
    suspend fun getEthernetInterfaces(@Body request: ProplistRequest): List<EthernetInterface>

    // DHCP client management
    @POST("/rest/ip/dhcp-client/print")
    suspend fun getDhcpClientStatus(@Body request: InterfaceNameRequest): List<DhcpClientStatus>

    @POST("/rest/ip/dhcp-client/add")
    suspend fun addDhcpClient(@Body request: DhcpClientAdd): Any

    @POST("/rest/ip/dhcp-client/enable")
    suspend fun enableDhcpClient(@Body request: NumbersRequest): Any

    @POST("/rest/ip/dhcp-client/disable")
    suspend fun disableDhcpClient(@Body request: NumbersRequest): Any

    // IP address management
    @POST("/rest/ip/address/print")
    suspend fun getIpAddresses(@Body request: ProplistRequest): List<IpAddressEntry>

    @POST("/rest/ip/address/add")
    suspend fun addIpAddress(@Body request: IpAddressAdd): Any

    @POST("/rest/ip/address/remove")
    suspend fun removeIpAddress(@Body request: NumbersRequest): Any

    // Routes management
    @POST("/rest/ip/route/print")
    suspend fun getRoutes(@Body request: ProplistRequest): List<RouteEntry>

    @POST("/rest/ip/route/add")
    suspend fun addRoute(@Body request: RouteAdd): Any

    @POST("/rest/ip/route/remove")
    suspend fun removeRoute(@Body request: NumbersRequest): Any

    // Tests
    @POST("/rest/interface/ethernet/cable-test")
    suspend fun runCableTest(@Body request: CableTestRequest): List<CableTestResult>

    @POST("/rest/interface/ethernet/monitor")
    suspend fun getLinkStatus(@Body request: MonitorRequest): List<MonitorResponse>

    @POST("/rest/ip/neighbor/print")
    suspend fun getIpNeighbors(@Body request: NeighborRequest): List<NeighborDetail>

    @POST("/rest/ping")
    suspend fun runPing(@Body request: PingRequest): List<PingResult>

    @POST("/rest/tool/traceroute")
    suspend fun runTraceroute(@Body request: TracerouteRequest): List<TracerouteHop>
}