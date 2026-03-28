package com.folklore25.ghosthand

import android.util.Log

class AccessibilityClicker {
    fun clickNode(nodeId: String): ClickAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return ClickAttemptResult.failure(
                reason = ClickFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val clickResult = service.performNodeClick(nodeId)

        return when {
            !clickResult.nodeFound -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=false reason=node_not_found")
                ClickAttemptResult.failure(
                    reason = when (clickResult.attemptedPath) {
                        "root_unavailable" -> ClickFailureReason.ACCESSIBILITY_UNAVAILABLE
                        else -> ClickFailureReason.NODE_NOT_FOUND
                    },
                    attemptedPath = clickResult.attemptedPath
                )
            }
            clickResult.performed -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=true")
                ClickAttemptResult.success(attemptedPath = clickResult.attemptedPath)
            }
            else -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=false reason=action_failed")
                ClickAttemptResult.failure(
                    reason = ClickFailureReason.ACTION_FAILED,
                    attemptedPath = clickResult.attemptedPath
                )
            }
        }
    }

    private companion object {
        const val LOG_TAG = "GhostClick"
    }
}

data class ClickAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: ClickFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): ClickAttemptResult = ClickAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: ClickFailureReason, attemptedPath: String): ClickAttemptResult =
            ClickAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class ClickFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NODE_NOT_FOUND,
    ACTION_FAILED
}
