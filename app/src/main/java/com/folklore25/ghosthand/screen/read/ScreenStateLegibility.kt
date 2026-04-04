/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.R


data class ScreenStateLegibility(
    val focusedEditablePresent: Boolean?,
    val renderMode: GhosthandRenderMode,
    val surfaceReadability: GhosthandSurfaceReadability,
    val visualAvailable: Boolean?,
    val previewAvailable: Boolean?
)

object ScreenStateLegibilityProjector {
    fun fromPayload(payload: ScreenReadPayload): ScreenStateLegibility {
        return ScreenStateLegibility(
            focusedEditablePresent = payload.focusedEditablePresent
                ?: payload.elements.any { it.editable && it.focused },
            renderMode = when {
                payload.source == ScreenReadMode.HYBRID.wireValue -> GhosthandRenderMode.HYBRID
                payload.source == ScreenReadMode.OCR.wireValue -> GhosthandRenderMode.OCR
                payload.retryHint != null -> GhosthandRenderMode.LIMITED_ACCESSIBILITY
                else -> GhosthandRenderMode.ACCESSIBILITY
            },
            surfaceReadability = when {
                payload.retryHint?.source == ScreenReadMode.OCR.wireValue -> GhosthandSurfaceReadability.POOR
                payload.retryHint != null || payload.partialOutput -> GhosthandSurfaceReadability.LIMITED
                else -> GhosthandSurfaceReadability.GOOD
            },
            visualAvailable = payload.visualAvailable,
            previewAvailable = payload.previewAvailable
        )
    }

    fun fromAccessibilitySnapshot(snapshot: AccessibilityTreeSnapshot): ScreenStateLegibility {
        val candidateNodeCount = snapshot.nodes.size
        val returnedElementCount = snapshot.nodes.count { it.hasActionableBounds() && !it.isLowSignalNode() }
        val omittedNodeCount = candidateNodeCount - returnedElementCount
        val retryHint = deriveAccessibilityRetryHint(
            candidateNodeCount = candidateNodeCount,
            returnedElementCount = returnedElementCount,
            omittedNodeCount = omittedNodeCount
        )

        return ScreenStateLegibility(
            focusedEditablePresent = snapshot.nodes.any { it.focused && it.editable },
            renderMode = if (retryHint != null) {
                GhosthandRenderMode.LIMITED_ACCESSIBILITY
            } else {
                GhosthandRenderMode.ACCESSIBILITY
            },
            surfaceReadability = when {
                retryHint?.source == ScreenReadMode.OCR.wireValue -> GhosthandSurfaceReadability.POOR
                retryHint != null || omittedNodeCount > 0 -> GhosthandSurfaceReadability.LIMITED
                else -> GhosthandSurfaceReadability.GOOD
            },
            visualAvailable = null,
            previewAvailable = null
        )
    }
}

internal fun deriveAccessibilityRetryHint(
    candidateNodeCount: Int,
    returnedElementCount: Int,
    omittedNodeCount: Int
): ScreenReadRetryHint? {
    val partialOutput = omittedNodeCount > 0
    return when {
        returnedElementCount == 0 -> ScreenReadRetryHint(
            source = ScreenReadMode.OCR.wireValue,
            reason = "accessibility_empty"
        )
        partialOutput && returnedElementCount <= 1 -> ScreenReadRetryHint(
            source = ScreenReadMode.HYBRID.wireValue,
            reason = "accessibility_operationally_insufficient"
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
}

internal fun NodeBounds.isValidGeometry(): Boolean {
    return right > left && bottom > top
}

internal fun FlatAccessibilityNode.hasActionableBounds(): Boolean {
    if (!bounds.isValidGeometry()) return false
    if (bounds.left < 0 || bounds.top < 0 || bounds.right < 0 || bounds.bottom < 0) return false
    return centerX in bounds.left..bounds.right && centerY in bounds.top..bounds.bottom
}

internal fun FlatAccessibilityNode.isLowSignalNode(): Boolean {
    val hasMeaningfulLabel = !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !resourceId.isNullOrBlank()
    if (hasMeaningfulLabel) return false
    if (clickable || editable || scrollable || focused) return false
    return true
}
