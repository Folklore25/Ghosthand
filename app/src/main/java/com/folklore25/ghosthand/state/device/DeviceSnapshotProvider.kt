/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Surface
import android.view.WindowManager

class DeviceSnapshotProvider(
    private val context: Context
) {
    fun snapshot(): DeviceSnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPercent = batteryIntent?.let(::readBatteryPercent)
        val charging = batteryIntent?.let(::readCharging) ?: false

        return DeviceSnapshot(
            screenOn = readScreenOn(),
            locked = null,
            rotation = readRotation(),
            batteryPercent = batteryPercent,
            charging = charging
        )
    }

    private fun readScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    private fun readRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = try {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
        return when (rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun readBatteryPercent(intent: Intent): Int? {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) {
            return null
        }
        return (level * 100) / scale
    }

    private fun readCharging(intent: Intent): Boolean {
        return when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> true
            else -> false
        }
    }
}

data class DeviceSnapshot(
    val screenOn: Boolean,
    val locked: Boolean?,
    val rotation: Int,
    val batteryPercent: Int?,
    val charging: Boolean
)
