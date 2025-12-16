/*
 * Purpose: Maintain a bounded, in-memory buffer of execution log lines for the UI without persisting or leaking data.
 * Inputs: Individual log lines appended via append(); optional clear() when restarting a test.
 * Outputs: Snapshot of the latest sanitized lines limited by ExecutionLogsConfig.MAX_LINES, with a trimming marker when older lines are dropped.
 */
package com.app.miklink.core.domain.test.logging

class ExecutionLogBuffer(
    private val maxLines: Int = ExecutionLogsConfig.MAX_LINES,
    private val trimmingMarker: String = ExecutionLogsConfig.TRIMMING_MARKER
) {
    private val lines = mutableListOf<String>()

    @Synchronized
    fun append(line: String) {
        if (line.isBlank()) return
        lines += line.trimEnd()
        trimIfNeeded()
    }

    @Synchronized
    fun clear() {
        lines.clear()
    }

    @Synchronized
    fun snapshot(): List<String> = lines.toList()

    private fun trimIfNeeded() {
        if (lines.size <= maxLines) return
        val overflow = lines.size - maxLines
        repeat(overflow) { lines.removeAt(0) }
        if (trimmingMarker.isNotBlank()) {
            if (lines.size == maxLines) {
                lines.removeAt(0)
            }
            if (lines.isEmpty() || lines.first() != trimmingMarker) {
                lines.add(0, trimmingMarker)
            }
        }
    }
}
