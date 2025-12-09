package com.app.miklink.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Small, testable connectivity helper providing HTTP and TCP reachability checks.
 * Designed for use in ViewModels / Repositories when probing a device.
 */
class ConnectivityProvider(private val client: OkHttpClient) {

    /**
     * Performs a lightweight HTTP HEAD request to the given host/port and returns true if the
     * response is successful (2xx).
     * The method uses IO dispatcher and will not block the main thread.
     */
    suspend fun isHttpReachable(host: String, port: Int, isHttps: Boolean = false, path: String = "/"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val scheme = if (isHttps) "https" else "http"
                val url = HttpUrl.Builder()
                    .scheme(scheme)
                    .host(host)
                    .port(port)
                    .addPathSegments(path.trimStart('/'))
                    .build()

                val req = Request.Builder().url(url).head().build()
                client.newCall(req).execute().use { resp ->
                    resp.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Tries to open a TCP socket to host:port within the provided timeout (ms).
     * Returns true if connect succeeds.
     */
    suspend fun isTcpReachable(host: String, port: Int, timeoutMs: Int = 1000): Boolean {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            } catch (e: Exception) {
                false
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }
}
