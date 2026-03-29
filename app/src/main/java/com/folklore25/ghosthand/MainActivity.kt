/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    private val acknowledgementStore by lazy { FirstLaunchAcknowledgementStore.getInstance(this) }

    override fun onResume() {
        super.onResume()
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val homeContent = findViewById<View>(R.id.homeContentScroll)
        homeContent.visibility = View.INVISIBLE

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        val views = HomeScreenViews.bind(this)
        val binder = HomeScreenBinder(
            context = this,
            versionBadge = views.versionBadge,
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

        acknowledgementStore.loadAcknowledged { acknowledged ->
            if (isFinishing || isDestroyed) {
                return@loadAcknowledged
            }
            homeContent.visibility = View.VISIBLE
            if (!acknowledged && !supportFragmentManager.isStateSaved) {
                FirstLaunchAcknowledgementDialogFragment.show(supportFragmentManager)
            }
        }
    }
}
