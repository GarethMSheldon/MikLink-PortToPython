package com.app.miklink.ui.history.model

import com.app.miklink.data.network.dto.CableTestResult
import com.app.miklink.data.network.dto.NeighborDetail
import com.app.miklink.data.network.dto.PingResult
import com.app.miklink.data.network.dto.SpeedTestResult

/**
 * A data class to hold the results of a test after being parsed from JSON.
 */
data class ParsedResults(
    val tdr: List<CableTestResult>? = null,
    val link: Map<String, String>? = null, // Placeholder for link status
    val lldp: List<NeighborDetail>? = null,
    val ping: List<PingResult>? = null,
    val speedTest: SpeedTestResult? = null
)
