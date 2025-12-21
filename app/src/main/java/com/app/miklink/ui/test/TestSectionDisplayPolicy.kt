/*
 * Purpose: Define ordering/visibility rules for test execution sections in UI.
 * Inputs: TestSectionSnapshot lists from TestRunSnapshot.
 * Outputs: Ordered/filtered lists and expandability decisions.
 * Notes: Running view hides PENDING items; details expand only on final statuses.
 */
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
     * Progressive reveal: show sections once they start (RUNNING) or finish.
     */
    fun visibleForRunning(sections: List<TestSectionSnapshot>): List<TestSectionSnapshot> =
        sections.filter { it.status != TestSectionStatus.PENDING }

    fun isExpandable(status: TestSectionStatus): Boolean =
        status in finalStatuses
}
