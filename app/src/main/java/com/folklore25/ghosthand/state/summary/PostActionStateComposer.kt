/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.summary

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

import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.screen.read.deriveAccessibilityRetryHint
import com.folklore25.ghosthand.screen.read.hasActionableBounds
import com.folklore25.ghosthand.screen.read.isLowSignalNode
import com.folklore25.ghosthand.screen.read.ScreenStateLegibilityProjector

object PostActionStateComposer {
    fun fromObservedEffect(
        actionEffect: ActionEffectObservation?,
        fallbackSnapshot: AccessibilityTreeSnapshot?
    ): PostActionState? {
        val packageName = actionEffect?.finalPackageName ?: fallbackSnapshot?.packageName
        val activity = actionEffect?.finalActivity ?: fallbackSnapshot?.activity
        val snapshotToken = actionEffect?.afterSnapshotToken ?: fallbackSnapshot?.snapshotToken
        val legibility = fallbackSnapshot?.let(ScreenStateLegibilityProjector::fromAccessibilitySnapshot)
        val retryHint = fallbackSnapshot?.let { snapshot ->
            val candidateNodeCount = snapshot.nodes.size
            val returnedElementCount = snapshot.nodes.count { it.hasActionableBounds() && !it.isLowSignalNode() }
            val omittedNodeCount = candidateNodeCount - returnedElementCount
            deriveAccessibilityRetryHint(
                candidateNodeCount = candidateNodeCount,
                returnedElementCount = returnedElementCount,
                omittedNodeCount = omittedNodeCount
            )
        }
        if (packageName == null &&
            activity == null &&
            snapshotToken == null &&
            legibility == null
        ) {
            return null
        }

        return PostActionState(
            packageName = packageName,
            activity = activity,
            snapshotToken = snapshotToken,
            focusedEditablePresent = legibility?.focusedEditablePresent,
            renderMode = legibility?.renderMode?.wireValue,
            surfaceReadability = legibility?.surfaceReadability?.wireValue,
            visualAvailable = legibility?.visualAvailable,
            suggestedSource = retryHint?.source,
            fallbackReason = retryHint?.reason
        )
    }

    fun fields(state: PostActionState): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            state.packageName?.let { put("packageName", it) }
            state.activity?.let { put("activity", it) }
            state.snapshotToken?.let { put("snapshotToken", it) }
            state.focusedEditablePresent?.let { put("focusedEditablePresent", it) }
            state.renderMode?.let { put("renderMode", it) }
            state.surfaceReadability?.let { put("surfaceReadability", it) }
            state.visualAvailable?.let { put("visualAvailable", it) }
            state.suggestedSource?.let { put("suggestedSource", it) }
            state.fallbackReason?.let { put("fallbackReason", it) }
        }
    }
}
