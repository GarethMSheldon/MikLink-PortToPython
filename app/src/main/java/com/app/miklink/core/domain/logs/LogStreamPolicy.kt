package com.app.miklink.core.domain.logs

/**
 * LogStreamPolicy - placeholder describing streaming vs polling trade-offs
 *
 * Responsibilities:
 * - decide whether to attempt streaming or fallback to polling depending on device capabilities
 */
sealed class LoggingMode {
    object Streaming : LoggingMode()
    object Polling : LoggingMode()
}

interface LogStreamPolicy {
    fun selectMode(deviceCapabilities: DeviceCapabilities): LoggingMode
}

data class DeviceCapabilities(val supportsStreaming: Boolean, val routerOsVersion: String?)
