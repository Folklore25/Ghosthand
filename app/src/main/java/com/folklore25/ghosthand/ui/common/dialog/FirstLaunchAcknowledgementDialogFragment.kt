/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.common.dialog

import com.folklore25.ghosthand.state.diagnostics.FirstLaunchAcknowledgementStore
import com.folklore25.ghosthand.ui.common.model.FirstLaunchAcknowledgementViewModel

import com.folklore25.ghosthand.R

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton

internal class FirstLaunchAcknowledgementDialogFragment : DialogFragment() {
    private val countdownHandler = Handler(Looper.getMainLooper())
    private lateinit var exitButton: MaterialButton
    private lateinit var agreeButton: MaterialButton
    private lateinit var viewModel: FirstLaunchAcknowledgementViewModel
    private lateinit var acknowledgementStore: FirstLaunchAcknowledgementStore

    private val countdownTicker = object : Runnable {
        override fun run() {
            renderAgreeButton()
            if (!viewModel.isCountdownComplete()) {
                countdownHandler.postDelayed(this, COUNTDOWN_TICK_MS)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_first_launch_acknowledgement, null, false)

        exitButton = view.findViewById(R.id.firstLaunchExitButton)
        agreeButton = view.findViewById(R.id.firstLaunchAgreeButton)

        acknowledgementStore = FirstLaunchAcknowledgementStore.getInstance(requireContext())
        viewModel = ViewModelProvider(requireActivity())[FirstLaunchAcknowledgementViewModel::class.java]
        viewModel.startCountdownIfNeeded(COUNTDOWN_DURATION_MS)

        exitButton.setOnClickListener {
            requireActivity().finishAffinity()
            requireActivity().finishAndRemoveTask()
        }

        agreeButton.setOnClickListener {
            if (!viewModel.isCountdownComplete()) {
                return@setOnClickListener
            }
            acknowledgementStore.markAcknowledged()
            dismissAllowingStateLoss()
        }

        renderAgreeButton()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        isCancelable = false
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        countdownHandler.post(countdownTicker)
    }

    override fun onStop() {
        countdownHandler.removeCallbacks(countdownTicker)
        super.onStop()
    }

    private fun renderAgreeButton() {
        val remainingSeconds = viewModel.remainingSeconds()
        val enabled = remainingSeconds == 0
        agreeButton.isEnabled = enabled
        agreeButton.text = if (enabled) {
            getString(R.string.first_launch_agree)
        } else {
            getString(R.string.first_launch_agree_countdown, remainingSeconds)
        }
        if (enabled) {
            agreeButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gh_brand_primary)
            )
            agreeButton.strokeWidth = 0
            agreeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            agreeButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gh_surface_card_alt)
            )
            agreeButton.strokeWidth = resources.getDimensionPixelSize(R.dimen.update_dialog_action_stroke)
            agreeButton.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gh_surface_stroke)
            )
            agreeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gh_text_secondary))
        }
    }

    companion object {
        private const val TAG = "first_launch_acknowledgement"
        private const val COUNTDOWN_DURATION_MS = 5_000L
        private const val COUNTDOWN_TICK_MS = 250L

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) {
                return
            }
            FirstLaunchAcknowledgementDialogFragment().show(fragmentManager, TAG)
        }
    }
}
