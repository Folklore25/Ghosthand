package com.folklore25.ghosthand

import android.util.Log

class AccessibilityScroller {
    fun scrollNode(
        snapshot: AccessibilityTreeSnapshot,
        nodeId: String,
        direction: String
    ): ScrollAttemptResult {
        val node = snapshot.nodes.firstOrNull { it.nodeId == nodeId }
            ?: return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.NODE_NOT_FOUND,
                attemptedPath = "node_lookup"
            )

        val bounds = node.bounds
        val cx = (bounds.left + bounds.right) / 2
        val cy = (bounds.top + bounds.bottom) / 2
        val w = bounds.right - bounds.left
        val h = bounds.bottom - bounds.top

        if (w <= 0 || h <= 0) {
            return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.NODE_TOO_SMALL,
                attemptedPath = "invalid_bounds"
            )
        }

        val scrollFraction = SCROLL_FRACTION.coerceIn(0f, 0.45f)
        val dx = (w * scrollFraction).toInt()
        val dy = (h * scrollFraction).toInt()

        val (fromX, fromY, toX, toY) = when (direction.lowercase()) {
            "up" -> ScrollVector(cx, cy + dy, cx, cy - dy)
            "down" -> ScrollVector(cx, cy - dy, cx, cy + dy)
            "left" -> ScrollVector(cx + dx, cy, cx - dx, cy)
            "right" -> ScrollVector(cx - dx, cy, cx + dx, cy)
            else -> return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.INVALID_DIRECTION,
                attemptedPath = "direction_validation"
            )
        }

        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val performed = service.performSwipeGesture(
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            durationMs = SCROLL_DURATION_MS
        )

        Log.i(
            LOG_TAG,
            "event=scroll_node nodeId=$nodeId direction=$direction from=($fromX,$fromY) to=($toX,$toY) success=$performed"
        )

        return if (performed) {
            ScrollAttemptResult.success(attemptedPath = "swipe_gesture")
        } else {
            ScrollAttemptResult.failure(
                reason = ScrollFailureReason.GESTURE_FAILED,
                attemptedPath = "gesture_dispatch"
            )
        }
    }

    private data class ScrollVector(
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    private companion object {
        const val LOG_TAG = "GhostScroll"
        const val SCROLL_FRACTION = 0.35f
        const val SCROLL_DURATION_MS = 300L
    }
}

data class ScrollAttemptResult(
    val performed: Boolean,
    val failureReason: ScrollFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): ScrollAttemptResult = ScrollAttemptResult(
            performed = true,
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: ScrollFailureReason, attemptedPath: String): ScrollAttemptResult =
            ScrollAttemptResult(
                performed = false,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class ScrollFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NODE_NOT_FOUND,
    INVALID_DIRECTION,
    NODE_TOO_SMALL,
    GESTURE_FAILED
}
