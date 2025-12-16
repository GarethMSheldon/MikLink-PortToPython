/*
 * Purpose: Centralize execution log configuration to avoid divergent limits or markers across producers/consumers.
 * Inputs: None; consumers read constants to size buffers or display markers.
 * Outputs: Constants used by ExecutionLogBuffer, ViewModels, and tests to enforce consistent log policies.
 */
package com.app.miklink.core.domain.test.logging

object ExecutionLogsConfig {
    const val MAX_LINES = 600
    const val TRIMMING_MARKER = "... (older logs trimmed)"
}
