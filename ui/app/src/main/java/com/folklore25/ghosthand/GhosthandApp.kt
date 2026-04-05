/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.app.Application
import android.util.Log

class GhosthandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTextResolver.initialize(this)
        CapabilityPolicyStore.initialize(this)
        RuntimeStateStore.markAppStarted()
        RuntimeStateStore.refreshHomeDiagnostics(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(LOG_TAG, "Ghosthand application initialized")
    }

    private companion object {
        const val LOG_TAG = "GhosthandApp"
    }
}
