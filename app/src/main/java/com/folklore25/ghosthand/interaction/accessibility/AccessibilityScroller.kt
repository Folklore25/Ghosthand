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
