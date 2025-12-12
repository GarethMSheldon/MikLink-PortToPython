package com.app.miklink.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.app.miklink.R

@Composable
fun mapTestSectionTitle(type: TestSectionType, fallback: String? = null): String {
    val res = when (type) {
        TestSectionType.NETWORK -> stringResource(R.string.section_network)
        TestSectionType.LINK -> stringResource(R.string.section_link)
        TestSectionType.LLDP -> stringResource(R.string.section_lldp)
        TestSectionType.PING -> stringResource(R.string.section_ping)
        TestSectionType.TDR -> stringResource(R.string.section_tdr)
        TestSectionType.SPEED -> stringResource(R.string.section_speed)
    }
    return res.ifBlank { fallback ?: type.name }
}
