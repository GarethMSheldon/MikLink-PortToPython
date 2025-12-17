/*
 * Purpose: Check MikroTik probe connectivity and surface board/interface metadata to the app layer.
 * Inputs: Probe configuration, MikroTikServiceProvider, and application context for localized errors.
 * Outputs: ProbeCheckResult indicating success with metadata or an error message.
 */
package com.app.miklink.data.repositoryimpl.mikrotik

import android.content.Context
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.SSLHandshakeException
import javax.inject.Inject

/**
 * Implementazione di ProbeConnectivityRepository.
 *
 * Usa MikroTikServiceProvider per costruire il service e chiama l'API MikroTik
 * per verificare la connessione e ottenere informazioni hardware.
 */
class ProbeConnectivityRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serviceProvider: MikroTikServiceProvider
) : ProbeConnectivityRepository {

    private val logTag = "ProbeConnectivityRepository"

    override suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult =
        withContext(Dispatchers.IO) {
            runCatching { fetchProbeMetadata(probe) }
                .getOrElse { error ->
                    if (error is SSLHandshakeException && probe.isHttps) {
                        if (android.util.Log.isLoggable(logTag, android.util.Log.WARN)) {
                            android.util.Log.w(
                                logTag,
                                "TLS handshake failed over HTTPS, retrying with HTTP fallback.",
                                error
                            )
                        }
                        runCatching { fetchProbeMetadata(probe.copy(isHttps = false)) }
                            .getOrElse { fallbackError ->
                                ProbeCheckResult.Error(
                                    ProbeErrorMapper.toMessage(
                                        error = error,
                                        defaultMessage = context.getString(R.string.error_probe_connection_unknown),
                                        handshakeMessage = context.getString(R.string.error_probe_connection_tls_handshake)
                                    )
                                )
                            }
                    } else {
                        android.util.Log.e(logTag, "checkProbeConnection: Errore durante la verifica", error)
                        val message = ProbeErrorMapper.toMessage(
                            error = error,
                            defaultMessage = context.getString(R.string.error_probe_connection_unknown),
                            handshakeMessage = context.getString(R.string.error_probe_connection_tls_handshake)
                        )
                        ProbeCheckResult.Error(message)
                    }
                }
        }

    private suspend fun fetchProbeMetadata(probe: ProbeConfig): ProbeCheckResult.Success {
        val api = serviceProvider.build(probe)
        val systemResources = api.getSystemResource(
            com.app.miklink.data.remote.mikrotik.dto.ProplistRequest(listOf("board-name"))
        )
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(
                logTag,
                "systemResource response (${if (probe.isHttps) "https" else "http"}): ${
                    systemResources.joinToString { resource ->
                        resource.boardName ?: "<null>"
                    }
                }"
            )
        }
        val boardName = systemResources
            .mapNotNull { it.boardName?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .firstOrNull()
            ?: "Unknown Board"
        val interfacesRaw = api.getEthernetInterfaces()
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(logTag, "checkProbeConnection: Ricevute ${interfacesRaw.size} interfacce dall'API")
        }
        val interfaces = interfacesRaw.map {
            if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
                android.util.Log.d(logTag, "checkProbeConnection: Interface name = '${it.name}'")
            }
            it.name
        }
        if (android.util.Log.isLoggable(logTag, android.util.Log.DEBUG)) {
            android.util.Log.d(logTag, "checkProbeConnection: Interfacce mappate: $interfaces")
        }
        return ProbeCheckResult.Success(boardName, interfaces)
    }
}
