package com.app.miklink.core.data.remote.mikrotik.golden

import com.app.miklink.testsupport.FixtureLoader
import com.app.miklink.testsupport.TestMoshiProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test

class LogGoldenParsingTest {
    data class GoldenLogEntry(
        @Json(name = ".id") val id: String?,
        val time: String?,
        val topics: String?,
        val message: String?
    )

    @Test
    fun `parse log proplist and assert topics present`() {
        val json = FixtureLoader.load("fixtures/routeros/7.20.5/log_get_proplist.json")
        val moshi: Moshi = TestMoshiProvider.provideMoshi()
        val type = Types.newParameterizedType(List::class.java, GoldenLogEntry::class.java)
        val adapter = moshi.adapter<List<GoldenLogEntry>>(type)

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        // at least one entry should contain interface,info
        val interfaceEntries = parsed!!.filter { it.topics?.contains("interface,info") == true }
        assertTrue(interfaceEntries.isNotEmpty())
        // at least one entry should contain dhcp,info
        val dhcpEntries = parsed.filter { it.topics?.contains("dhcp,info") == true }
        assertTrue(dhcpEntries.isNotEmpty())
        // sample field presence
        assertNotNull(parsed.first().id)
        assertNotNull(parsed.first().time)
        assertNotNull(parsed.first().message)
    }
}
