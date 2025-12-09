package com.app.miklink.utils

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectivityProviderTest {
    private lateinit var mockServer: MockWebServer

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `isHttpReachable returns true when server OK`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))

        val client = OkHttpClient.Builder().build()
        val provider = ConnectivityProvider(client)

        val ok = provider.isHttpReachable(mockServer.hostName, mockServer.port)
        assertTrue(ok)
    }

    @Test
    fun `isHttpReachable returns false on server error`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(500))

        val client = OkHttpClient.Builder().build()
        val provider = ConnectivityProvider(client)

        val ok = provider.isHttpReachable(mockServer.hostName, mockServer.port)
        assertFalse(ok)
    }

    @Test
    fun `isHttpReachable supports https with held certificate`() = runBlocking {
        val held = HeldCertificate.Builder().addSubjectAlternativeName(mockServer.hostName).build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(held).build()
        mockServer.useHttps(serverCertificates.sslSocketFactory(), false)

        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(held.certificate).build()
        val client = OkHttpClient.Builder().sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).hostnameVerifier { _, _ -> true }.build()

        mockServer.enqueue(MockResponse().setResponseCode(200))

        val provider = ConnectivityProvider(client)
        val ok = provider.isHttpReachable(mockServer.hostName, mockServer.port, isHttps = true)
        assertTrue(ok)
    }

    @Test
    fun `isTcpReachable succeeds connecting to server port`() = runBlocking {
        mockServer.enqueue(MockResponse().setResponseCode(200))

        val client = OkHttpClient.Builder().build()
        val provider = ConnectivityProvider(client)

        val ok = provider.isTcpReachable(mockServer.hostName, mockServer.port)
        assertTrue(ok)
    }

    @Test
    fun `isTcpReachable returns false when port closed`() = runBlocking {
        val client = OkHttpClient.Builder().build()
        val provider = ConnectivityProvider(client)

        // Very likely unused high port — expect false quickly with short timeout
        val ok = provider.isTcpReachable("127.0.0.1", 65000, timeoutMs = 200)
        assertFalse(ok)
    }
}
