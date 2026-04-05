/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.common.dialog

import com.folklore25.ghosthand.ui.common.model.ModuleExplanation

import com.folklore25.ghosthand.R

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

internal class ModuleExplanationDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val moduleName = requireArguments().getString(ARG_MODULE)
        val module = requireNotNull(moduleName?.let(ModuleExplanation::valueOf)) {
            "Module explanation is required"
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(module.titleRes)
            .setMessage(module.bodyRes)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        private const val ARG_MODULE = "module"
        private const val TAG = "module_explanation"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            module: ModuleExplanation
        ) {
            ModuleExplanationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODULE, module.name)
                }
            }.show(fragmentManager, TAG)
        }
    }
}
