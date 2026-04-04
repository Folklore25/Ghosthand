/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.accessibility

import com.folklore25.ghosthand.R

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GhostProbeAccessibilityService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        Log.i(LOG_TAG, "Probe accessibility service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(LOG_TAG, "Probe accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Diagnostic probe only.
    }

    override fun onInterrupt() {
        Log.i(LOG_TAG, "Probe accessibility service interrupted")
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Probe accessibility service destroyed")
        super.onDestroy()
    }

    private companion object {
        const val LOG_TAG = "GhostProbeA11y"
    }
}
