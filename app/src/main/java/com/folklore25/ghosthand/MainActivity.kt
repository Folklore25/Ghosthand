/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        val views = HomeScreenViews.bind(this)
        val binder = HomeScreenBinder(
            context = this,
            versionBadge = views.versionBadge,
            updateInstalledValue = views.updateInstalledValue,
            updateLatestValue = views.updateLatestValue,
            updateStatusValue = views.updateStatusValue,
            updateButton = views.updateButton,
            runtimeStatusValue = views.runtimeStatusValue,
            runtimeApiChip = views.runtimeApiChip,
            runtimeServiceChip = views.runtimeServiceChip,
            runtimeAccessibilityChip = views.runtimeAccessibilityChip,
            startRuntimeButton = views.startRuntimeButton,
            permissionSummaryValue = views.permissionSummaryValue,
            accessibilityRow = views.accessibilityRow,
            screenshotRow = views.screenshotRow,
            diagnosticsBuildValue = views.diagnosticsBuildValue,
            diagnosticsLastActionValue = views.diagnosticsLastActionValue,
            diagnosticsForegroundValue = views.diagnosticsForegroundValue
        )
        HomeScreenActions(this, runtimeViewModel, views).bind()

        runtimeViewModel.homeScreenState.observe(this) { state ->
            binder.bind(state)
        }
    }
}
