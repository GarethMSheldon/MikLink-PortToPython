/*
 * Purpose: Verify correct JSON parsing of DhcpClientStatus from RouterOS REST API.
 * Inputs: JSON payloads matching real RouterOS /ip/dhcp-client responses.
 * Outputs: Assertions on parsed DTO fields, especially 'primary-dns' mapping.
 * Notes: RouterOS returns 'primary-dns' (not 'dns'), this test ensures the mapping is correct.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DhcpClientStatusJsonTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, DhcpClientStatus::class.java)
    private val adapter = moshi.adapter<List<DhcpClientStatus>>(listType)

    @Test
    fun `parses primary-dns field correctly`() {
        // Real RouterOS response format: DNS is returned as "primary-dns"
        val json = """
            [{
                ".id": "*1",
                "disabled": "false",
                "status": "bound",
                "address": "192.168.1.100/24",
                "gateway": "192.168.1.1",
                "primary-dns": "8.8.8.8"
            }]
        """.trimIndent()

        val parsed = adapter.fromJson(json)

        assertEquals("8.8.8.8", parsed?.firstOrNull()?.dns)
    }

    @Test
    fun `parses all fields from dhcp-client response`() {
        val json = """
            [{
                ".id": "*2",
                "disabled": "true",
                "status": "searching",
                "address": "10.0.0.50/24",
                "gateway": "10.0.0.1",
                "primary-dns": "1.1.1.1"
            }]
        """.trimIndent()

        val parsed = adapter.fromJson(json)?.firstOrNull()

        assertEquals("*2", parsed?.id)
        assertEquals("true", parsed?.disabled)
        assertEquals("searching", parsed?.status)
        assertEquals("10.0.0.50/24", parsed?.address)
        assertEquals("10.0.0.1", parsed?.gateway)
        assertEquals("1.1.1.1", parsed?.dns)
    }

    @Test
    fun `handles missing primary-dns field`() {
        // Some DHCP responses may not include DNS (e.g., during 'searching' state)
        val json = """
            [{
                ".id": "*1",
                "status": "searching"
            }]
        """.trimIndent()

        val parsed = adapter.fromJson(json)?.firstOrNull()

        assertNull(parsed?.dns)
    }
}
