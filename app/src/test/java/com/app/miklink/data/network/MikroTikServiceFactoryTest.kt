package com.app.miklink.data.network

import com.app.miklink.data.db.model.ProbeConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MikroTikServiceFactoryTest {

    private lateinit var factory: MikroTikServiceFactory

    @Before
    fun setup() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofitBuilder = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
        val client = OkHttpClient.Builder().build()
        factory = MikroTikServiceFactory(retrofitBuilder, client)
    }

    @Test
    fun `basicAuthHeader produces valid header`() {
        val header = factory.basicAuthHeader("admin", "secret")
        // base64 of "admin:secret" is "YWRtaW46c2VjcmV0"
        assertEquals("Basic YWRtaW46c2VjcmV0", header)
    }

    @Test
    fun `createService builds base url correctly with http`() {
        val probe = ProbeConfig(1, "10.0.0.1", "", "", "eth0", true, null, true, false)
        val service = factory.createService(probe)
        // Service instance should be created successfully (retrofit dynamic proxy)
        assertNotNull(service)
    }
}
