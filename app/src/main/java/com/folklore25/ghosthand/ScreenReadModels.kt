/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

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
    val visualAvailable: Boolean? = null,
    val previewAvailable: Boolean? = null,
    val previewToken: String? = null,
    val previewWidth: Int? = null,
    val previewHeight: Int? = null,
    val previewImage: String? = null,
    val retryHint: ScreenReadRetryHint? = null
) {
    fun accessibilityTreeIsOperationallyInsufficient(): Boolean {
        if (returnedElementCount == 0) {
            return true
        }
        if (!partialOutput) {
            return false
        }
        if (returnedElementCount <= 1) {
            return true
        }
        val omittedRatio = if (candidateNodeCount <= 0) {
            0.0
        } else {
            omittedNodeCount.toDouble() / candidateNodeCount.toDouble()
        }
        return candidateNodeCount >= 20 && omittedNodeCount >= 20 && omittedRatio >= 0.40
    }

    fun renderMode(): String {
        return when {
            source == ScreenReadMode.HYBRID.wireValue -> "hybrid"
            source == ScreenReadMode.OCR.wireValue -> "ocr"
            retryHint != null -> "limited_accessibility"
            else -> "accessibility"
        }
    }

    fun surfaceReadability(): String {
        return when {
            retryHint?.source == ScreenReadMode.OCR.wireValue -> "poor"
            retryHint != null || partialOutput -> "limited"
            else -> "good"
        }
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
