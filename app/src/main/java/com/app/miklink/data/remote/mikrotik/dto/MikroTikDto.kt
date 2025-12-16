/*
 * Purpose: Moshi DTOs for MikroTik RouterOS requests and responses used by the data layer.
 * Inputs: Raw JSON payloads from MikroTik HTTP APIs mapped with explicit field targets.
 * Outputs: Typed request/response models consumed by MikroTik repositories and services.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

// General request wrappers
data class ProplistRequest(@field:Json(name = ".proplist") val proplist: List<String>)
data class InterfaceNameRequest(@field:Json(name = "?interface") val interfaceName: String)
data class NumbersRequest(@field:Json(name = "numbers") val numbers: String)

// System / Interface responses
data class SystemResource(@field:Json(name = "board-name") val boardName: String)
data class EthernetInterface(@field:Json(name = "name") val name: String)

// DHCP client
data class DhcpClientStatus(
    @field:Json(name = ".id") val id: String? = null,
    val disabled: String? = null,
    val status: String? = null,
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null
)

// IP address management
data class IpAddressEntry(
    @field:Json(name = ".id") val id: String? = null,
    val address: String? = null,
    @field:Json(name = "interface") val iface: String? = null
)

data class IpAddressAdd(@field:Json(name = "address") val address: String, @field:Json(name = "interface") val `interface`: String)

// Routes
data class RouteEntry(
    @field:Json(name = ".id") val id: String? = null,
    @field:Json(name = "dst-address") val dstAddress: String? = null,
    val gateway: String? = null,
    val comment: String? = null
)

data class RouteAdd(
    @field:Json(name = "dst-address") val dstAddress: String,
    val gateway: String,
    val comment: String? = null
)

// DHCP client add
data class DhcpClientAdd(@field:Json(name = "interface") val `interface`: String, @field:Json(name = "use-peer-dns") val usePeerDns: String = "yes")

// Cable test
data class CableTestRequest(
    @field:Json(name = "numbers") val numbers: String,
    val duration: String = "5s"
)

data class CableTestResult(
    @field:Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>?,
    val status: String
)

// Link monitor
data class MonitorRequest(@field:Json(name = "numbers") val numbers: String, @field:Json(name = "once") val once: Boolean = true)
data class MonitorResponse(val status: String, val rate: String?)

// Neighbor / LLDP
data class NeighborRequest(@field:Json(name = "?.query") val query: List<String>, @field:Json(name = ".proplist") val proplist: List<String>)

data class NeighborDetail(
    val identity: String?,
    @field:Json(name = "interface-name") val interfaceName: String?,
    @field:Json(name = "system-caps-enabled") val systemCaps: String?,
    @field:Json(name = "discovered-by") val discoveredBy: String?,
    @field:Json(name = "vlan-id") val vlanId: String? = null,
    @field:Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @field:Json(name = "poe-class") val poeClass: String? = null,
    @field:Json(name = "system-description") val systemDescription: String? = null,
    @field:Json(name = "port-id") val portId: String? = null
)

// Ping
data class PingRequest(
    val address: String,
    val `interface`: String? = null,
    val count: String = "4"
)

data class PingResult(
    @field:Json(name = "avg-rtt") val avgRtt: String?,
    val host: String?,
    @field:Json(name = "max-rtt") val maxRtt: String?,
    @field:Json(name = "min-rtt") val minRtt: String?,
    @field:Json(name = "packet-loss") val packetLoss: String?,
    val received: String?,
    val sent: String?,
    val seq: String?,
    val size: String?,
    val time: String?,
    val ttl: String?
)

// Note: SpeedTestRequest/Result live in their own files under this package and should not be duplicated.
