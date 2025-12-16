/*
 * Purpose: DTO for MikroTik speed test responses parsed from RouterOS HTTP API calls.
 * Inputs: Raw JSON payloads with ping, jitter, throughput, and warning fields.
 * Outputs: Structured results for repository/domain mapping.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

data class SpeedTestResult(
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "ping-min-avg-max") val ping: String?,
    @field:Json(name = "jitter-min-avg-max") val jitter: String?,
    @field:Json(name = "loss") val loss: String?,
    @field:Json(name = "tcp-download") val tcpDownload: String?,
    @field:Json(name = "tcp-upload") val tcpUpload: String?,
    @field:Json(name = "udp-download") val udpDownload: String?,
    @field:Json(name = "udp-upload") val udpUpload: String?,
    @field:Json(name = ".about") val warning: String? // Messaggio CPU warning
)
