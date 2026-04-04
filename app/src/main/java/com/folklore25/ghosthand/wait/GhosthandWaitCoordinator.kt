/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.wait

import com.folklore25.ghosthand.screen.find.AccessibilityNodeFinder
import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshotResult
import com.folklore25.ghosthand.screen.read.FlatAccessibilityNode
import com.folklore25.ghosthand.state.device.ForegroundAppSnapshot

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.state.StateCoordinator

internal class GhosthandWaitCoordinator(
    private val treeSnapshotProvider: () -> AccessibilityTreeSnapshotResult,
    private val foregroundSnapshotProvider: () -> ForegroundAppSnapshot,
    private val nodeFinder: AccessibilityNodeFinder,
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
    private val sleep: (Long) -> Unit = Thread::sleep
) {
    fun waitForCondition(
        strategy: String,
        query: String?,
        timeoutMs: Long,
        intervalMs: Long
    ): StateCoordinator.WaitConditionResult {
        val initialState = captureUiState(
            treeSnapshot = treeSnapshotProvider().snapshot,
            foregroundSnapshot = foregroundSnapshotProvider()
        )
        val startTime = currentTimeMs()
        val deadline = startTime + timeoutMs.coerceAtLeast(0L)

        while (currentTimeMs() < deadline) {
            val treeResult = treeSnapshotProvider()
            if (treeResult.available && treeResult.snapshot != null) {
                val found = nodeFinder.findNode(
                    snapshot = treeResult.snapshot,
                    strategy = strategy,
                    query = query
                )
                if (found.found && found.node != null) {
                    val finalState = captureUiState(
                        treeSnapshot = treeResult.snapshot,
                        foregroundSnapshot = foregroundSnapshotProvider()
                    )
                    return conditionMatched(
                        initialState = initialState,
                        finalState = finalState,
                        node = found.node,
                        elapsedMs = currentTimeMs() - startTime,
                        attemptedPath = "condition_met"
                    )
                }
            }

            val remaining = deadline - currentTimeMs()
            if (remaining > 0) {
                sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        val finalState = captureUiState(
            treeSnapshot = treeSnapshotProvider().snapshot,
            foregroundSnapshot = foregroundSnapshotProvider()
        )
        return conditionTimedOut(
            initialState = initialState,
            finalState = finalState,
            elapsedMs = timeoutMs,
            attemptedPath = "timeout"
        )
    }

    fun waitForUiChange(timeoutMs: Long, intervalMs: Long): StateCoordinator.WaitUiChangeResult {
        val initialState = captureUiState(
            treeSnapshot = treeSnapshotProvider().snapshot,
            foregroundSnapshot = foregroundSnapshotProvider()
        )
        val startTime = currentTimeMs()
        val deadline = startTime + timeoutMs.coerceAtLeast(0L)

        while (currentTimeMs() < deadline) {
            val currentForeground = foregroundSnapshotProvider()
            val currentState = captureUiState(
                treeSnapshot = treeSnapshotProvider().snapshot,
                foregroundSnapshot = currentForeground
            )

            if (GhosthandWaitLogic.hasUiChanged(initialState, currentState)) {
                return uiChangeDetected(
                    initialState = initialState,
                    currentState = currentState,
                    elapsedMs = currentTimeMs() - startTime,
                    packageName = currentForeground.packageName,
                    activity = currentForeground.activity
                )
            }

            val remaining = deadline - currentTimeMs()
            if (remaining > 0) {
                sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        val finalForeground = foregroundSnapshotProvider()
        val finalState = captureUiState(
            treeSnapshot = treeSnapshotProvider().snapshot,
            foregroundSnapshot = finalForeground
        )

        if (GhosthandWaitLogic.hasUiChanged(initialState, finalState)) {
            return uiChangeDetected(
                initialState = initialState,
                currentState = finalState,
                elapsedMs = currentTimeMs() - startTime,
                packageName = finalForeground.packageName,
                activity = finalForeground.activity
            )
        }

        return uiChangeTimedOut(
            initialState = initialState,
            finalState = finalState,
            elapsedMs = currentTimeMs() - startTime,
            packageName = finalForeground.packageName ?: initialState.packageName,
            activity = finalForeground.activity ?: initialState.activity
        )
    }

    private fun captureUiState(
        treeSnapshot: AccessibilityTreeSnapshot?,
        foregroundSnapshot: ForegroundAppSnapshot
    ): UiStateSnapshot {
        return UiStateSnapshot(
            snapshotToken = treeSnapshot?.snapshotToken,
            packageName = foregroundSnapshot.packageName,
            activity = foregroundSnapshot.activity
        )
    }

    private fun conditionMatched(
        initialState: UiStateSnapshot,
        finalState: UiStateSnapshot,
        node: FlatAccessibilityNode,
        elapsedMs: Long,
        attemptedPath: String
    ): StateCoordinator.WaitConditionResult {
        return StateCoordinator.WaitConditionResult(
            satisfied = true,
            outcome = WaitOutcome.forCondition(
                conditionMet = true,
                initialState = initialState,
                finalState = finalState,
                timedOut = false
            ),
            node = node,
            elapsedMs = elapsedMs,
            polledCount = 0,
            attemptedPath = attemptedPath
        )
    }

    private fun conditionTimedOut(
        initialState: UiStateSnapshot,
        finalState: UiStateSnapshot,
        elapsedMs: Long,
        attemptedPath: String
    ): StateCoordinator.WaitConditionResult {
        return StateCoordinator.WaitConditionResult(
            satisfied = false,
            outcome = WaitOutcome.forCondition(
                conditionMet = false,
                initialState = initialState,
                finalState = finalState,
                timedOut = true
            ),
            node = null,
            elapsedMs = elapsedMs,
            polledCount = 0,
            attemptedPath = attemptedPath
        )
    }

    private fun uiChangeDetected(
        initialState: UiStateSnapshot,
        currentState: UiStateSnapshot,
        elapsedMs: Long,
        packageName: String?,
        activity: String?
    ): StateCoordinator.WaitUiChangeResult {
        return StateCoordinator.WaitUiChangeResult(
            changed = true,
            outcome = WaitOutcome.forUiChange(
                stateChanged = GhosthandWaitLogic.hasUiChanged(initialState, currentState),
                timedOut = false
            ),
            elapsedMs = elapsedMs,
            snapshotToken = currentState.snapshotToken ?: initialState.snapshotToken,
            packageName = packageName,
            activity = activity
        )
    }

    private fun uiChangeTimedOut(
        initialState: UiStateSnapshot,
        finalState: UiStateSnapshot,
        elapsedMs: Long,
        packageName: String?,
        activity: String?
    ): StateCoordinator.WaitUiChangeResult {
        return StateCoordinator.WaitUiChangeResult(
            changed = false,
            outcome = WaitOutcome.forUiChange(
                stateChanged = false,
                timedOut = true
            ),
            elapsedMs = elapsedMs,
            snapshotToken = finalState.snapshotToken ?: initialState.snapshotToken,
            packageName = packageName,
            activity = activity
        )
    }
}
