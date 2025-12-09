package com.app.miklink.data.network

import com.app.miklink.data.db.model.ProbeConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MikroTikServiceFactoryMockWebServerTest {

    private lateinit var factory: MikroTikServiceFactory
    private lateinit var mockServer: MockWebServer

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofitBuilder = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
        val client = OkHttpClient.Builder().build()
        factory = MikroTikServiceFactory(retrofitBuilder, client)
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `service uses http scheme and sends none-auth header when credentials blank`() = runBlocking {
        val probe = ProbeConfig(1, "${mockServer.hostName}:${mockServer.port}", "", "", "eth0", true, null, true, false)

        // enqueue a valid JSON array response for GET /rest/interface/ethernet
        mockServer.enqueue(MockResponse().setBody("[{\"name\":\"eth0\"}]").setHeader("Content-Type", "application/json"))

        val service = factory.createService(probe)
        val result = service.getEthernetInterfaces()

        // server should have received the request
        val recorded = mockServer.takeRequest()
        assertEquals("http", recorded.requestUrl?.scheme)

        // Authorization header not present when username/password blank
        assertNull(recorded.getHeader("Authorization"))

        // result should parse correctly
        assertEquals(1, result.size)
        assertEquals("eth0", result[0].name)
    }

    @Test
    fun `service adds Authorization header when credentials supplied`() = runBlocking {
        val probe = ProbeConfig(1, "${mockServer.hostName}:${mockServer.port}", "admin", "secret", "eth0", true, null, true, false)

        mockServer.enqueue(MockResponse().setBody("[{\"name\":\"eth0\"}]").setHeader("Content-Type", "application/json"))

        val service = factory.createService(probe)
        val result = service.getEthernetInterfaces()

        val recorded = mockServer.takeRequest()
        assertEquals("http", recorded.requestUrl?.scheme)

        // check Authorization header value
        val expected = factory.basicAuthHeader("admin", "secret")
        assertEquals(expected, recorded.getHeader("Authorization"))

        // sanity check
        assertEquals(1, result.size)
    }

    @Test
    fun `service connects over https when isHttps true`() = runBlocking {
        // configure MockWebServer to use HTTPS with self-signed cert
        // generate an ephemeral server certificate & configure MockWebServer to use TLS
        val serverHeld = HeldCertificate.Builder()
            .addSubjectAlternativeName(mockServer.hostName)
            .build()

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(serverHeld)
            .build()

        mockServer.useHttps(serverCertificates.sslSocketFactory(), false)

        // client trusts that held certificate
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(serverHeld.certificate)
            .build()

        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            // MockWebServer uses a hostname that won't match a real certificate so allow it
            .hostnameVerifier { _, _ -> true }
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofitBuilder = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
        val httpsFactory = MikroTikServiceFactory(retrofitBuilder, client)

        val probe = ProbeConfig(1, "${mockServer.hostName}:${mockServer.port}", "admin", "secret", "eth0", true, null, true, true)

        mockServer.enqueue(MockResponse().setBody("[{\"name\":\"eth0\"}]").setHeader("Content-Type", "application/json"))

        val service = httpsFactory.createService(probe)
        val result = service.getEthernetInterfaces()

        val recorded = mockServer.takeRequest()
        assertEquals("https", recorded.requestUrl?.scheme)

        val expected = httpsFactory.basicAuthHeader("admin", "secret")
        assertEquals(expected, recorded.getHeader("Authorization"))

        assertEquals(1, result.size)
    }

    @Test
    fun `server error responses cause exception`() = runBlocking {
        val probe = ProbeConfig(1, "${mockServer.hostName}:${mockServer.port}", "", "", "eth0", true, null, true, false)

        // server returns 500
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal error"))

        val service = factory.createService(probe)

        try {
            service.getEthernetInterfaces()
            org.junit.Assert.fail("Expected HttpException due to 500 response")
        } catch (e: Exception) {
            // Retrofit suspend functions throw HttpException on non-2xx
            org.junit.Assert.assertTrue(e is HttpException || e is IOException)
        }
    }

    @Test
    fun `client read timeout leads to exception`() = runBlocking {
        // short client timeout
        val timedClient = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofitBuilder = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
        val timeoutFactory = MikroTikServiceFactory(retrofitBuilder, timedClient)

        val probe = ProbeConfig(1, "${mockServer.hostName}:${mockServer.port}", "", "", "eth0", true, null, true, false)

        // add a response that delays body delivery to trigger a timeout
        mockServer.enqueue(MockResponse().setBody("[{\"name\":\"eth0\"}]").setBodyDelay(2, TimeUnit.SECONDS))

        val service = timeoutFactory.createService(probe)
        try {
            service.getEthernetInterfaces()
            org.junit.Assert.fail("Expected timeout exception")
        } catch (e: Exception) {
            // OkHttp will usually throw an IOException on timeout
            org.junit.Assert.assertTrue(e is IOException)
        }
    }
}
