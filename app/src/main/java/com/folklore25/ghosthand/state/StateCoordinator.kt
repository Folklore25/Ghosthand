/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

import com.folklore25.ghosthand.screen.find.AccessibilityNodeFinder
import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshotProvider
import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshotResult
import com.folklore25.ghosthand.capability.CapabilityAccessSnapshot
import com.folklore25.ghosthand.capability.CapabilityPolicyStore
import com.folklore25.ghosthand.interaction.accessibility.ClickAttemptResult
import com.folklore25.ghosthand.interaction.clipboard.ClipboardProvider
import com.folklore25.ghosthand.interaction.clipboard.ClipboardReadResult
import com.folklore25.ghosthand.interaction.clipboard.ClipboardWriteResult
import com.folklore25.ghosthand.screen.find.FindNodeResult
import com.folklore25.ghosthand.screen.read.FlatAccessibilityNode
import com.folklore25.ghosthand.interaction.execution.GestureStroke
import com.folklore25.ghosthand.interaction.execution.GlobalActionResult
import com.folklore25.ghosthand.interaction.accessibility.InputKeyFailureReason
import com.folklore25.ghosthand.integration.projection.MediaProjectionProvider
import com.folklore25.ghosthand.notification.NotificationBuffer
import com.folklore25.ghosthand.notification.NotificationCancelResult
import com.folklore25.ghosthand.notification.NotificationDispatcher
import com.folklore25.ghosthand.notification.NotificationPostResult
import com.folklore25.ghosthand.screen.ocr.ScreenOcrProvider
import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.accessibility.ScrollAttemptResult
import com.folklore25.ghosthand.interaction.accessibility.ScrollFailureReason
import com.folklore25.ghosthand.interaction.accessibility.SetTextAttemptResult
import com.folklore25.ghosthand.interaction.accessibility.SwipeAttemptResult
import com.folklore25.ghosthand.interaction.accessibility.TapAttemptResult
import com.folklore25.ghosthand.interaction.accessibility.TypeAttemptResult
import com.folklore25.ghosthand.interaction.accessibility.TypeFailureReason

import com.folklore25.ghosthand.R

import android.content.Context
import com.folklore25.ghosthand.interaction.execution.AccessibilityScreenshotAccess
import com.folklore25.ghosthand.capability.CapabilityAccessResolver
import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation
import com.folklore25.ghosthand.interaction.execution.AccessibilityInteractionPlane
import com.folklore25.ghosthand.interaction.execution.GhosthandScreenshotAccess
import com.folklore25.ghosthand.interaction.execution.GhosthandInteractionPlane
import com.folklore25.ghosthand.interaction.execution.InteractionExecutionCoordinator
import com.folklore25.ghosthand.interaction.execution.InputOperationPerformer
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
import com.folklore25.ghosthand.state.runtime.RuntimeState
import com.folklore25.ghosthand.state.diagnostics.HomeDiagnosticsProvider
import com.folklore25.ghosthand.state.device.DeviceSnapshotProvider
import com.folklore25.ghosthand.state.device.ForegroundAppProvider
import com.folklore25.ghosthand.state.device.PermissionSnapshotProvider
import com.folklore25.ghosthand.state.read.AccessibilityStatusProvider
import com.folklore25.ghosthand.wait.GhosthandWaitCoordinator
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
    private val peripheralCoordinator = StatePeripheralCoordinator(
        clipboardProvider = clipboardProvider,
        notificationDispatcher = notificationDispatcher
    )
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
    private val interactionExecutionCoordinator = InteractionExecutionCoordinator(
        treeSnapshotProvider = screenSnapshotCoordinator::getTreeSnapshotResult,
        nodeFinder = accessibilityNodeFinder,
        interactionPlane = interactionPlane,
        focusedNodeResultProvider = screenFindCoordinator::getFocusedNodeResult,
        inputOperationPerformer = inputOperationPerformer
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
    private val waitCoordinator = GhosthandWaitCoordinator(
        treeSnapshotProvider = screenSnapshotCoordinator::getTreeSnapshotResult,
        foregroundSnapshotProvider = foregroundAppProvider::snapshot,
        nodeFinder = accessibilityNodeFinder
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

    fun createCapabilitiesPayload(): JSONObject {
        return JSONObject(
            GhosthandCapabilityPresentation.capabilitiesFields(
                capabilityAccess = stateReadCoordinator.capabilityAccessSnapshot(),
                permissionSnapshot = permissionSnapshotProvider.snapshot()
            )
        )
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
        return interactionExecutionCoordinator.clickFirstMatch(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
    }

    fun clickFirstMatchFresh(
        strategy: String,
        query: String,
        clickableOnly: Boolean = false,
        index: Int = 0,
        attempts: Int = 4,
        retryDelayMs: Long = 250L
    ): ClickAttemptResult {
        return interactionExecutionCoordinator.clickFirstMatchFresh(
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index,
            attempts = attempts,
            retryDelayMs = retryDelayMs
        )
    }

    fun inputText(text: String): TypeAttemptResult {
        return interactionPlane.typeText(text)
    }

    fun performInput(request: GhosthandInputRequest): InputOperationResult {
        return interactionExecutionCoordinator.performInput(request)
    }

    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult {
        return interactionPlane.setTextOnNode(nodeId, text)
    }

    fun scrollNode(nodeId: String, direction: String): ScrollAttemptResult {
        return interactionExecutionCoordinator.scrollNode(nodeId, direction)
    }

    fun scroll(
        direction: String,
        target: String?,
        count: Int
    ): ScrollBatchResult {
        return interactionExecutionCoordinator.scroll(
            direction = direction,
            target = target,
            count = count
        )
    }

    fun performGlobalAction(action: Int): GlobalActionResult {
        return interactionPlane.performGlobalAction(action)
    }

    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        return interactionExecutionCoordinator.performLongPressGesture(x, y, durationMs)
    }

    fun performGesture(strokes: List<GestureStroke>): Boolean {
        return interactionExecutionCoordinator.performGesture(strokes)
    }

    fun readClipboard(): ClipboardReadResult {
        return peripheralCoordinator.readClipboard()
    }

    fun writeClipboard(text: String): ClipboardWriteResult {
        return peripheralCoordinator.writeClipboard(text)
    }

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        screenPreviewCoordinator.setMediaProjection(projection)
    }

    fun hasMediaProjection(): Boolean = screenPreviewCoordinator.hasMediaProjection()

    fun captureBestScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        return screenPreviewCoordinator.captureBestScreenshot(width, height)
    }

    fun postNotification(title: String, text: String): NotificationPostResult {
        return peripheralCoordinator.postNotification(title, text)
    }

    fun cancelNotification(notificationId: Int): NotificationCancelResult {
        return peripheralCoordinator.cancelNotification(notificationId)
    }

    fun readNotifications(packageFilter: String?, excludedPackages: Set<String>): JSONObject {
        return peripheralCoordinator.readNotifications(packageFilter, excludedPackages)
    }

    fun waitForCondition(
        strategy: String,
        query: String?,
        timeoutMs: Long,
        intervalMs: Long
    ): WaitConditionResult {
        return waitCoordinator.waitForCondition(
            strategy = strategy,
            query = query,
            timeoutMs = timeoutMs,
            intervalMs = intervalMs
        )
    }

    fun waitForUiChange(timeoutMs: Long, intervalMs: Long): WaitUiChangeResult {
        return waitCoordinator.waitForUiChange(timeoutMs, intervalMs)
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
