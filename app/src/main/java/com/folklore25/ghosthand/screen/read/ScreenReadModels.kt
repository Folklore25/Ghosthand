/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

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
    val previewPath: String? = null,
    val previewWidth: Int? = null,
    val previewHeight: Int? = null,
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
