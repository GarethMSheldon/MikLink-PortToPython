/*
 * Purpose: Verify SectionDetailFormatter produces consistent labels/values and hides noisy fields per UX rules.
 * Inputs: Section identifiers with raw key/value pairs mirroring domain detail payloads.
 * Outputs: FormattedDetail instances with UiText resources or dynamic strings used by the UI layer.
 * Notes: Ensures single-source formatting for test/history detail views and prevents regressions on label/value mapping.
 */
package com.app.miklink.ui.format

import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.ui.common.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SectionDetailFormatterTest {

    @Test
    fun `mode label uses resource and dhcp is mapped`() {
        val formatted = SectionDetailFormatter.format(SectionId.NETWORK, "mode", "dhcp")
        assertNotNull(formatted)
        assertEquals(UiText.Resource(R.string.detail_label_mode), formatted?.label)
        assertEquals(UiText.Resource(R.string.detail_value_dhcp), formatted?.value)
    }

    @Test
    fun `link ok value is human readable`() {
        val formatted = SectionDetailFormatter.format(SectionId.LINK, "status", "link-ok")
        assertNotNull(formatted)
        assertEquals(UiText.Resource(R.string.detail_label_status), formatted?.label)
        assertEquals(UiText.Resource(R.string.detail_value_link_ok), formatted?.value)
    }

    @Test
    fun `neighbor count one is hidden`() {
        val formatted = SectionDetailFormatter.format(SectionId.LLDP, "count", "1")
        assertNull(formatted)
    }

    @Test
    fun `neighbor count greater than zero is rendered`() {
        val formatted = SectionDetailFormatter.format(SectionId.LLDP, "count", "3")
        assertNotNull(formatted)
        assertEquals(UiText.Resource(R.string.detail_label_neighbors), formatted?.label)
        assertEquals(UiText.Dynamic("3"), formatted?.value)
    }

    @Test
    fun `fallback label title cases raw keys`() {
        val formatted = SectionDetailFormatter.format(SectionId.LLDP, "voice-vlan-id", "12")
        assertNotNull(formatted)
        assertEquals(UiText.Dynamic("Voice Vlan Id"), formatted?.label)
        assertEquals(UiText.Dynamic("12"), formatted?.value)
    }

    @Test
    fun `reason values map to localized resources`() {
        val formatted = SectionDetailFormatter.format(SectionId.NETWORK, "reason", TestSkipReason.PROFILE_DISABLED)
        assertNotNull(formatted)
        assertEquals(UiText.Resource(R.string.detail_label_reason), formatted?.label)
        assertEquals(UiText.Resource(R.string.skip_reason_profile_disabled), formatted?.value)
    }
}
