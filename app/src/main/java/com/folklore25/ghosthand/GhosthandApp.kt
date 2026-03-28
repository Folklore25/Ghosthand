package com.folklore25.ghosthand

import android.app.Application
import android.util.Log

class GhosthandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTextResolver.initialize(this)
        RuntimeStateStore.markAppStarted()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(LOG_TAG, "Ghosthand application initialized")
    }

    private companion object {
        const val LOG_TAG = "GhosthandApp"
    }
}
