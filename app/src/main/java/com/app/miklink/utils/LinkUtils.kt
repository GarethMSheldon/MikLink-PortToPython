package com.app.miklink.utils

import androidx.annotation.StringRes
import com.app.miklink.R

/**
 * Normalizes link status strings for consistent display.
 * 
 * Logic:
 * - "link-ok", "up", "running", "ok" -> R.string.link_status_connected
 * - "no-link", "down" -> R.string.link_status_disconnected
 * - "unknown" -> R.string.link_status_unknown
 * - Everything else -> R.string.link_status_na (fallback)
 * 
 * @return String resource ID for the normalized status
 */
@StringRes
fun normalizeLinkStatus(status: String?): Int {
    if (status.isNullOrBlank()) return R.string.link_status_na
    return when (status.lowercase().trim()) {
        "link-ok", "up", "running", "ok" -> R.string.link_status_connected
        "no-link", "down" -> R.string.link_status_disconnected
        "unknown" -> R.string.link_status_unknown
        else -> R.string.link_status_na
    }
}

/**
 * Normalizes link speed strings for consistent display.
 * 
 * Logic:
 * - Adds space between value and unit (e.g. "1Gbps" -> "1 Gbps")
 * - Supports Mbps, Gbps (up to 400Gbps)
 * - Retains original string if format is not recognized
 */
fun normalizeLinkSpeed(speed: String?): String {
    if (speed.isNullOrBlank()) return "N/A"
    // Remove extra spaces first
    val s = speed.trim().replace(" ", "")
    
    // Check if it ends with "bps" (case insensitive)
    return when {
        s.endsWith("bps", ignoreCase = true) -> {
            // Find where digits end and letters start
            val unitIndex = s.indexOfFirst { c -> c.isLetter() }
            if (unitIndex > 0) {
                "${s.substring(0, unitIndex)} ${s.substring(unitIndex)}"
            } else s
        }
        else -> s
    }
}
