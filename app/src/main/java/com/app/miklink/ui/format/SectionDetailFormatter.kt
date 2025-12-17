/*
 * Purpose: Provide a single source of truth for formatting section detail labels/values across test and history screens.
 * Inputs: Section identifiers plus raw key/value pairs emitted by domain/test flows.
 * Outputs: UiText-wrapped labels and values ready for UI rendering, with optional suppression of noisy fields.
 * Notes: Avoids scattered .replace()/.capitalize() calls and enforces consistent handling (e.g., hiding count=1 neighbors).
 */
package com.app.miklink.ui.format

import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.ui.common.UiText
import com.app.miklink.ui.test.TestSectionType

enum class SectionId {
    NETWORK,
    LINK,
    PING,
    LLDP,
    CDP,
    SPEED,
    UNKNOWN;

    companion object {
        fun fromTestSectionType(type: TestSectionType): SectionId = when (type) {
            TestSectionType.NETWORK -> NETWORK
            TestSectionType.LINK -> LINK
            TestSectionType.LLDP -> LLDP
            TestSectionType.PING -> PING
            TestSectionType.TDR -> UNKNOWN
            TestSectionType.SPEED -> SPEED
        }
    }
}

data class FormattedDetail(val label: UiText, val value: UiText)

object SectionDetailFormatter {
    fun format(section: SectionId, rawKey: String, rawValue: String): FormattedDetail? {
        val normalizedKey = rawKey.trim()
        val normalizedValue = rawValue.trim()

        if (section.isNeighbor() && normalizedKey.equals("count", ignoreCase = true)) {
            val count = normalizedValue.toIntOrNull()
            if (count == null || count <= 1) return null
            return FormattedDetail(
                label = UiText.Resource(R.string.detail_label_neighbors),
                value = UiText.Dynamic(count.toString())
            )
        }

        val label = formatLabel(section, normalizedKey) ?: return null
        val value = formatValue(section, normalizedKey, normalizedValue) ?: return null
        return FormattedDetail(label, value)
    }

    fun formatLabel(section: SectionId, rawKey: String): UiText? {
        val key = rawKey.trim()
        val normalized = key.lowercase()
        return when (section) {
            SectionId.NETWORK -> networkLabels[normalized]?.let { UiText.Resource(it) } ?: fallbackLabel(key)
            SectionId.LINK -> when (normalized) {
                "status" -> UiText.Resource(R.string.detail_label_status)
                "rate", "speed" -> UiText.Resource(R.string.detail_label_speed)
                else -> fallbackLabel(key)
            }
            SectionId.LLDP, SectionId.CDP -> when (normalized) {
                "identity" -> UiText.Resource(R.string.detail_label_identity)
                "interface" -> UiText.Resource(R.string.detail_label_interface)
                "protocol" -> UiText.Resource(R.string.detail_label_protocol)
                else -> fallbackLabel(key)
            }
            SectionId.PING -> when {
                normalized == "packet loss" || normalized == "loss" -> UiText.Resource(R.string.detail_label_packet_loss)
                normalized == "min rtt" -> UiText.Resource(R.string.detail_label_min_rtt)
                normalized == "avg rtt" || normalized == "average rtt" -> UiText.Resource(R.string.detail_label_avg_rtt)
                normalized == "max rtt" -> UiText.Resource(R.string.detail_label_max_rtt)
                normalized == "targets" -> UiText.Resource(R.string.detail_label_targets)
                normalized.startsWith("target") -> {
                    val suffix = key.substringAfter("target", "").trim().ifBlank { null }
                    suffix?.let { UiText.Resource(R.string.detail_label_target_number, listOf(it)) }
                        ?: UiText.Resource(R.string.detail_label_target_generic)
                }
                else -> fallbackLabel(key)
            }
            SectionId.SPEED -> when (normalized) {
                "server", "serveraddress" -> UiText.Resource(R.string.detail_label_server)
                "warning" -> UiText.Resource(R.string.detail_label_warning)
                else -> fallbackLabel(key)
            }
            SectionId.UNKNOWN -> fallbackLabel(key)
        }
    }

    fun formatValue(section: SectionId, rawKey: String, rawValue: String): UiText? {
        val normalizedValue = rawValue.trim()
        if (normalizedValue.equals("null", ignoreCase = true)) return UiText.Dynamic("-")
        if (normalizedValue.isEmpty()) return UiText.Dynamic("-")

        val normalizedKey = rawKey.trim().lowercase()
        if (normalizedKey == "reason") {
            return mapReasonToText(normalizedValue)
        }

        val token = normalizedValue.lowercase()
        valueTokens[token]?.let { return UiText.Resource(it) }

        if (section.isNeighbor() && normalizedKey == "protocol") {
            return UiText.Dynamic(token.uppercase())
        }

        return UiText.Dynamic(formatFallbackValue(normalizedValue))
    }

    private fun mapReasonToText(reason: String): UiText {
        return when (reason) {
            TestSkipReason.PING_NO_TARGETS -> UiText.Resource(R.string.skip_reason_ping_no_targets)
            TestSkipReason.PING_NO_VALID_TARGETS -> UiText.Resource(R.string.skip_reason_ping_no_valid_targets)
            TestSkipReason.SPEED_NO_SERVER -> UiText.Resource(R.string.skip_reason_speed_no_server)
            TestSkipReason.PROFILE_DISABLED -> UiText.Resource(R.string.skip_reason_profile_disabled)
            TestSkipReason.HARDWARE_UNSUPPORTED -> UiText.Resource(R.string.skip_reason_hardware_unsupported)
            else -> UiText.Dynamic(reason.ifBlank { "-" })
        }
    }

    private fun SectionId.isNeighbor(): Boolean = this == SectionId.LLDP || this == SectionId.CDP

    private fun fallbackLabel(rawKey: String): UiText = UiText.Dynamic(rawKey.toReadableTitle())

    private fun formatFallbackValue(value: String): String {
        if (value.contains("=")) return value // Keep structured summaries (e.g., loss=0% min=10ms)
        return value.toReadableTitle()
    }

    private val networkLabels = mapOf(
        "mode" to R.string.detail_label_mode,
        "interface" to R.string.detail_label_interface,
        "interface-name" to R.string.detail_label_interface,
        "address" to R.string.detail_label_address,
        "gateway" to R.string.detail_label_gateway,
        "dns" to R.string.detail_label_dns,
        "message" to R.string.detail_label_message,
        "error" to R.string.detail_label_error,
        "reason" to R.string.detail_label_reason
    )

    private val valueTokens = mapOf(
        "dhcp" to R.string.detail_value_dhcp,
        "static" to R.string.detail_value_static,
        "link-ok" to R.string.detail_value_link_ok,
        "running" to R.string.detail_value_running,
        "bound" to R.string.detail_value_bound,
        "skip" to R.string.detail_value_skipped,
        "skipped" to R.string.detail_value_skipped
    )
}

private fun String.toReadableTitle(): String {
    val cleaned = replace("_", " ").replace("-", " ").trim()
    if (cleaned.isBlank()) return "-"
    return cleaned
        .split("\\s+".toRegex())
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }
}
