/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.payload.GhosthandInputRequest
import com.folklore25.ghosthand.payload.InputTextAction
import com.folklore25.ghosthand.state.InputKeyDispatchResult
import com.folklore25.ghosthand.state.InputOperationResult
import com.folklore25.ghosthand.state.InputTextMutationResult

internal object InputOperationPerformer {
    internal fun perform(
        request: GhosthandInputRequest,
        focusedTextProvider: () -> String,
        interactionPlane: GhosthandInteractionPlane
    ): InputOperationResult {
        val textMutation = request.textAction?.let { action ->
            val previousText = focusedTextProvider()
            val finalText = when (action) {
                InputTextAction.SET -> request.text ?: ""
                InputTextAction.APPEND -> previousText + (request.text ?: "")
                InputTextAction.CLEAR -> ""
            }
            val result = interactionPlane.typeText(finalText)
            InputTextMutationResult(
                requested = true,
                performed = result.performed,
                action = action.wireValue,
                previousText = previousText,
                finalText = finalText,
                backendUsed = result.backendUsed,
                failureReason = result.failureReason,
                attemptedPath = result.attemptedPath
            )
        }

        val keyDispatch = request.key?.let { key ->
            val result = interactionPlane.dispatchKey(key)
            InputKeyDispatchResult(
                requested = true,
                performed = result.performed,
                key = key.wireValue,
                backendUsed = result.backendUsed,
                failureReason = result.failureReason,
                attemptedPath = result.attemptedPath
            )
        }

        return InputOperationResult(
            performed = listOfNotNull(
                textMutation?.performed,
                keyDispatch?.performed
            ).let { requested -> requested.isNotEmpty() && requested.all { it } },
            textMutation = textMutation,
            keyDispatch = keyDispatch
        )
    }
}
