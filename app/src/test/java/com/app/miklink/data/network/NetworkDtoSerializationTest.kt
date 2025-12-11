package com.app.miklink.data.network

import com.app.miklink.data.network.dto.CableTestResult
import com.app.miklink.data.network.dto.NeighborDetail
import com.app.miklink.data.network.dto.PingResult
import com.app.miklink.data.network.NeighborDetailListAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import com.app.miklink.data.network.dto.RouteEntry
import com.app.miklink.data.network.dto.RouteAdd

class NetworkDtoSerializationTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(NeighborDetailListAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `PingResult serialize and deserialize`() {
        val proto = PingResult(
            avgRtt = "10ms",
            host = "8.8.8.8",
            maxRtt = "15ms",
            minRtt = "8ms",
            packetLoss = "0",
            received = "4",
            sent = "4",
            seq = "4",
            size = "64",
            time = "10ms",
            ttl = "64"
        )

        val adapter = moshi.adapter(PingResult::class.java)
        val json = adapter.toJson(proto)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(proto, parsed)
    }

    @Test
    fun `CableTestResult serialize and deserialize`() {
        val pairs = listOf(mapOf("pair" to "1-2", "status" to "open", "length" to "45m"))
        val proto = CableTestResult(cablePairs = pairs, status = "OK")

        val adapter = moshi.adapter(CableTestResult::class.java)
        val json = adapter.toJson(proto)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(proto.status, parsed?.status)
        assertEquals(proto.cablePairs?.size, parsed?.cablePairs?.size)
        assertEquals(proto.cablePairs?.first(), parsed?.cablePairs?.first())
    }

    @Test
    fun `NeighborDetail serialize and deserialize`() {
        val proto = NeighborDetail(
            identity = "Switch-1",
            interfaceName = "ether1",
            systemCaps = "bridge",
            discoveredBy = "lldp",
            vlanId = "10",
            voiceVlanId = null,
            poeClass = "class-3",
            systemDescription = "Test Switch",
            portId = "eth1"
        )

        val adapter = moshi.adapter(NeighborDetail::class.java)
        val json = adapter.toJson(proto)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(proto, parsed)
    }

    @Test
    fun `NeighborDetailListAdapter handles single object and arrays`() {
        val singleJson = "{\"identity\":\"sw\",\"interface-name\":\"eth1\"}"
        val listJson = "[{\"identity\":\"a\"},{\"identity\":\"b\"}]"

        val type = Types.newParameterizedType(List::class.java, NeighborDetail::class.java)
        val adapter = moshi.adapter<List<NeighborDetail>>(type)

        val fromSingle = adapter.fromJson(singleJson)
        val fromList = adapter.fromJson(listJson)

        assertNotNull(fromSingle)
        assertEquals(1, fromSingle?.size)
        assertEquals("sw", fromSingle?.first()?.identity)

        assertNotNull(fromList)
        assertEquals(2, fromList?.size)
        assertEquals("a", fromList?.get(0)?.identity)
    }

    @Test
    fun `RouteEntry serialize and deserialize includes comment`() {
        val proto = RouteEntry(id = "*1", dstAddress = "0.0.0.0/0", gateway = "192.168.1.254", comment = "MikLink_Auto")
        val adapter = moshi.adapter(RouteEntry::class.java)
        val json = adapter.toJson(proto)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(proto, parsed)
    }

    @Test
    fun `RouteAdd serialize and deserialize includes comment`() {
        val proto = RouteAdd(dstAddress = "0.0.0.0/0", gateway = "192.168.1.254", comment = "MikLink_Auto")
        val adapter = moshi.adapter(RouteAdd::class.java)
        val json = adapter.toJson(proto)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(proto, parsed)
    }
}
