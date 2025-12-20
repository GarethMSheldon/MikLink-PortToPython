// UI component/screen: test execution section ordering; input state: snapshots; output rendering: ordered/visible lists.
package com.app.miklink.ui.test

import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus

object TestSectionDisplayPolicy {
    private val orderedIds = listOf(
        TestSectionId.LINK,
        TestSectionId.NETWORK,
        TestSectionId.TDR,
        TestSectionId.NEIGHBORS,
        TestSectionId.PING,
        TestSectionId.SPEED
    )

    private val finalStatuses = setOf(
        TestSectionStatus.PASS,
        TestSectionStatus.FAIL,
        TestSectionStatus.SKIP,
        TestSectionStatus.INFO
    )

    fun ordered(sections: List<TestSectionSnapshot>): List<TestSectionSnapshot> =
        sections.sortedBy { orderedIds.indexOf(it.id).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE }

    /**
     * Progressive reveal: keep all final sections plus only the first pending/running section.
     */
    fun visibleForRunning(sections: List<TestSectionSnapshot>): List<TestSectionSnapshot> {
        val visible = mutableListOf<TestSectionSnapshot>()
        var pendingIncluded = false
        sections.forEach { section ->
            val isFinal = section.status in finalStatuses
            val isPending = section.status == TestSectionStatus.PENDING || section.status == TestSectionStatus.RUNNING
            if (isFinal || (isPending && !pendingIncluded)) {
                visible += section
            }
            if (isPending && !pendingIncluded) pendingIncluded = true
        }
        return visible
    }

    fun isExpandable(status: TestSectionStatus): Boolean =
        status in finalStatuses || status == TestSectionStatus.RUNNING || status == TestSectionStatus.PENDING
}
