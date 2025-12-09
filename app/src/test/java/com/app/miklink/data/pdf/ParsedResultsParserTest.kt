package com.app.miklink.data.pdf

import com.app.miklink.data.network.NeighborDetailListAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ParsedResultsParserTest {

    private lateinit var moshi: Moshi
    private lateinit var parser: ParsedResultsParser

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .add(NeighborDetailListAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        parser = ParsedResultsParser(moshi)
    }

    @Test
    fun `GIVEN complete JSON WHEN parse called THEN returns parsed results`() {
        val completeJson = """
            {
                "link": { "status": "up", "speed": "1Gbps" },
                "lldp": [ { "identity": "Switch-1", "interface-name": "ether1" } ],
                "ping": [ { "avg-rtt": "10ms", "host": "1.2.3.4" } ],
                "tdr": [ { "status": "ok", "cable-pairs": [ { "pair":"1-2", "length":"50m", "status":"ok" } ] } ]
            }
        """.trimIndent()

        val result = parser.parse(completeJson)

        assertNotNull(result)
        assertEquals("up", result?.link?.get("status"))
        assertEquals("1Gbps", result?.link?.get("speed"))
        assertNotNull(result?.lldp)
        assertNotNull(result?.ping)
        assertNotNull(result?.tdr)
    }

    @Test
    fun `GIVEN legacy json WHEN parse called THEN normalizes ping and tdr lists`() {
        val legacyJson = """
            {
                "ping_target1": [ { "avg-rtt": "10ms", "packet-loss": "0" } ],
                "ping_8_8_8_8": [ { "avg-rtt": "20ms", "packet-loss": "0" } ],
                "tdr": { "status": "ok", "cable-pairs": [ { "pair":"1-2","length":"10m","status":"ok" } ] }
            }
        """.trimIndent()

        val parsed = parser.parse(legacyJson)
        assertNotNull(parsed)
        parsed!!.ping?.let { assertTrue(it.isNotEmpty()) }
        assertNotNull(parsed.tdr)
        assertEquals(1, parsed.tdr!!.size)
        assertEquals("ok", parsed.tdr!![0].status)
    }

    @Test
    fun `GIVEN empty JSON string WHEN parse called THEN returns null`() {
        val res = parser.parse("")
        assertNull(res)
    }

    @Test
    fun `GIVEN malformed JSON WHEN parse called THEN returns null`() {
        val res = parser.parse("{ invalid json")
        assertNull(res)
    }
}
