/*
 * Purpose: Define MikroTik test operations contract using domain models to avoid DTO leaks.
 * Inputs: Probe configuration plus per-call parameters (interface name, targets, credentials).
 * Outputs: Domain representations of link status, cable test summaries, ping samples, neighbors, and speed test data.
 * Notes: Mapping from Retrofit DTOs lives in data/remote/mikrotik/mapper; keep this interface framework-free.
 */
package com.app.miklink.core.data.repository.test

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement

interface MikroTikTestRepository {
    suspend fun monitorEthernet(probe: ProbeConfig, interfaceName: String, once: Boolean = true): LinkStatusData
    suspend fun cableTest(probe: ProbeConfig, interfaceName: String, once: Boolean = true): CableTestSummary
    suspend fun ping(probe: ProbeConfig, target: String, interfaceName: String?, count: Int): List<PingMeasurement>
    suspend fun neighbors(probe: ProbeConfig, interfaceName: String): List<NeighborData>
    suspend fun speedTest(
        probe: ProbeConfig,
        serverAddress: String,
        username: String? = null,
        password: String? = null,
        duration: String = "5"
    ): SpeedTestData
}
