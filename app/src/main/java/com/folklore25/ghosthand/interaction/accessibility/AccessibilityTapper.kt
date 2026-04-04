/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.accessibility

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

import android.util.Log

class AccessibilityTapper {
    fun tapPoint(x: Int, y: Int): TapAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return TapAttemptResult.failure(
                reason = TapFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val gesturePerformed = service.performTapGesture(x, y)
        return if (gesturePerformed) {
            Log.i(LOG_TAG, "event=tap_path targetType=point path=gesture_dispatch success=true")
            TapAttemptResult.success(attemptedPath = "gesture_dispatch")
        } else {
            Log.i(LOG_TAG, "event=tap_path targetType=point path=gesture_dispatch success=false")
            TapAttemptResult.failure(
                reason = TapFailureReason.ACTION_FAILED,
                attemptedPath = "gesture_dispatch"
            )
        }
    }

    fun tapNode(nodeId: String): TapAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return TapAttemptResult.failure(
                reason = TapFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val nodeTapResult = service.performNodeClick(nodeId)

        if (!nodeTapResult.nodeFound) {
            Log.i(LOG_TAG, "event=tap_path targetType=node path=node_lookup success=false")
            return when (nodeTapResult.attemptedPath) {
                "root_unavailable" -> TapAttemptResult.failure(
                    reason = TapFailureReason.ACCESSIBILITY_UNAVAILABLE,
                    attemptedPath = nodeTapResult.attemptedPath
                )
                else -> TapAttemptResult.failure(
                    reason = TapFailureReason.NODE_NOT_FOUND,
                    attemptedPath = nodeTapResult.attemptedPath
                )
            }
        }

        if (nodeTapResult.performed) {
            Log.i(LOG_TAG, "event=tap_path targetType=node path=${nodeTapResult.attemptedPath} success=true")
            return TapAttemptResult.success(attemptedPath = nodeTapResult.attemptedPath)
        }

        Log.i(LOG_TAG, "event=tap_path targetType=node path=${nodeTapResult.attemptedPath} success=false")
        return TapAttemptResult.failure(
            reason = TapFailureReason.ACTION_FAILED,
            attemptedPath = nodeTapResult.attemptedPath
        )
    }
}

data class TapAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: TapFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): TapAttemptResult = TapAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: TapFailureReason, attemptedPath: String): TapAttemptResult = TapAttemptResult(
            performed = false,
            backendUsed = null,
            failureReason = reason,
            attemptedPath = attemptedPath
        )
    }
}

enum class TapFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NODE_NOT_FOUND,
    ACTION_FAILED
}

private const val LOG_TAG = "GhostTap"
