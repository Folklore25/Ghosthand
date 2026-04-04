/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.interaction.execution.ActionEffectObservation
import com.folklore25.ghosthand.interaction.accessibility.ClickAttemptResult
import com.folklore25.ghosthand.screen.find.ClickSelectorResolution
import com.folklore25.ghosthand.interaction.execution.GlobalActionResult

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.interaction.effects.ActionEvidencePayloads
import com.folklore25.ghosthand.interaction.effects.ActionEffectPayloads
import com.folklore25.ghosthand.state.summary.PostActionStateComposer

internal object GhosthandInteractionPayloads {
    fun clickFields(
        result: ClickAttemptResult,
        fallbackSnapshot: AccessibilityTreeSnapshot? = null
    ): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>().apply {
            putAll(
                ActionEvidencePayloads.commonFields(
                    performed = result.performed,
                    backendUsed = result.backendUsed,
                    attemptedPath = result.attemptedPath,
                    actionEffect = result.effect,
                    postActionState = PostActionStateComposer.fromObservedEffect(
                        actionEffect = result.effect,
                        fallbackSnapshot = fallbackSnapshot
                    )
                )
            )
        }
        result.selectorResolution?.let { resolution ->
            payload["resolution"] = clickResolutionFields(resolution)
        }
        return payload
    }

    fun globalActionFields(
        result: GlobalActionResult,
        fallbackSnapshot: AccessibilityTreeSnapshot? = null
    ): Map<String, Any?> {
        return ActionEvidencePayloads.commonFields(
            performed = result.performed,
            attemptedPath = result.attemptedPath,
            actionEffect = result.effect,
            postActionState = PostActionStateComposer.fromObservedEffect(
                actionEffect = result.effect,
                fallbackSnapshot = fallbackSnapshot
            )
        )
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
