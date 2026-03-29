/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.os.SystemClock
import androidx.lifecycle.ViewModel

internal class FirstLaunchAcknowledgementViewModel : ViewModel() {
    private var deadlineElapsedRealtime: Long? = null

    fun startCountdownIfNeeded(durationMs: Long) {
        if (deadlineElapsedRealtime == null) {
            deadlineElapsedRealtime = SystemClock.elapsedRealtime() + durationMs
        }
    }

    fun remainingSeconds(): Int {
        val deadline = deadlineElapsedRealtime ?: return 0
        val remainingMs = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        return if (remainingMs == 0L) 0 else ((remainingMs + 999L) / 1000L).toInt()
    }

    fun isCountdownComplete(): Boolean = remainingSeconds() == 0
}
