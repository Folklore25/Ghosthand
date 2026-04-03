/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.ClickAttemptResult
import com.folklore25.ghosthand.ClickSelectorResolution
import com.folklore25.ghosthand.GlobalActionResult
import com.folklore25.ghosthand.interaction.effects.ActionEffectPayloads
import com.folklore25.ghosthand.state.summary.PostActionStateComposer

internal object GhosthandInteractionPayloads {
    fun clickFields(result: ClickAttemptResult): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "backendUsed" to result.backendUsed,
            "attemptedPath" to result.attemptedPath
        )
        result.effect?.let { effect -> payload.putAll(actionEffectFields(effect)) }
        PostActionStateComposer.fromObservedEffect(
            actionEffect = result.effect,
            fallbackSnapshot = null
        )?.let(PostActionStateComposer::fields)?.takeIf { it.isNotEmpty() }?.let { payload["postActionState"] = it }
        result.selectorResolution?.let { resolution ->
            payload["resolution"] = clickResolutionFields(resolution)
        }
        return payload
    }

    fun globalActionFields(result: GlobalActionResult): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "attemptedPath" to result.attemptedPath
        ).apply {
            result.effect?.let { effect -> putAll(ActionEffectPayloads.fields(effect)) }
            PostActionStateComposer.fromObservedEffect(
                actionEffect = result.effect,
                fallbackSnapshot = null
            )?.let(PostActionStateComposer::fields)?.takeIf { it.isNotEmpty() }?.let { put("postActionState", it) }
        }
    }

    fun clickResolutionFields(resolution: ClickSelectorResolution): Map<String, Any?> {
        return linkedMapOf(
            "requestedStrategy" to resolution.requestedStrategy,
            "effectiveStrategy" to resolution.effectiveStrategy,
            "requestedSurface" to resolution.requestedSurface,
            "matchedSurface" to resolution.matchedSurface,
            "requestedMatchSemantics" to resolution.requestedMatchSemantics,
            "matchedMatchSemantics" to resolution.matchedMatchSemantics,
            "usedSurfaceFallback" to resolution.usedSurfaceFallback,
            "usedContainsFallback" to resolution.usedContainsFallback,
            "matchedNodeId" to resolution.matchedNodeId,
            "matchedNodeClickable" to resolution.matchedNodeClickable,
            "resolvedNodeId" to resolution.resolvedNodeId,
            "resolutionKind" to resolution.resolutionKind,
            "ancestorDepth" to resolution.ancestorDepth
        )
    }

    fun actionEffectFields(effect: ActionEffectObservation): Map<String, Any?> {
        return ActionEffectPayloads.fields(effect)
    }

    fun postActionStateFields(state: PostActionState): Map<String, Any?> {
        return PostActionStateComposer.fields(state)
    }
}
