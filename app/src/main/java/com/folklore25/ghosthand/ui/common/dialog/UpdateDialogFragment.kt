/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.common.dialog

import com.folklore25.ghosthand.ui.main.RuntimeStateViewModel
import com.folklore25.ghosthand.ui.common.model.UiStatusSupport
import com.folklore25.ghosthand.ui.diagnostics.UpdateStatus
import com.folklore25.ghosthand.ui.main.UpdateSummaryUiState

import com.folklore25.ghosthand.R

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton

internal class UpdateDialogFragment : DialogFragment() {
    private var updateSummary: UpdateSummaryUiState? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update, null, false)
        val installedValue = view.findViewById<TextView>(R.id.updateDialogInstalledValue)
        val latestValue = view.findViewById<TextView>(R.id.updateDialogLatestValue)
        val statusValue = view.findViewById<TextView>(R.id.updateDialogStatusValue)
        val actionButton = view.findViewById<MaterialButton>(R.id.updateDialogActionButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val runtimeViewModel = ViewModelProvider(requireActivity())[RuntimeStateViewModel::class.java]
        runtimeViewModel.homeScreenState.observe(this) { state ->
            updateSummary = state.updateSummary
            installedValue.text = state.updateSummary.installedVersionText
            latestValue.text = state.updateSummary.latestReleaseText
            statusValue.text = state.updateSummary.statusText
            UiStatusSupport.styleChip(requireContext(), statusValue, state.updateSummary.statusTone)

            when (state.updateSummary.status) {
                UpdateStatus.UPDATE_AVAILABLE -> {
                    actionButton.text = getString(R.string.home_update_action_download)
                    actionButton.isEnabled = !state.updateSummary.actionUrl.isNullOrBlank()
                    actionButton.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gh_brand_primary)
                    )
                    actionButton.strokeWidth = 0
                    actionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                UpdateStatus.CHECKING -> {
                    actionButton.text = getString(R.string.home_update_action_checking)
                    actionButton.isEnabled = false
                    actionButton.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gh_surface_card_alt)
                    )
                    actionButton.strokeWidth = resources.getDimensionPixelSize(R.dimen.update_dialog_action_stroke)
                    actionButton.strokeColor = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gh_surface_stroke)
                    )
                    actionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gh_text_secondary))
                }
                UpdateStatus.UP_TO_DATE,
                UpdateStatus.CHECK_FAILED -> {
                    actionButton.text = getString(R.string.update_dialog_action_retry)
                    actionButton.isEnabled = true
                    actionButton.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gh_surface_card_alt)
                    )
                    actionButton.strokeWidth = resources.getDimensionPixelSize(R.dimen.update_dialog_action_stroke)
                    actionButton.strokeColor = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gh_surface_stroke)
                    )
                    actionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gh_brand_primary))
                }
            }
        }

        actionButton.setOnClickListener {
            val summary = updateSummary ?: return@setOnClickListener
            when (summary.status) {
                UpdateStatus.UPDATE_AVAILABLE -> {
                    val url = summary.actionUrl ?: return@setOnClickListener
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    dismiss()
                }
                UpdateStatus.UP_TO_DATE,
                UpdateStatus.CHECK_FAILED -> {
                    runtimeViewModel.refreshReleaseInfo()
                }
                UpdateStatus.CHECKING -> Unit
            }
        }

        return dialog
    }

    companion object {
        private const val TAG = "update_dialog"

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            UpdateDialogFragment().show(fragmentManager, TAG)
        }
    }
}
