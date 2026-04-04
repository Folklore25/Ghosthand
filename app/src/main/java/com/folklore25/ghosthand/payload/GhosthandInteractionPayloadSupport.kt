/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

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
