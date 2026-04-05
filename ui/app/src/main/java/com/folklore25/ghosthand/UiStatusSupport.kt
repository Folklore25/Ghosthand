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

    fun booleanText(context: Context, value: Boolean): String {
        return if (value) context.getString(R.string.runtime_boolean_true)
        else context.getString(R.string.runtime_boolean_false)
    }

    fun accessibilityStatusText(context: Context, status: String): String {
        return when (status) {
            "disabled" -> context.getString(R.string.accessibility_status_disabled)
            "enabled_idle" -> context.getString(R.string.accessibility_status_enabled_idle)
            "enabled_connected" -> context.getString(R.string.accessibility_status_enabled_connected)
            else -> status
        }
    }

    fun rootStatusText(context: Context, status: String): String {
        return when (status) {
            "available" -> context.getString(R.string.root_status_available)
            "authorization_required" -> context.getString(R.string.root_status_authorization_required)
            "unavailable" -> context.getString(R.string.root_status_unavailable)
            else -> context.getString(R.string.runtime_boolean_unknown)
        }
    }

    fun policyStatusText(context: Context, allowed: Boolean): String {
        return if (allowed) context.getString(R.string.permission_policy_allowed)
        else context.getString(R.string.permission_policy_blocked)
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
}
