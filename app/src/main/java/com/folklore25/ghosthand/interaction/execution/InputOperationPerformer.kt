/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
