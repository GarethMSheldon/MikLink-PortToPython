/*
 * Purpose: Map MikroTik REST DTOs to domain models so ports remain DTO-free.
 * Inputs: MikroTik Retrofit DTOs returned by the API service.
 * Outputs: Domain objects used by test steps and reports (link status, cable test, ping, neighbor, speed test data).
 * Notes: Keep DTO usage localized to the data layer; adjust mappings centrally to avoid divergence across steps.
 */
package com.app.miklink.data.remote.mikrotik.mapper

import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.model.report.TdrEntry
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.data.remote.mikrotik.dto.PingResult
import com.app.miklink.data.remote.mikrotik.dto.SpeedTestResult

internal fun MonitorResponse.toLinkStatusData(): LinkStatusData =
    LinkStatusData(status = status, rate = rate)

internal fun CableTestResult.toSummary(): CableTestSummary {
    val entries = cablePairs.orEmpty().mapNotNull { pair ->
        val distance = pair["distance"] ?: pair["len"] ?: pair["length"]
        val statusValue = pair["status"] ?: pair["state"] ?: pair["result"]
        val description = pair["pair"] ?: pair["description"]
        if (distance == null && statusValue == null && description == null) {
            null
        } else {
            TdrEntry(
                distance = distance,
                status = statusValue,
                description = description
            )
        }
    }.ifEmpty {
        listOf(TdrEntry(status = status))
    }
    return CableTestSummary(status = status, entries = entries)
}

internal fun PingResult.toMeasurement(): PingMeasurement =
    PingMeasurement(
        host = host,
        minRtt = minRtt,
        avgRtt = avgRtt,
        maxRtt = maxRtt,
        packetLoss = packetLoss,
        sent = sent,
        received = received,
        seq = seq,
        time = time,
        ttl = ttl,
        size = size
    )

internal fun NeighborDetail.toDomain(): NeighborData =
    NeighborData(
        identity = identity,
        interfaceName = interfaceName,
        discoveredBy = discoveredBy,
        vlanId = vlanId,
        voiceVlanId = voiceVlanId,
        poeClass = poeClass,
        systemDescription = systemDescription,
        portId = portId
    )

internal fun SpeedTestResult.toDomain(serverAddress: String?): SpeedTestData =
    SpeedTestData(
        status = status,
        ping = ping,
        jitter = jitter,
        loss = loss,
        tcpDownload = tcpDownload,
        tcpUpload = tcpUpload,
        udpDownload = udpDownload,
        udpUpload = udpUpload,
        warning = warning,
        serverAddress = serverAddress
    )
