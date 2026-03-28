package com.folklore25.ghosthand

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
