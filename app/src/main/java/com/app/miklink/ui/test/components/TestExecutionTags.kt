/*
 * Purpose: Centralize semantics tags for Test Execution UI to keep instrumentation tests stable and localization-agnostic.
 * Inputs: None; constants are consumed by composables when applying testTag modifiers.
 * Outputs: Stable string tags used by UI tests to query toggles and log panels.
 */
package com.app.miklink.ui.test.components

object TestExecutionTags {
    const val IN_PROGRESS_TOGGLE = "test_execution_toggle_in_progress_logs"
    const val COMPLETED_TOGGLE = "test_execution_toggle_completed_logs"
    const val LOG_PANE = "test_execution_log_pane"
    const val LOG_LINE = "test_execution_log_line"
}
