/*
 * Purpose: Represent UI text as either raw strings or string resources to centralize formatting and localization.
 * Inputs: Resource identifiers with optional format arguments or already formatted literal strings.
 * Outputs: Resolved strings for Compose/UI consumers via asString() helpers or direct resource/context resolution.
 * Notes: Keeps formatting logic testable without Compose by exposing resolve(context) alongside the Compose helper.
 */
package com.app.miklink.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(@param:StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    fun resolve(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args.toTypedArray())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId, *args.toTypedArray())
}
