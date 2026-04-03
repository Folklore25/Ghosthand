/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

enum class ScreenReadMode(val wireValue: String) {
    ACCESSIBILITY("accessibility"),
    OCR("ocr"),
    HYBRID("hybrid");

    companion object {
        fun fromWireValue(raw: String?): ScreenReadMode? {
            return entries.firstOrNull { it.wireValue == raw?.trim()?.lowercase() }
        }
    }
}

enum class GhosthandRenderMode(val wireValue: String) {
    ACCESSIBILITY("accessibility"),
    LIMITED_ACCESSIBILITY("limited_accessibility"),
    OCR("ocr"),
    HYBRID("hybrid")
}

enum class GhosthandSurfaceReadability(val wireValue: String) {
    GOOD("good"),
    LIMITED("limited"),
    POOR("poor")
}

data class ScreenReadElement(
    val nodeId: String? = null,
    val text: String = "",
    val desc: String = "",
    val id: String = "",
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val focused: Boolean = false,
    val scrollable: Boolean = false,
    val bounds: String,
    val centerX: Int,
    val centerY: Int,
    val source: String
)

data class ScreenReadPayload(
    val packageName: String?,
    val activity: String?,
    val snapshotToken: String?,
    val capturedAt: String?,
    val foregroundStableDuringCapture: Boolean,
    val partialOutput: Boolean,
    val candidateNodeCount: Int,
    val returnedElementCount: Int,
    val warnings: List<String>,
    val omittedInvalidBoundsCount: Int,
    val omittedLowSignalCount: Int,
    val omittedNodeCount: Int,
    val omittedCategories: List<String>,
    val omittedSummary: String?,
    val invalidBoundsPresent: Boolean,
    val lowSignalPresent: Boolean,
    val elements: List<ScreenReadElement>,
    val source: String,
    val accessibilityElementCount: Int,
    val ocrElementCount: Int,
    val usedOcrFallback: Boolean,
    val focusedEditablePresent: Boolean? = null,
    val visualAvailable: Boolean? = null,
    val previewAvailable: Boolean? = null,
    val previewToken: String? = null,
    val previewWidth: Int? = null,
    val previewHeight: Int? = null,
    val previewImage: String? = null,
    val retryHint: ScreenReadRetryHint? = null
) {
    fun accessibilityTreeIsOperationallyInsufficient(): Boolean {
        return deriveAccessibilityRetryHint(
            candidateNodeCount = candidateNodeCount,
            returnedElementCount = returnedElementCount,
            omittedNodeCount = omittedNodeCount
        )?.reason == "accessibility_operationally_insufficient"
    }

    fun renderModeKind(): GhosthandRenderMode {
        return ScreenStateLegibilityProjector.fromPayload(this).renderMode
    }

    fun renderMode(): String {
        return renderModeKind().wireValue
    }

    fun surfaceReadabilityKind(): GhosthandSurfaceReadability {
        return ScreenStateLegibilityProjector.fromPayload(this).surfaceReadability
    }

    fun surfaceReadability(): String {
        return surfaceReadabilityKind().wireValue
    }
}

data class ScreenReadRetryHint(
    val source: String,
    val reason: String
)

data class ScreenOcrResult(
    val elements: List<ScreenReadElement>,
    val attemptedPath: String,
    val warnings: List<String> = emptyList()
)
