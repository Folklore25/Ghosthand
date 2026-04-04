/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.interaction.execution.ActionEffectObservation

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.interaction.effects.ActionEvidencePayloads
import com.folklore25.ghosthand.state.InputKeyDispatchResult
import com.folklore25.ghosthand.state.InputOperationResult
import com.folklore25.ghosthand.state.InputTextMutationResult
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
import org.json.JSONException
import org.json.JSONObject

internal object GhosthandInputPayloads {
    fun parseRequest(body: JSONObject): GhosthandInputRequestParseResult {
        return parseRequest(
            linkedMapOf<String, Any?>().apply {
                body.keys().forEach { key ->
                    put(key, if (body.isNull(key)) null else body.opt(key))
                }
            }
        )
    }

    fun parseRequest(body: String): GhosthandInputRequestParseResult {
        return try {
            parseRequest(JSONObject(body))
        } catch (_: JSONException) {
            GhosthandInputRequestParseResult(errorMessage = "Request body must be valid JSON.")
        }
    }

    fun parseRequest(body: Map<String, Any?>): GhosthandInputRequestParseResult {
        val text = when {
            body.containsKey("text") && body["text"] != null -> body["text"] as? String
                ?: return GhosthandInputRequestParseResult(errorMessage = "text must be a string.")
            else -> null
        }
        val explicitTextAction = when {
            body.containsKey("textAction") && body["textAction"] != null -> {
                val raw = body["textAction"] as? String
                    ?: return GhosthandInputRequestParseResult(errorMessage = "textAction must be a string.")
                InputTextAction.fromWireValue(raw)
                    ?: return GhosthandInputRequestParseResult(errorMessage = "textAction must be one of: set, append, clear.")
            }
            else -> null
        }
        val key = when {
            body.containsKey("key") && body["key"] != null -> {
                val raw = body["key"] as? String
                    ?: return GhosthandInputRequestParseResult(errorMessage = "key must be a string.")
                InputKey.fromWireValue(raw)
                    ?: return GhosthandInputRequestParseResult(errorMessage = "key must be one of: enter.")
            }
            else -> null
        }

        val append = body["append"] as? Boolean ?: false
        val clear = body["clear"] as? Boolean ?: false
        if (explicitTextAction == null && append && clear) {
            return GhosthandInputRequestParseResult(errorMessage = "append and clear cannot both be true.")
        }

        val textAction = explicitTextAction ?: when {
            append -> InputTextAction.APPEND
            text != null -> InputTextAction.SET
            clear -> InputTextAction.CLEAR
            else -> null
        }

        if (textAction == null && key == null) {
            return GhosthandInputRequestParseResult(errorMessage = "At least one explicit /input operation is required.")
        }
        if (textAction == InputTextAction.CLEAR && text != null) {
            return GhosthandInputRequestParseResult(errorMessage = "text must be omitted when textAction=clear.")
        }
        if (textAction != null && textAction != InputTextAction.CLEAR && text == null) {
            return GhosthandInputRequestParseResult(
                errorMessage = "text is required when textAction=${textAction.wireValue}."
            )
        }

        return GhosthandInputRequestParseResult(
            request = GhosthandInputRequest(
                textAction = textAction,
                text = text,
                key = key
            )
        )
    }

    fun inputResultFields(
        result: InputOperationResult,
        actionEffect: ActionEffectObservation? = null,
        attemptedPath: String? = null,
        backendUsed: String? = null
    ): Map<String, Any?> {
        return ActionEvidencePayloads.commonFields(
            performed = result.performed,
            attemptedPath = attemptedPath,
            backendUsed = backendUsed,
            actionEffect = actionEffect,
            postActionState = result.postActionState,
            extras = linkedMapOf(
                "textChanged" to (result.textMutation?.performed ?: false),
                "keyDispatched" to (result.keyDispatch?.performed ?: false),
                "textMutation" to result.textMutation?.let(::textMutationFields),
                "keyDispatch" to result.keyDispatch?.let(::keyDispatchFields)
            )
        )
    }

    private fun textMutationFields(mutation: InputTextMutationResult): Map<String, Any?> {
        return linkedMapOf(
            "requested" to mutation.requested,
            "performed" to mutation.performed,
            "action" to mutation.action,
            "previousText" to mutation.previousText,
            "text" to mutation.finalText,
            "backendUsed" to mutation.backendUsed,
            "failureReason" to mutation.failureReason?.name,
            "attemptedPath" to mutation.attemptedPath
        )
    }

    private fun keyDispatchFields(dispatch: InputKeyDispatchResult): Map<String, Any?> {
        return linkedMapOf(
            "requested" to dispatch.requested,
            "performed" to dispatch.performed,
            "key" to dispatch.key,
            "backendUsed" to dispatch.backendUsed,
            "failureReason" to dispatch.failureReason?.name,
            "attemptedPath" to dispatch.attemptedPath
        )
    }
}
