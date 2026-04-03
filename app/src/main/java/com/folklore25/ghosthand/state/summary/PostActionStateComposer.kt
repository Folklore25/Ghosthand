/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.summary

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.PostActionState
import com.folklore25.ghosthand.ScreenReadMode
import com.folklore25.ghosthand.ScreenReadRetryHint

object PostActionStateComposer {
    fun fromObservedEffect(
        actionEffect: ActionEffectObservation?,
        fallbackSnapshot: AccessibilityTreeSnapshot?
    ): PostActionState? {
        val packageName = actionEffect?.finalPackageName ?: fallbackSnapshot?.packageName
        val activity = actionEffect?.finalActivity ?: fallbackSnapshot?.activity
        val snapshotToken = actionEffect?.afterSnapshotToken ?: fallbackSnapshot?.snapshotToken
        val focusedEditablePresent = fallbackSnapshot?.nodes?.any { it.focused && it.editable }
        val renderContext = fallbackSnapshot?.let(::deriveRenderContext)
        if (packageName == null &&
            activity == null &&
            snapshotToken == null &&
            focusedEditablePresent == null &&
            renderContext == null
        ) {
            return null
        }

        return PostActionState(
            packageName = packageName,
            activity = activity,
            snapshotToken = snapshotToken,
            focusedEditablePresent = focusedEditablePresent,
            renderMode = renderContext?.renderMode,
            surfaceReadability = renderContext?.surfaceReadability,
            visualAvailable = renderContext?.visualAvailable
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
        }
    }

    private fun deriveRenderContext(snapshot: AccessibilityTreeSnapshot): DerivedRenderContext {
        val candidateNodeCount = snapshot.nodes.size
        val returnedElementCount = snapshot.nodes.count { it.hasActionableBounds() && !it.isLowSignalNode() }
        val omittedNodeCount = candidateNodeCount - returnedElementCount
        val partialOutput = omittedNodeCount > 0
        val retryHint = when {
            returnedElementCount == 0 -> ScreenReadRetryHint(
                source = ScreenReadMode.OCR.wireValue,
                reason = "accessibility_empty"
            )
            candidateNodeCount >= 20 &&
                omittedNodeCount >= 20 &&
                candidateNodeCount > 0 &&
                omittedNodeCount.toDouble() / candidateNodeCount.toDouble() >= 0.40 ->
                ScreenReadRetryHint(
                    source = ScreenReadMode.HYBRID.wireValue,
                    reason = "accessibility_operationally_insufficient"
                )
            else -> null
        }
        val renderMode = when {
            retryHint != null -> "limited_accessibility"
            else -> "accessibility"
        }
        val surfaceReadability = when {
            retryHint?.source == ScreenReadMode.OCR.wireValue -> "poor"
            retryHint != null || partialOutput -> "limited"
            else -> "good"
        }
        return DerivedRenderContext(
            renderMode = renderMode,
            surfaceReadability = surfaceReadability,
            visualAvailable = null
        )
    }
}

private data class DerivedRenderContext(
    val renderMode: String,
    val surfaceReadability: String,
    val visualAvailable: Boolean?
)

private fun com.folklore25.ghosthand.FlatAccessibilityNode.hasActionableBounds(): Boolean {
    if (bounds.right <= bounds.left || bounds.bottom <= bounds.top) return false
    if (bounds.left < 0 || bounds.top < 0 || bounds.right < 0 || bounds.bottom < 0) return false
    return centerX in bounds.left..bounds.right && centerY in bounds.top..bounds.bottom
}

private fun com.folklore25.ghosthand.FlatAccessibilityNode.isLowSignalNode(): Boolean {
    val hasMeaningfulLabel = !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !resourceId.isNullOrBlank()
    if (hasMeaningfulLabel) return false
    if (clickable || editable || scrollable || focused) return false
    return true
}
