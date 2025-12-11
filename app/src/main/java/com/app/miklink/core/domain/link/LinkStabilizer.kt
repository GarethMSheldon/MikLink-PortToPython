package com.app.miklink.core.domain.link

/**
 * LinkStabilizer - placeholder for link stabilization rules.
 *
 * Responsibilities:
 * - decide whether a link has stabilized based on status and retry/timeouts
 */
interface LinkStabilizer {
    fun isLinkStable(currentStatus: LinkStatus, elapsedMs: Long): Boolean
}

data class LinkStatus(val isUp: Boolean, val rate: String?)
