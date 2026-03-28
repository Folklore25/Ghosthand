package com.folklore25.ghosthand

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
