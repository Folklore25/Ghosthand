/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.main

import com.folklore25.ghosthand.ui.common.model.AppTextResolver
import com.folklore25.ghosthand.ui.common.dialog.FirstLaunchAcknowledgementDialogFragment
import com.folklore25.ghosthand.state.diagnostics.FirstLaunchAcknowledgementStore
import com.folklore25.ghosthand.state.runtime.RuntimeStateStore

import com.folklore25.ghosthand.R

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    private val acknowledgementStore by lazy { FirstLaunchAcknowledgementStore.getInstance(this) }

    override fun onResume() {
        super.onResume()
        AppTextResolver.initialize(this)
        RuntimeStateStore.refreshLocalizedUiText(this)
        RuntimeStateStore.refreshRuntimeSnapshot(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTextResolver.initialize(this)
        setContentView(R.layout.activity_main)
        val homeContent = findViewById<View>(R.id.homeContentScroll)
        homeContent.visibility = View.INVISIBLE

        val runtimeViewModel = ViewModelProvider(this)[RuntimeStateViewModel::class.java]
        val views = HomeScreenViews.bind(this)
        val binder = HomeScreenBinder(
            context = this,
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
            diagnosticsLastActionValue = views.diagnosticsLastActionValue
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
