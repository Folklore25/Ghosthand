/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import android.os.SystemClock
import com.folklore25.ghosthand.AccessibilityNodeFinder
import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.AccessibilityTreeSnapshotResult
import com.folklore25.ghosthand.ClickAttemptResult
import com.folklore25.ghosthand.ClickFailureReason
import com.folklore25.ghosthand.FindNodeResult
import com.folklore25.ghosthand.GestureStroke
import com.folklore25.ghosthand.ScrollAttemptResult
import com.folklore25.ghosthand.ScrollFailureReason
import com.folklore25.ghosthand.payload.GhosthandInputRequest
import com.folklore25.ghosthand.screen.find.FocusedNodeResult
import com.folklore25.ghosthand.state.InputOperationResult
import com.folklore25.ghosthand.state.ScrollBatchResult

internal class InteractionExecutionCoordinator(
    private val treeSnapshotProvider: () -> AccessibilityTreeSnapshotResult,
    private val nodeFinder: AccessibilityNodeFinder,
    private val interactionPlane: GhosthandInteractionPlane,
    private val focusedNodeResultProvider: () -> FocusedNodeResult,
    private val inputOperationPerformer: InputOperationPerformer = InputOperationPerformer
) {
    fun clickFirstMatch(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): ClickAttemptResult {
        val found = nodeFinder.findNodesForClick(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )

        val nodeId = found.node?.nodeId
            ?: return ClickAttemptResult.failure(
                reason = ClickFailureReason.NODE_NOT_FOUND,
                attemptedPath = "selector_lookup",
                selectorResolution = found.clickResolution,
                selectorMissHint = found.missHint
            )

        val clickResult = interactionPlane.clickNode(nodeId)
        return clickResult.copy(selectorResolution = found.clickResolution)
    }

    fun clickFirstMatchFresh(
        strategy: String,
        query: String,
        clickableOnly: Boolean = false,
        index: Int = 0,
        attempts: Int = 4,
        retryDelayMs: Long = 250L
    ): ClickAttemptResult {
        var lastResult = ClickAttemptResult.failure(
            reason = ClickFailureReason.ACCESSIBILITY_UNAVAILABLE,
            attemptedPath = "tree_unavailable"
        )

        repeat(attempts.coerceAtLeast(1)) { attempt ->
            val treeSnapshotResult = treeSnapshotProvider()
            val snapshot = treeSnapshotResult.snapshot

            lastResult = if (!treeSnapshotResult.available || snapshot == null) {
                ClickAttemptResult.failure(
                    reason = ClickFailureReason.ACCESSIBILITY_UNAVAILABLE,
                    attemptedPath = "tree_unavailable"
                )
            } else {
                clickFirstMatch(
                    snapshot = snapshot,
                    strategy = strategy,
                    query = query,
                    clickableOnly = clickableOnly,
                    index = index
                )
            }

            if (lastResult.performed || lastResult.failureReason != ClickFailureReason.NODE_NOT_FOUND) {
                return lastResult
            }

            if (attempt < attempts - 1) {
                SystemClock.sleep(retryDelayMs)
            }
        }

        return lastResult
    }

    fun performInput(request: GhosthandInputRequest): InputOperationResult {
        return inputOperationPerformer.perform(
            request = request,
            focusedTextProvider = { focusedNodeResultProvider().node?.text ?: "" },
            interactionPlane = interactionPlane
        )
    }

    fun scrollNode(nodeId: String, direction: String): ScrollAttemptResult {
        val treeResult = treeSnapshotProvider()
        if (!treeResult.available || treeResult.snapshot == null) {
            return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "tree_unavailable"
            )
        }
        return interactionPlane.scrollNode(treeResult.snapshot, nodeId, direction)
    }

    fun scroll(direction: String, target: String?, count: Int): ScrollBatchResult {
        val treeResult = treeSnapshotProvider()
        if (!treeResult.available || treeResult.snapshot == null) {
            return ScrollBatchResult(
                performed = false,
                performedCount = 0,
                failureReason = ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "tree_unavailable"
            )
        }

        val nodeId = target?.takeIf { it.isNotBlank() }?.let { query ->
            val findResult: FindNodeResult = nodeFinder.findNodes(
                snapshot = treeResult.snapshot,
                strategy = "textContains",
                query = query,
                clickableOnly = false,
                index = 0
            )
            findResult.node?.nodeId
        } ?: treeResult.snapshot.nodes.firstOrNull { it.scrollable }?.nodeId
            ?: treeResult.snapshot.nodes.firstOrNull()?.nodeId

        if (nodeId == null) {
            return ScrollBatchResult(
                performed = false,
                performedCount = 0,
                failureReason = ScrollFailureReason.NODE_NOT_FOUND,
                attemptedPath = "scroll_target_missing"
            )
        }

        var performedCount = 0
        repeat(count.coerceAtLeast(1)) { index ->
            val result = scrollNode(nodeId, direction)
            if (!result.performed) {
                return ScrollBatchResult(
                    performed = performedCount > 0,
                    performedCount = performedCount,
                    failureReason = result.failureReason,
                    attemptedPath = result.attemptedPath
                )
            }
            performedCount += 1
            if (index < count - 1) {
                Thread.sleep(300L)
            }
        }

        return ScrollBatchResult(
            performed = true,
            performedCount = performedCount,
            failureReason = null,
            attemptedPath = "repeated_scroll"
        )
    }

    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        return interactionPlane.performLongPressGesture(x, y, durationMs)
    }

    fun performGesture(strokes: List<GestureStroke>): Boolean {
        return interactionPlane.performGesture(strokes)
    }
}
