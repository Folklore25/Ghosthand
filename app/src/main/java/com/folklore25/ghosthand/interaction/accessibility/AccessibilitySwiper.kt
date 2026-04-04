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

class AccessibilitySwiper {
    fun swipe(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): SwipeAttemptResult {
        val runtimeState = RuntimeStateStore.snapshot()
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return SwipeAttemptResult.failure(
                reason = SwipeFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )
        val serviceName = service::class.java.simpleName
        val dispatchInstanceId = System.identityHashCode(service)
        val connectedForDispatch = service.dispatchConnectionActive()
        val dispatchConnectionId = service.currentConnectionIdForDispatch()
        val frameworkConnectionPresent = service.frameworkConnectionAvailable()

        val beforeScrollY = runtimeState.swipeProbeScrollY
        val beforeTopItem = runtimeState.swipeProbeTopVisibleItem
        if (!frameworkConnectionPresent) {
            Log.i(
                LOG_TAG,
                "event=swipe_dispatch service=$serviceName from=$fromX,$fromY to=$toX,$toY durationMs=$durationMs dispatchInstanceId=$dispatchInstanceId dispatchConnectionId=$dispatchConnectionId frameworkConnectionPresent=$frameworkConnectionPresent connectedForDispatch=$connectedForDispatch dispatched=false enabled=${runtimeState.accessibilityEnabled} connected=${runtimeState.accessibilityServiceConnected} status=${runtimeState.accessibilityStatus} probeBeforeScrollY=$beforeScrollY probeBeforeTopItem=$beforeTopItem probeAfterScrollY=$beforeScrollY probeAfterTopItem=$beforeTopItem"
            )
            Log.i(LOG_TAG, "event=swipe_path path=framework_connection_missing success=false")
            return SwipeAttemptResult.failure(
                reason = SwipeFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "framework_connection_missing"
            )
        }

        val dispatchDiagnostic = service.performSwipeGestureDiagnostic(
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            durationMs = durationMs
        )
        val afterState = RuntimeStateStore.snapshot()

        Log.i(
            LOG_TAG,
            "event=swipe_dispatch service=$serviceName from=$fromX,$fromY to=$toX,$toY durationMs=$durationMs dispatchInstanceId=$dispatchInstanceId dispatchConnectionId=$dispatchConnectionId frameworkConnectionPresent=$frameworkConnectionPresent connectedForDispatch=$connectedForDispatch dispatched=${dispatchDiagnostic.dispatched} callback=${dispatchDiagnostic.callbackResult} completed=${dispatchDiagnostic.completed} enabled=${runtimeState.accessibilityEnabled} connected=${runtimeState.accessibilityServiceConnected} status=${runtimeState.accessibilityStatus} probeBeforeScrollY=$beforeScrollY probeBeforeTopItem=$beforeTopItem probeAfterScrollY=${afterState.swipeProbeScrollY} probeAfterTopItem=${afterState.swipeProbeTopVisibleItem}"
        )

        return if (dispatchDiagnostic.dispatched) {
            Log.i(LOG_TAG, "event=swipe_path path=gesture_dispatch success=true")
            SwipeAttemptResult.success(attemptedPath = "gesture_dispatch")
        } else {
            Log.i(LOG_TAG, "event=swipe_path path=gesture_dispatch success=false")
            SwipeAttemptResult.failure(
                reason = SwipeFailureReason.ACTION_FAILED,
                attemptedPath = "gesture_dispatch"
            )
        }
    }

    private companion object {
        const val LOG_TAG = "GhostSwipe"
    }
}

data class SwipeAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: SwipeFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): SwipeAttemptResult = SwipeAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: SwipeFailureReason, attemptedPath: String): SwipeAttemptResult =
            SwipeAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class SwipeFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    ACTION_FAILED
}
