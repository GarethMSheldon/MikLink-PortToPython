/*
 * Purpose: Sanitize execution log lines before they reach the UI by redacting credentials/tokens and truncating oversized content.
 * Inputs: Raw log strings that may contain sensitive tokens or long payloads.
 * Outputs: Safe, trimmed log strings with secrets replaced and overly long lines shortened.
 */
package com.app.miklink.core.domain.test.logging

class LogSanitizer(
    private val redactionToken: String = "<redacted>",
    private val maxLength: Int = MAX_LENGTH
) {
    fun sanitize(message: String): String {
        var safe = message.trim()
        if (safe.isEmpty()) return safe

        redactionPatterns.forEach { pattern ->
            safe = pattern.replace(safe) { matchResult ->
                val prefix = matchResult.groups[1]?.value ?: ""
                "$prefix$redactionToken"
            }
        }

        if (safe.length > maxLength) {
            safe = safe.take(maxLength) + " ...[truncated]"
        }

        return safe
    }

    companion object {
        private const val MAX_LENGTH = 500
        private val redactionPatterns = listOf(
            // password=secret
            Regex("(?i)(password\\s*=\\s*)([^;\\s,]+)"),
            // token=abcd
            Regex("(?i)(token\\s*=\\s*)([^;\\s,]+)"),
            // probePassword=xxx / probeToken=xxx
            Regex("(?i)(probe(password|token)\\s*=\\s*)([^;\\s,]+)"),
            // Authorization: Bearer xyz
            Regex("(?i)(authorization:\\s*)([^\\n\\r]+)"),
            // Generic secret=xxx
            Regex("(?i)(secret\\s*=\\s*)([^;\\s,]+)")
        )
    }
}
