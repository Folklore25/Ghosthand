/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat

internal enum class StatusTone(
    val backgroundRes: Int,
    val textRes: Int
) {
    Success(R.color.gh_chip_success_bg, R.color.gh_chip_success_fg),
    Warning(R.color.gh_chip_warning_bg, R.color.gh_chip_warning_fg),
    Danger(R.color.gh_chip_error_bg, R.color.gh_chip_error_fg),
    Neutral(R.color.gh_chip_neutral_bg, R.color.gh_chip_neutral_fg)
}

internal object UiStatusSupport {
    fun styleChip(context: Context, view: TextView, tone: StatusTone) {
        view.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, tone.backgroundRes)
        )
        view.setTextColor(ContextCompat.getColor(context, tone.textRes))
    }

    fun booleanText(textLookup: UiTextLookup, value: Boolean): String {
        return if (value) textLookup.getString(R.string.runtime_boolean_true)
        else textLookup.getString(R.string.runtime_boolean_false)
    }

    fun booleanText(context: Context, value: Boolean): String =
        booleanText(context.asTextLookup(), value)

    fun accessibilityStatusText(textLookup: UiTextLookup, status: String): String {
        return when (status) {
            "disabled" -> textLookup.getString(R.string.accessibility_status_disabled)
            "enabled_idle" -> textLookup.getString(R.string.accessibility_status_enabled_idle)
            "enabled_connected" -> textLookup.getString(R.string.accessibility_status_enabled_connected)
            else -> status
        }
    }

    fun accessibilityStatusText(context: Context, status: String): String =
        accessibilityStatusText(context.asTextLookup(), status)

    fun rootStatusText(textLookup: UiTextLookup, status: String): String {
        return when (status) {
            "available" -> textLookup.getString(R.string.root_status_available)
            "authorization_required" -> textLookup.getString(R.string.root_status_authorization_required)
            "unavailable" -> textLookup.getString(R.string.root_status_unavailable)
            else -> textLookup.getString(R.string.runtime_boolean_unknown)
        }
    }

    fun rootStatusText(context: Context, status: String): String =
        rootStatusText(context.asTextLookup(), status)

    fun screenshotSystemStatusText(
        textLookup: UiTextLookup,
        system: ScreenshotSystemAuthorizationState
    ): String {
        return when {
            system.mediaProjectionGranted ->
                textLookup.getString(R.string.permission_screenshot_system_projection)
            system.accessibilityCaptureReady ->
                textLookup.getString(R.string.permission_screenshot_system_accessibility)
            system.rootFallbackAvailable ->
                textLookup.getString(R.string.permission_screenshot_system_root_fallback)
            else ->
                textLookup.getString(R.string.permission_system_missing)
        }
    }

    fun screenshotSystemStatusText(context: Context, system: ScreenshotSystemAuthorizationState): String =
        screenshotSystemStatusText(context.asTextLookup(), system)

    fun policyStatusText(textLookup: UiTextLookup, allowed: Boolean): String {
        return if (allowed) textLookup.getString(R.string.permission_policy_allowed)
        else textLookup.getString(R.string.permission_policy_blocked)
    }

    fun policyStatusText(context: Context, allowed: Boolean): String =
        policyStatusText(context.asTextLookup(), allowed)

    fun effectiveStatusText(
        textLookup: UiTextLookup,
        effective: CapabilityEffectiveState
    ): String {
        if (!effective.usableNow) {
            return when (effective.reason) {
                "policy_blocked" -> textLookup.getString(R.string.permission_effective_policy_blocked)
                "authorization_required" -> textLookup.getString(R.string.permission_effective_authorization_required)
                "service_idle" -> textLookup.getString(R.string.permission_effective_service_idle)
                else -> textLookup.getString(R.string.permission_effective_unavailable)
            }
        }

        return when (effective.reason) {
            "root_fallback" -> textLookup.getString(R.string.permission_effective_root_fallback)
            else -> textLookup.getString(R.string.permission_effective_available)
        }
    }

    fun booleanTone(value: Boolean): StatusTone = if (value) StatusTone.Success else StatusTone.Neutral

    fun accessibilityTone(status: String): StatusTone {
        return when (status) {
            "enabled_connected" -> StatusTone.Success
            "enabled_idle" -> StatusTone.Warning
            else -> StatusTone.Neutral
        }
    }

    fun rootTone(status: String): StatusTone {
        return when (status) {
            "available" -> StatusTone.Success
            "authorization_required" -> StatusTone.Warning
            else -> StatusTone.Neutral
        }
    }

    fun policyTone(allowed: Boolean): StatusTone = if (allowed) StatusTone.Success else StatusTone.Neutral

    fun screenshotSystemTone(system: ScreenshotSystemAuthorizationState): StatusTone {
        return when {
            system.mediaProjectionGranted ||
                system.accessibilityCaptureReady ||
                system.rootFallbackAvailable -> StatusTone.Success
            else -> StatusTone.Neutral
        }
    }

    fun effectiveTone(available: Boolean): StatusTone = if (available) StatusTone.Success else StatusTone.Warning

    private fun Context.asTextLookup(): UiTextLookup {
        return object : UiTextLookup {
            override fun getString(resId: Int, vararg args: Any): String {
                return this@asTextLookup.getString(resId, *args)
            }
        }
    }
}
