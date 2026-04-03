/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import android.content.Context
import android.os.SystemClock
import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.interaction.execution.AccessibilityScreenshotAccess
import com.folklore25.ghosthand.capability.CapabilityAccessResolver
import com.folklore25.ghosthand.interaction.execution.AccessibilityInteractionPlane
import com.folklore25.ghosthand.interaction.execution.GhosthandScreenshotAccess
import com.folklore25.ghosthand.interaction.execution.GhosthandInteractionPlane
import com.folklore25.ghosthand.interaction.execution.InputOperationPerformer
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.payload.GhosthandInputRequest
import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.preview.ScreenPreviewCoordinator
import com.folklore25.ghosthand.screen.find.FocusedNodeResult
import com.folklore25.ghosthand.screen.find.ScreenFindCoordinator
import com.folklore25.ghosthand.screen.read.ScreenReadCoordinator
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.screen.read.ScreenSnapshotCoordinator
import com.folklore25.ghosthand.state.read.StateReadCoordinator
import com.folklore25.ghosthand.state.health.StateHealthPayloads
import com.folklore25.ghosthand.wait.UiStateSnapshot
import com.folklore25.ghosthand.wait.GhosthandWaitLogic
import com.folklore25.ghosthand.wait.WaitOutcome
import org.json.JSONObject

