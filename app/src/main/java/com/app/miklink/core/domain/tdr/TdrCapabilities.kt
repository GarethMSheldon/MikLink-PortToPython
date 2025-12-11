package com.app.miklink.core.domain.tdr

/**
 * TdrCapabilities - source of truth for TDR support per device model/board.
 *
 * Responsibilities:
 * - Provide authoritative rules to determine whether a probe supports TDR
 * - Implementations consult model/board-name mapping (device compatibility)
 *
 * Notes for integration:
 * - `ProbeConfig.tdrSupported` in the persistence layer is a cached boolean for
 *   performance/UI convenience and should **not** be used as the source of truth.
 * - The domain `TdrCapabilities` must be used when deciding to run TDR or when
 *   deriving TDR-related behavior. Update the cached `ProbeConfig.tdrSupported` in
 *   a background process or when probe discovery occurs.
 */
sealed class TdrSupportStatus {
    object Supported : TdrSupportStatus()
    object NotSupported : TdrSupportStatus()
    object Unknown : TdrSupportStatus()
}

interface TdrCapabilities {
    /**
     * Return the authoritative support status for the given model/board name.
     *
     * Implementations should avoid side-effects and only return the derived result
     * (e.g. Supported/NotSupported/Unknown). Caching or updating `ProbeConfig.tdrSupported`
     * should be performed outside this pure domain function.
     */
    fun checkSupport(modelName: String?): TdrSupportStatus
}
