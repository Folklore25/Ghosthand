/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.os.SystemClock
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
    private val notificationDispatcher = NotificationDispatcher(appContext)
    private val screenOcrProvider = ScreenOcrProvider()

    fun createPingPayload(): JSONObject {
        val diagnosticsSnapshot = homeDiagnosticsProvider.snapshot()
        return JSONObject()
            .put("service", "ghosthand")
            .put("version", diagnosticsSnapshot.buildVersion)
    }

    fun createHealthPayload(): JSONObject {
        val runtimeState = runtimeStateProvider()
        val ready = isRuntimeReady(runtimeState)

        return JSONObject()
            .put("status", if (ready) "ready" else "starting")
            .put("ready", ready)
            .put("listener", JSONObject()
                .put("host", LocalApiServer.HOST)
                .put("port", LocalApiServer.PORT)
            )
            .put("runtime", JSONObject()
                .put("appStarted", runtimeState.appStarted)
                .put("localApiServerRunning", runtimeState.localApiServerRunning)
                .put("foregroundServiceRunning", runtimeState.foregroundServiceRunning)
                .put("lastServiceAction", runtimeState.lastServiceAction)
                .put("statusText", runtimeState.statusText)
            )
    }

    fun createStatePayload(): JSONObject {
        val runtimeState = runtimeStateProvider()
        val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
        val diagnosticsSnapshot = homeDiagnosticsProvider.snapshot()
        val deviceSnapshot = deviceSnapshotProvider.snapshot()
        val foregroundSnapshot = foregroundAppProvider.snapshot()
        val permissionSnapshot = permissionSnapshotProvider.snapshot()
        val accessibilitySnapshot = accessibilityStatusProvider.snapshot(
            isConnected = executionStatus.connected,
            isDispatchCapable = executionStatus.dispatchCapable
        )
        val capabilityPolicy = capabilityPolicyStore.snapshot()
        val capabilityAccess = CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = accessibilitySnapshot,
            mediaProjectionGranted = mediaProjectionProvider.hasProjection(),
            policy = capabilityPolicy
        )
        val runtimeUptimeMs = runtimeState.appStartedAtElapsedRealtimeMs?.let {
            (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L)
        }

        return StateCoordinatorStatePayloadSupport.createStatePayload(
            runtimeState = runtimeState,
            runtimeReady = isRuntimeReady(runtimeState),
            runtimeUptimeMs = runtimeUptimeMs,
            diagnosticsSnapshot = diagnosticsSnapshot,
            deviceSnapshot = deviceSnapshot,
            foregroundSnapshot = foregroundSnapshot,
            accessibilitySnapshot = accessibilitySnapshot,
            capabilityAccess = capabilityAccess,
            permissionSnapshot = permissionSnapshot
        )
    }

    fun capabilityAccessSnapshot(): CapabilityAccessSnapshot {
        val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
        val accessibilitySnapshot = accessibilityStatusProvider.snapshot(
            isConnected = executionStatus.connected,
            isDispatchCapable = executionStatus.dispatchCapable
        )
        return CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = accessibilitySnapshot,
            mediaProjectionGranted = mediaProjectionProvider.hasProjection(),
            policy = capabilityPolicyStore.snapshot()
        )
    }

    fun createForegroundPayload(): JSONObject {
        return foregroundAppProvider.toJson(foregroundAppProvider.snapshot())
    }

    fun createDevicePayload(): JSONObject {
        val deviceSnapshot = deviceSnapshotProvider.snapshot()
        val foregroundSnapshot = foregroundAppProvider.snapshot()

        return JSONObject()
            .put("screenOn", deviceSnapshot.screenOn)
            .put("locked", deviceSnapshot.locked ?: JSONObject.NULL)
            .put("rotation", deviceSnapshot.rotation)
            .put("batteryPercent", deviceSnapshot.batteryPercent ?: JSONObject.NULL)
            .put("charging", deviceSnapshot.charging)
            .put("foregroundPackage", foregroundSnapshot.packageName ?: JSONObject.NULL)
    }

    fun getTreeSnapshotResult(): AccessibilityTreeSnapshotResult {
        return accessibilityTreeSnapshotProvider.snapshot()
    }

    fun createTreePayload(snapshot: AccessibilityTreeSnapshot, mode: String): JSONObject {
        return if (mode == "raw") {
            accessibilityTreeSnapshotProvider.toRawJson(snapshot)
        } else {
            accessibilityTreeSnapshotProvider.toJson(snapshot)
        }
    }

    fun createScreenPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        return accessibilityTreeSnapshotProvider.toScreenJson(
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
        val screenshotUsableNow = capabilityAccessSnapshot().screenshot.effective.usableNow
        return StateCoordinatorScreenPayloadSupport.applyPreviewMetadata(
            payload = GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot,
                editableOnly = editableOnly,
                scrollableOnly = scrollableOnly,
                packageFilter = packageFilter,
                clickableOnly = clickableOnly
            ),
            screenshotUsableNow = screenshotUsableNow,
            previewToken = snapshot.snapshotToken?.let { "preview:$it" },
            previewWidth = SCREEN_PREVIEW_WIDTH,
            previewHeight = SCREEN_PREVIEW_HEIGHT
        )
    }

    fun createOcrScreenPayload(): ScreenReadPayload {
        val screenshotResult = captureBestScreenshot(0, 0)
        val foregroundSnapshot = foregroundAppProvider.snapshot()
        val ocrResult = screenOcrProvider.read(screenshotResult)

        return ScreenReadPayload(
            packageName = foregroundSnapshot.packageName,
            activity = foregroundSnapshot.activity,
            snapshotToken = null,
            capturedAt = null,
            foregroundStableDuringCapture = true,
            partialOutput = false,
            candidateNodeCount = 0,
            returnedElementCount = ocrResult.elements.size,
            warnings = ocrResult.warnings,
            omittedInvalidBoundsCount = 0,
            omittedLowSignalCount = 0,
            omittedNodeCount = 0,
            omittedCategories = emptyList(),
            omittedSummary = null,
            invalidBoundsPresent = false,
            lowSignalPresent = false,
            elements = ocrResult.elements,
            source = ScreenReadMode.OCR.wireValue,
            accessibilityElementCount = 0,
            ocrElementCount = ocrResult.elements.size,
            usedOcrFallback = false,
            visualAvailable = true,
            previewAvailable = true,
            previewToken = foregroundSnapshot.packageName?.let { "preview:$it:${foregroundSnapshot.activity ?: "unknown"}" },
            previewWidth = SCREEN_PREVIEW_WIDTH,
            previewHeight = SCREEN_PREVIEW_HEIGHT
        )
    }

    fun createHybridScreenPayload(
        snapshot: AccessibilityTreeSnapshot,
        packageFilter: String?
    ): ScreenReadPayload {
        val accessibilityPayload = StateCoordinatorScreenPayloadSupport.applyPreviewMetadata(
            payload = GhosthandApiPayloads.accessibilityScreenRead(
                snapshot = snapshot,
                editableOnly = false,
                scrollableOnly = false,
                packageFilter = packageFilter,
                clickableOnly = false
            ),
            screenshotUsableNow = capabilityAccessSnapshot().screenshot.effective.usableNow,
            previewToken = snapshot.snapshotToken?.let { "preview:$it" },
            previewWidth = SCREEN_PREVIEW_WIDTH,
            previewHeight = SCREEN_PREVIEW_HEIGHT
        )
        if (!accessibilityPayload.accessibilityTreeIsOperationallyInsufficient()) {
            return accessibilityPayload
        }

        val ocrPayload = createOcrScreenPayload()
        return StateCoordinatorScreenPayloadSupport.mergeHybridPayloads(
            accessibilityPayload = accessibilityPayload,
            ocrPayload = ocrPayload
        )
    }

    fun createFindPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): JSONObject {
        val result = findResult(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )

        return GhosthandApiPayloads.findPayload(result)
    }

    fun findResult(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): FindNodeResult {
        return accessibilityNodeFinder.findNodes(
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
        val treeResult = accessibilityTreeSnapshotProvider.snapshot()
        val deviceSnapshot = deviceSnapshotProvider.snapshot()
        val foregroundSnapshot = foregroundAppProvider.snapshot()

        return JSONObject()
            .put("package", foregroundSnapshot.packageName ?: JSONObject.NULL)
            .put("activity", foregroundSnapshot.activity ?: JSONObject.NULL)
            .put("label", foregroundSnapshot.label ?: JSONObject.NULL)
            .put("screen", JSONObject()
                .put("on", deviceSnapshot.screenOn)
                .put("rotation", deviceSnapshot.rotation)
                .put("batteryPercent", deviceSnapshot.batteryPercent ?: JSONObject.NULL)
                .put("charging", deviceSnapshot.charging)
            )
            .put("tree", JSONObject()
                .put("available", treeResult.available)
                .put("reason", treeResult.reason?.name ?: JSONObject.NULL)
            )
    }

    fun getFocusedNodeResult(): FocusedNodeResult {
        val treeResult = accessibilityTreeSnapshotProvider.snapshot()
        if (!treeResult.available || treeResult.snapshot == null) {
            return FocusedNodeResult(
                available = false,
                node = null,
                reason = "accessibility_unavailable"
            )
        }

        val found = accessibilityNodeFinder.findNode(
            snapshot = treeResult.snapshot,
            strategy = "focused",
            query = null
        )

        return FocusedNodeResult(
            available = true,
            node = found.node,
            reason = null
        )
    }

    fun createFocusedNodePayload(result: FocusedNodeResult): JSONObject {
        return JSONObject()
            .put("available", result.available)
            .put("node", result.node?.let { accessibilityTreeSnapshotProvider.toJson(it) } ?: JSONObject.NULL)
            .put("reason", result.reason ?: JSONObject.NULL)
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
        val textMutation = request.textAction?.let { action ->
            val previousText = getFocusedNodeResult().node?.text ?: ""
            val finalText = when (action) {
                InputTextAction.SET -> request.text ?: ""
                InputTextAction.APPEND -> previousText + (request.text ?: "")
                InputTextAction.CLEAR -> ""
            }
            val result = interactionPlane.typeText(finalText)
            InputTextMutationResult(
                requested = true,
                performed = result.performed,
                action = action.wireValue,
                previousText = previousText,
                finalText = finalText,
                backendUsed = result.backendUsed,
                failureReason = result.failureReason,
                attemptedPath = result.attemptedPath
            )
        }

        val keyDispatch = request.key?.let { key ->
            val result = interactionPlane.dispatchKey(key)
            InputKeyDispatchResult(
                requested = true,
                performed = result.performed,
                key = key.wireValue,
                backendUsed = result.backendUsed,
                failureReason = result.failureReason,
                attemptedPath = result.attemptedPath
            )
        }

        return InputOperationResult(
            performed = listOfNotNull(
                textMutation?.performed,
                keyDispatch?.performed
            ).let { requested -> requested.isNotEmpty() && requested.all { it } }
        ).copy(
            textMutation = textMutation,
            keyDispatch = keyDispatch
        )
    }

    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult {
        return interactionPlane.setTextOnNode(nodeId, text)
    }

    fun takeScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "service_missing"
            )
        return service.takeScreenshot(width, height)
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
        mediaProjectionProvider.setProjection(projection)
    }

    fun hasMediaProjection(): Boolean = mediaProjectionProvider.hasProjection()

    fun captureFullScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        return mediaProjectionProvider.captureScreenshot(width, height)
    }

    fun captureBestScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        val serviceCapture = takeScreenshot(width, height)
        if (serviceCapture.available) {
            return serviceCapture
        }

        val projectionCapture = captureFullScreenshot(width, height)
        if (projectionCapture.available) {
            return projectionCapture
        }

        return if (projectionCapture.attemptedPath != "projection_missing") {
            projectionCapture
        } else if (serviceCapture.attemptedPath != "service_disconnected") {
            serviceCapture
        } else {
            projectionCapture
        }
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

    private fun isRuntimeReady(runtimeState: RuntimeState): Boolean {
        return runtimeState.appStarted &&
            runtimeState.localApiServerRunning &&
            runtimeState.foregroundServiceRunning
    }

    data class FocusedNodeResult(
        val available: Boolean,
        val node: FlatAccessibilityNode?,
        val reason: String?
    )

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

