package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemResourceJsonTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `parses board-name from json`() {
        val json = """[{"board-name":"hAP ax^2"}]"""
        val type = Types.newParameterizedType(List::class.java, SystemResource::class.java)
        val adapter = moshi.adapter<List<SystemResource>>(type)

        val parsed = adapter.fromJson(json)

        assertEquals("hAP ax^2", parsed?.firstOrNull()?.boardName)
    }
}
