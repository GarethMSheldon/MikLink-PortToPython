/*
 * Purpose: DTO for MikroTik speed test requests sent over the RouterOS HTTP API.
 * Inputs: Server address, credentials, and optional duration passed from repository callers.
 * Outputs: Serialized JSON payload consumed by MikroTik endpoints.
 */
package com.app.miklink.data.remote.mikrotik.dto

import com.squareup.moshi.Json

data class SpeedTestRequest(
    @field:Json(name = "address") val address: String,
    @field:Json(name = "user") val user: String,
    @field:Json(name = "password") val password: String,
    @field:Json(name = "test-duration") val testDuration: String = "5"
)
