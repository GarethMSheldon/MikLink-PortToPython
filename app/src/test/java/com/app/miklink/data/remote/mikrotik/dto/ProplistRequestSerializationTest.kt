/*
 * Purpose: Ensure ProplistRequest JSON uses the expected ".proplist" key to match RouterOS conventions and docs.
 * Inputs: ProplistRequest serialized with Moshi.
 * Outputs: Assertion that the serialized JSON contains the ".proplist" property name with the provided values.
 * Notes: Prevents regressions where the dot prefix is dropped, which could change server-side filtering.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.app.miklink.testsupport.TestMoshiProvider
import org.junit.Assert.assertTrue
import org.junit.Test

class ProplistRequestSerializationTest {
    @Test
    fun `proplist is serialized with dot prefix`() {
        val moshi = TestMoshiProvider.provideMoshi()
        val adapter = moshi.adapter(ProplistRequest::class.java)

        val json = adapter.toJson(ProplistRequest(listOf("board-name")))

        assertTrue(json.contains("\".proplist\""))
        assertTrue(json.contains("board-name"))
    }
}