class StateCoordinator(
    context: Context,
    private val runtimeStateProvider: () -> RuntimeState
) {
    companion object {
        const val SCREEN_PREVIEW_WIDTH = 240
        const val SCREEN_PREVIEW_HEIGHT = 240
    }
    private val appContext = context.applicationContext
    private val homeDiagnosticsProvider = HomeDiagnosticsProvider(appContext)
    private val deviceSnapshotProvider = DeviceSnapshotProvider(appContext)
    private val foregroundAppProvider = ForegroundAppProvider(appContext)
    private val permissionSnapshotProvider = PermissionSnapshotProvider(appContext)
    private val accessibilityStatusProvider = AccessibilityStatusProvider(appContext)
    private val accessibilityTreeSnapshotProvider = AccessibilityTreeSnapshotProvider(appContext)
    private val accessibilityNodeFinder = AccessibilityNodeFinder()
    private val interactionPlane: GhosthandInteractionPlane = AccessibilityInteractionPlane()
    private val capabilityPolicyStore = CapabilityPolicyStore.getInstance(appContext)
    private val clipboardProvider = ClipboardProvider(appContext)
    private val mediaProjectionProvider = MediaProjectionProvider(appContext)
    private val capabilityAccessResolver = CapabilityAccessResolver(
        accessibilityStatusProvider = accessibilityStatusProvider,
        mediaProjectionProvider = mediaProjectionProvider,
        capabilityPolicyStore = capabilityPolicyStore
    )
    private val stateHealthPayloads = StateHealthPayloads
    private val inputOperationPerformer = InputOperationPerformer
    private val screenshotAccess: GhosthandScreenshotAccess = AccessibilityScreenshotAccess
    private val notificationDispatcher = NotificationDispatcher(appContext)
    private val screenOcrProvider = ScreenOcrProvider()
    private val screenPreviewCoordinator = ScreenPreviewCoordinator(
        screenshotAccess = screenshotAccess,
        mediaProjectionProvider = mediaProjectionProvider
    )
    private val screenSnapshotCoordinator = ScreenSnapshotCoordinator(accessibilityTreeSnapshotProvider)
    private val screenFindCoordinator = ScreenFindCoordinator(
        treeSnapshotProvider = accessibilityTreeSnapshotProvider,
        nodeFinder = accessibilityNodeFinder
    )
    private val stateReadCoordinator = StateReadCoordinator(
        runtimeStateProvider = runtimeStateProvider,
        treeSnapshotProvider = screenSnapshotCoordinator::getTreeSnapshotResult,
        homeDiagnosticsProvider = homeDiagnosticsProvider,
        deviceSnapshotProvider = deviceSnapshotProvider,
        foregroundAppProvider = foregroundAppProvider,
        permissionSnapshotProvider = permissionSnapshotProvider,
        accessibilityStatusProvider = accessibilityStatusProvider,
        capabilityAccessResolver = capabilityAccessResolver
    )
    private val screenReadCoordinator = ScreenReadCoordinator(
        capabilityAccessSnapshotProvider = stateReadCoordinator::capabilityAccessSnapshot,
        captureScreenshot = screenPreviewCoordinator::captureBestScreenshot,
        foregroundSnapshotProvider = foregroundAppProvider::snapshot,
        screenOcrProvider = screenOcrProvider,
        previewWidth = SCREEN_PREVIEW_WIDTH,
        previewHeight = SCREEN_PREVIEW_HEIGHT
    )

    fun createPingPayload(): JSONObject {
        val diagnosticsSnapshot = homeDiagnosticsProvider.snapshot()
        return JSONObject()
            .put("service", "ghosthand")
            .put("version", diagnosticsSnapshot.buildVersion)
    }

    fun createHealthPayload(): JSONObject {
        return stateHealthPayloads.createHealthPayload(runtimeStateProvider())
    }

    fun createStatePayload(): JSONObject {
        return stateReadCoordinator.createStatePayload()
    }

    fun capabilityAccessSnapshot(): CapabilityAccessSnapshot {
        return stateReadCoordinator.capabilityAccessSnapshot()
    }

    fun createForegroundPayload(): JSONObject {
        return stateReadCoordinator.createForegroundPayload()
    }

    fun createDevicePayload(): JSONObject {
        return stateReadCoordinator.createDevicePayload()
    }

    fun getTreeSnapshotResult(): AccessibilityTreeSnapshotResult {
        return screenSnapshotCoordinator.getTreeSnapshotResult()
    }

    fun createTreePayload(snapshot: AccessibilityTreeSnapshot, mode: String): JSONObject {
        return screenSnapshotCoordinator.createTreePayload(snapshot, mode)
    }

    fun createScreenPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        return screenSnapshotCoordinator.createScreenPayload(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    }

    fun createScreenReadPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        return screenReadCoordinator.createAccessibilityPayload(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    }

    fun createOcrScreenPayload(): ScreenReadPayload {
        return screenReadCoordinator.createOcrPayload()
    }

    fun createHybridScreenPayload(
        snapshot: AccessibilityTreeSnapshot,
        packageFilter: String?
    ): ScreenReadPayload {
        return screenReadCoordinator.createHybridPayload(
            snapshot = snapshot,
            packageFilter = packageFilter
        )
    }

    fun createFindPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): JSONObject {
        return screenFindCoordinator.createFindPayload(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
    }

    fun findResult(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): FindNodeResult {
        return screenFindCoordinator.findResult(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
    }

    fun tapPoint(x: Int, y: Int): TapAttemptResult {
        return interactionPlane.tapPoint(x, y)
    }

    fun tapNode(nodeId: String): TapAttemptResult {
        return interactionPlane.tapNode(nodeId)
    }

    fun swipe(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): SwipeAttemptResult {
        return interactionPlane.swipe(fromX, fromY, toX, toY, durationMs)
    }

    fun typeText(text: String): TypeAttemptResult {
        return interactionPlane.typeText(text)
    }

    fun createInfoPayload(): JSONObject {
        return stateReadCoordinator.createInfoPayload()
    }

    fun getFocusedNodeResult(): FocusedNodeResult {
        return screenFindCoordinator.getFocusedNodeResult()
    }

    fun createFocusedNodePayload(result: FocusedNodeResult): JSONObject {
        return screenFindCoordinator.createFocusedNodePayload(result)
    }

    fun clickNode(nodeId: String): ClickAttemptResult {
        return interactionPlane.clickNode(nodeId)
    }

    fun clickFirstMatch(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): ClickAttemptResult {
        val found = accessibilityNodeFinder.findNodesForClick(
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

        val clickResult = clickNode(nodeId)
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
            val treeSnapshotResult = accessibilityTreeSnapshotProvider.snapshot()
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

    fun inputText(text: String): TypeAttemptResult {
        return interactionPlane.typeText(text)
    }

    fun performInput(request: GhosthandInputRequest): InputOperationResult {
        return inputOperationPerformer.perform(
            request = request,
            focusedTextProvider = { getFocusedNodeResult().node?.text ?: "" },
            interactionPlane = interactionPlane
        )
    }

    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult {
        return interactionPlane.setTextOnNode(nodeId, text)
    }

    fun scrollNode(nodeId: String, direction: String): ScrollAttemptResult {
        val treeResult = accessibilityTreeSnapshotProvider.snapshot()
        if (!treeResult.available || treeResult.snapshot == null) {
            return ScrollAttemptResult.failure(
                reason = ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "tree_unavailable"
            )
        }
        return interactionPlane.scrollNode(treeResult.snapshot, nodeId, direction)
    }

    fun scroll(
        direction: String,
        target: String?,
        count: Int
    ): ScrollBatchResult {
        val treeResult = accessibilityTreeSnapshotProvider.snapshot()
        if (!treeResult.available || treeResult.snapshot == null) {
            return ScrollBatchResult(
                performed = false,
                performedCount = 0,
                failureReason = ScrollFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "tree_unavailable"
            )
        }

        val nodeId = target?.takeIf { it.isNotBlank() }?.let { query ->
            val findResult = accessibilityNodeFinder.findNodes(
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
        repeat(count.coerceAtLeast(1)) {
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
            if (it < count - 1) {
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

    fun performGlobalAction(action: Int): GlobalActionResult {
        return interactionPlane.performGlobalAction(action)
    }

    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        return interactionPlane.performLongPressGesture(x, y, durationMs)
    }

    fun performGesture(strokes: List<GestureStroke>): Boolean {
        return interactionPlane.performGesture(strokes)
    }

    fun readClipboard(): ClipboardReadResult {
        return clipboardProvider.readClipboard()
    }

    fun writeClipboard(text: String): ClipboardWriteResult {
        return clipboardProvider.writeClipboard(text)
    }

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        screenPreviewCoordinator.setMediaProjection(projection)
    }

    fun hasMediaProjection(): Boolean = screenPreviewCoordinator.hasMediaProjection()

    fun captureBestScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        return screenPreviewCoordinator.captureBestScreenshot(width, height)
    }

    fun postNotification(title: String, text: String): NotificationPostResult {
        return notificationDispatcher.postNotification(title, text)
    }

    fun cancelNotification(notificationId: Int): NotificationCancelResult {
        return notificationDispatcher.cancelNotification(notificationId)
    }

    fun readNotifications(packageFilter: String?, excludedPackages: Set<String>): JSONObject {
        return NotificationBuffer.toJson(packageFilter, excludedPackages)
    }

    fun waitForCondition(
        strategy: String,
        query: String?,
        timeoutMs: Long,
        intervalMs: Long
    ): WaitConditionResult {
        val initialTree = getTreeSnapshotResult().snapshot
        val initialForeground = foregroundAppProvider.snapshot()
        val initialState = StateCoordinatorObservationSupport.captureUiState(initialTree, initialForeground)
        val startTime = System.currentTimeMillis()
        val deadline = startTime + timeoutMs.coerceAtLeast(0L)

        while (System.currentTimeMillis() < deadline) {
            val treeResult = accessibilityTreeSnapshotProvider.snapshot()
            if (treeResult.available && treeResult.snapshot != null) {
                val found = accessibilityNodeFinder.findNode(
                    snapshot = treeResult.snapshot,
                    strategy = strategy,
                    query = query
                )
                if (found.found && found.node != null) {
                    val currentForeground = foregroundAppProvider.snapshot()
                    val finalState = StateCoordinatorObservationSupport.captureUiState(treeResult.snapshot, currentForeground)
                    return StateCoordinatorObservationSupport.conditionMatched(
                        initialState = initialState,
                        finalState = finalState,
                        node = found.node,
                        elapsedMs = System.currentTimeMillis() - startTime,
                        attemptedPath = "condition_met"
                    )
                }
            }
            val remaining = deadline - System.currentTimeMillis()
            if (remaining > 0) {
                Thread.sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        val finalTree = getTreeSnapshotResult().snapshot
        val finalForeground = foregroundAppProvider.snapshot()
        return StateCoordinatorObservationSupport.conditionTimedOut(
            initialState = initialState,
            finalState = StateCoordinatorObservationSupport.captureUiState(finalTree, finalForeground),
            elapsedMs = timeoutMs,
            attemptedPath = "timeout"
        )
    }

    fun waitForUiChange(timeoutMs: Long, intervalMs: Long): WaitUiChangeResult {
        val initialTree = getTreeSnapshotResult().snapshot
        val initialForeground = foregroundAppProvider.snapshot()
        val initialState = StateCoordinatorObservationSupport.captureUiState(initialTree, initialForeground)
        val startTime = System.currentTimeMillis()
        val deadline = startTime + timeoutMs.coerceAtLeast(0L)

        while (System.currentTimeMillis() < deadline) {
            val currentTree = getTreeSnapshotResult().snapshot
            val currentForeground = foregroundAppProvider.snapshot()
            val currentState = StateCoordinatorObservationSupport.captureUiState(currentTree, currentForeground)

            if (GhosthandWaitLogic.hasUiChanged(initialState, currentState)) {
                return StateCoordinatorObservationSupport.uiChangeDetected(
                    initialState = initialState,
                    currentState = currentState,
                    elapsedMs = System.currentTimeMillis() - startTime,
                    packageName = currentForeground.packageName,
                    activity = currentForeground.activity
                )
            }
            val remaining = deadline - System.currentTimeMillis()
            if (remaining > 0) {
                Thread.sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        val finalTree = getTreeSnapshotResult().snapshot
        val finalForeground = foregroundAppProvider.snapshot()
        val finalState = StateCoordinatorObservationSupport.captureUiState(finalTree, finalForeground)

        if (GhosthandWaitLogic.hasUiChanged(initialState, finalState)) {
            return StateCoordinatorObservationSupport.uiChangeDetected(
                initialState = initialState,
                currentState = finalState,
                elapsedMs = System.currentTimeMillis() - startTime,
                packageName = finalForeground.packageName,
                activity = finalForeground.activity
            )
        }

        return StateCoordinatorObservationSupport.uiChangeTimedOut(
            initialState = initialState,
            finalState = finalState,
            elapsedMs = System.currentTimeMillis() - startTime,
            packageName = finalForeground.packageName ?: initialState.packageName,
            activity = finalForeground.activity ?: initialState.activity
        )
    }

    data class WaitConditionResult(
        val satisfied: Boolean,
        val outcome: WaitOutcome,
        val node: FlatAccessibilityNode?,
        val elapsedMs: Long,
        val polledCount: Int,
        val attemptedPath: String
    ) {
        fun matchedCondition(): Boolean {
            return satisfied && outcome.conditionMet == true && node != null
        }
    }

    data class WaitUiChangeResult(
        val changed: Boolean,
        val outcome: WaitOutcome,
        val elapsedMs: Long,
        val snapshotToken: String?,
        val packageName: String?,
        val activity: String?
    )
}

internal object StateCoordinatorObservationSupport {
    fun captureUiState(
        treeSnapshot: AccessibilityTreeSnapshot?,
        foregroundSnapshot: ForegroundAppSnapshot
    ): UiStateSnapshot {
        return UiStateSnapshot(
            snapshotToken = treeSnapshot?.snapshotToken,
            packageName = foregroundSnapshot.packageName,
            activity = foregroundSnapshot.activity
        )
    }

    fun conditionMatched(
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

    fun conditionTimedOut(
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

    fun uiChangeDetected(
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

    fun uiChangeTimedOut(
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

data class InputOperationResult(
    val performed: Boolean,
    val textMutation: InputTextMutationResult? = null,
    val keyDispatch: InputKeyDispatchResult? = null,
    val postActionState: PostActionState? = null
)

data class InputTextMutationResult(
    val requested: Boolean,
    val performed: Boolean,
    val action: String,
    val previousText: String,
    val finalText: String,
    val backendUsed: String?,
    val failureReason: TypeFailureReason?,
    val attemptedPath: String
)

data class InputKeyDispatchResult(
    val requested: Boolean,
    val performed: Boolean,
    val key: String,
    val backendUsed: String?,
    val failureReason: InputKeyFailureReason?,
    val attemptedPath: String
)

data class ScrollBatchResult(
    val performed: Boolean,
    val performedCount: Int,
    val failureReason: ScrollFailureReason?,
    val attemptedPath: String
)
