/*
 * Purpose: Verify ExecutionLogBuffer keeps a bounded log list with trimming marker behavior.
 * Inputs: Sequence of log lines with varying sizes and buffer capacities.
 * Outputs: Assertions on snapshot contents, order, and marker presence after trimming.
 */
package com.app.miklink.core.domain.test.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionLogBufferTest {

    @Test
    fun `appends lines until max without trimming`() {
        val buffer = ExecutionLogBuffer(maxLines = 3, trimmingMarker = ExecutionLogsConfig.TRIMMING_MARKER)
        buffer.append("a")
        buffer.append("b")
        buffer.append("c")

        assertEquals(listOf("a", "b", "c"), buffer.snapshot())
    }

    @Test
    fun `trims old entries and prepends marker once`() {
        val buffer = ExecutionLogBuffer(maxLines = 3, trimmingMarker = ExecutionLogsConfig.TRIMMING_MARKER)
        buffer.append("1")
        buffer.append("2")
        buffer.append("3")
        buffer.append("4") // triggers trim

        val snapshot = buffer.snapshot()
        assertEquals(3, snapshot.size)
        assertEquals(ExecutionLogsConfig.TRIMMING_MARKER, snapshot.first())
        assertEquals(listOf(ExecutionLogsConfig.TRIMMING_MARKER, "3", "4"), snapshot)
    }

    @Test
    fun `trimming marker does not duplicate on subsequent trims`() {
        val buffer = ExecutionLogBuffer(maxLines = 2, trimmingMarker = ExecutionLogsConfig.TRIMMING_MARKER)
        buffer.append("line-1")
        buffer.append("line-2")
        buffer.append("line-3") // trim once
        buffer.append("line-4") // trim again

        val snapshot = buffer.snapshot()
        assertEquals(2, snapshot.size)
        assertEquals(ExecutionLogsConfig.TRIMMING_MARKER, snapshot[0])
        assertTrue(snapshot.contains("line-4"))
    }
}