internal object StateCoordinatorScreenPayloadSupport {
    fun applyPreviewMetadata(
        payload: ScreenReadPayload,
        screenshotUsableNow: Boolean,
        previewToken: String?,
        previewWidth: Int,
        previewHeight: Int
    ): ScreenReadPayload {
        return payload.copy(
            visualAvailable = screenshotUsableNow,
            previewAvailable = screenshotUsableNow,
            previewToken = previewToken,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun mergeHybridPayloads(
        accessibilityPayload: ScreenReadPayload,
        ocrPayload: ScreenReadPayload
    ): ScreenReadPayload {
        if (ocrPayload.elements.isEmpty()) {
            return accessibilityPayload.copy(
                warnings = (accessibilityPayload.warnings + ocrPayload.warnings).distinct()
            )
        }

        return accessibilityPayload.copy(
            returnedElementCount = accessibilityPayload.elements.size + ocrPayload.elements.size,
            warnings = (accessibilityPayload.warnings + listOf("ocr_fallback_used") + ocrPayload.warnings).distinct(),
            elements = accessibilityPayload.elements + ocrPayload.elements,
            source = ScreenReadMode.HYBRID.wireValue,
            ocrElementCount = ocrPayload.ocrElementCount,
            usedOcrFallback = true,
            visualAvailable = ocrPayload.visualAvailable ?: accessibilityPayload.visualAvailable,
            previewAvailable = ocrPayload.previewAvailable ?: accessibilityPayload.previewAvailable,
            previewToken = accessibilityPayload.previewToken ?: ocrPayload.previewToken,
            previewWidth = accessibilityPayload.previewWidth ?: ocrPayload.previewWidth,
            previewHeight = accessibilityPayload.previewHeight ?: ocrPayload.previewHeight
        )
    }
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

internal object StateCoordinatorStatePayloadSupport {
    fun createStatePayload(
        runtimeState: RuntimeState,
        runtimeReady: Boolean,
        runtimeUptimeMs: Long?,
        diagnosticsSnapshot: HomeDiagnosticsSnapshot,
        deviceSnapshot: DeviceSnapshot,
        foregroundSnapshot: ForegroundAppSnapshot,
        accessibilitySnapshot: AccessibilityStatusSnapshot,
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): JSONObject {
        return JSONObject()
            .put("runtime", JSONObject()
                .put("ready", runtimeReady)
                .put("runtimeUptimeMs", runtimeUptimeMs ?: JSONObject.NULL)
                .put("appStartedAt", runtimeState.appStartedAtIso ?: JSONObject.NULL)
                .put("buildVersion", diagnosticsSnapshot.buildVersion)
                .put("installIdentity", diagnosticsSnapshot.installIdentity)
                .put("tapProbeUiBuildState", diagnosticsSnapshot.tapProbeUiBuildState)
                .put("foregroundServiceRunning", runtimeState.foregroundServiceRunning)
                .put("appStarted", runtimeState.appStarted)
                .put("lastServiceAction", runtimeState.lastServiceAction)
                .put("statusText", runtimeState.statusText)
            )
            .put("accessibility", JSONObject()
                .put("implemented", accessibilitySnapshot.implemented)
                .put("enabled", accessibilitySnapshot.enabled)
                .put("connected", accessibilitySnapshot.connected)
                .put("dispatchCapable", accessibilitySnapshot.dispatchCapable)
                .put("healthy", accessibilitySnapshot.healthy ?: JSONObject.NULL)
                .put("status", accessibilitySnapshot.status)
            )
            .put("device", JSONObject()
                .put("screenOn", deviceSnapshot.screenOn)
                .put("locked", deviceSnapshot.locked ?: JSONObject.NULL)
                .put("rotation", deviceSnapshot.rotation)
                .put("batteryPercent", deviceSnapshot.batteryPercent)
                .put("charging", deviceSnapshot.charging)
                .put("foregroundPackage", foregroundSnapshot.packageName ?: JSONObject.NULL)
            )
            .put("openclaw", JSONObject()
                .put("apiServerReady", runtimeState.localApiServerRunning)
                .put("port", LocalApiServer.PORT)
            )
            .put("recovery", JSONObject()
                .put("implemented", false)
                .put("lastAction", JSONObject.NULL)
                .put("lastResult", JSONObject.NULL)
                .put("status", "not_implemented")
            )
            .put(
                "permissions",
                JSONObject(
                    permissionsPayload(
                        accessibilityEnabled = accessibilitySnapshot.enabled,
                        capabilityAccess = capabilityAccess
                    )
                )
            )
            .put("systemPermissions", JSONObject(systemPermissionsPayload(permissionSnapshot)))
    }

    fun permissionsPayload(
        accessibilityEnabled: Boolean,
        capabilityAccess: CapabilityAccessSnapshot
    ): Map<String, Any?> {
        return linkedMapOf(
            "implemented" to true,
            "accessibility" to accessibilityEnabled,
            "capabilitySummary" to linkedMapOf(
                "accessibility" to linkedMapOf(
                    "allowed" to capabilityAccess.accessibility.policy.allowed,
                    "usableNow" to capabilityAccess.accessibility.effective.usableNow,
                    "reason" to capabilityAccess.accessibility.effective.reason
                ),
                "screenshot" to linkedMapOf(
                    "allowed" to capabilityAccess.screenshot.policy.allowed,
                    "usableNow" to capabilityAccess.screenshot.effective.usableNow,
                    "reason" to capabilityAccess.screenshot.effective.reason
                )
            ),
            "capabilities" to linkedMapOf(
                "accessibility" to GovernedCapabilityPayloads.accessibilityToJson(capabilityAccess.accessibility),
                "screenshot" to GovernedCapabilityPayloads.screenshotToJson(capabilityAccess.screenshot)
            )
        )
    }

    fun systemPermissionsPayload(permissionSnapshot: PermissionSnapshot): Map<String, Any?> {
        return linkedMapOf(
            "usageAccess" to permissionSnapshot.usageAccess,
            "notifications" to permissionSnapshot.notifications,
            "overlay" to permissionSnapshot.overlay,
            "writeSecureSettings" to permissionSnapshot.writeSecureSettings
        )
    }
}

internal object GovernedCapabilityPayloads {
    fun accessibilityToJson(snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>): JSONObject {
        return JSONObject(accessibilityFields(snapshot))
    }

    fun screenshotToJson(snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>): JSONObject {
        return JSONObject(screenshotFields(snapshot))
    }

    fun accessibilityFields(
        snapshot: GovernedCapabilitySnapshot<AccessibilitySystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "system" to linkedMapOf(
                "authorized" to snapshot.system.authorized,
                "enabled" to snapshot.system.enabled,
                "connected" to snapshot.system.connected,
                "dispatchCapable" to snapshot.system.dispatchCapable,
                "healthy" to snapshot.system.healthy,
                "status" to snapshot.system.status
            ),
            "policy" to linkedMapOf("allowed" to snapshot.policy.allowed),
            "effective" to linkedMapOf(
                "usableNow" to snapshot.effective.usableNow,
                "reason" to snapshot.effective.reason
            )
        )
    }

    fun screenshotFields(
        snapshot: GovernedCapabilitySnapshot<ScreenshotSystemAuthorizationState>
    ): Map<String, Any?> {
        return linkedMapOf(
            "system" to linkedMapOf(
                "authorized" to snapshot.system.authorized,
                "accessibilityCaptureReady" to snapshot.system.accessibilityCaptureReady,
                "mediaProjectionGranted" to snapshot.system.mediaProjectionGranted,
                "mediaProjectionGranted" to snapshot.system.mediaProjectionGranted
            ),
            "policy" to linkedMapOf("allowed" to snapshot.policy.allowed),
            "effective" to linkedMapOf(
                "usableNow" to snapshot.effective.usableNow,
                "reason" to snapshot.effective.reason
            )
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
