/*
 * Purpose: Ensure LogSanitizer redacts sensitive tokens and truncates overly long log lines.
 * Inputs: Raw log strings containing secrets or excessive length.
 * Outputs: Sanitized strings with redaction tokens and truncated suffix markers.
 */
package com.app.miklink.core.domain.test.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    private val sanitizer = LogSanitizer()

    @Test
    fun `redacts password and tokens`() {
        val raw = "password=hunter2 token=abcd probePassword=secret"
        val sanitized = sanitizer.sanitize(raw)
        assertEquals("password=<redacted> token=<redacted> probePassword=<redacted>", sanitized)
    }

    @Test
    fun `redacts authorization header`() {
        val raw = "Authorization: Bearer 123456"
        val sanitized = sanitizer.sanitize(raw)
        assertEquals("Authorization: <redacted>", sanitized)
    }

    @Test
    fun `truncates long lines`() {
        val long = buildString {
            repeat(510) { append('x') }
        }
        val sanitized = sanitizer.sanitize(long)
        assertEquals(500 + " ...[truncated]".length, sanitized.length)
        assertTrue(sanitized.endsWith("...[truncated]"))
    }
}
