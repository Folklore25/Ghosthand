package com.folklore25.ghosthand

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject

class StateCoordinator(
    context: Context,
    private val runtimeStateProvider: () -> RuntimeState
) {
    private val homeDiagnosticsProvider = HomeDiagnosticsProvider(context.applicationContext)
    private val deviceSnapshotProvider = DeviceSnapshotProvider(context.applicationContext)
    private val foregroundAppProvider = ForegroundAppProvider(context.applicationContext)
    private val permissionSnapshotProvider = PermissionSnapshotProvider(context.applicationContext)
    private val accessibilityStatusProvider = AccessibilityStatusProvider(context.applicationContext)
    private val accessibilityTreeSnapshotProvider = AccessibilityTreeSnapshotProvider(context.applicationContext)
    private val accessibilityNodeFinder = AccessibilityNodeFinder()
    private val accessibilityTapper = AccessibilityTapper()
    private val accessibilityClicker = AccessibilityClicker()
    private val accessibilitySwiper = AccessibilitySwiper()
    private val accessibilityTyper = AccessibilityTyper()
    private val accessibilityScroller = AccessibilityScroller()
    private val rootControlProvider = RootControlProvider()
    private val clipboardProvider = ClipboardProvider(context.applicationContext)
    private val mediaProjectionProvider = MediaProjectionProvider(context.applicationContext)
    private val notificationDispatcher = NotificationDispatcher(context.applicationContext)
    private val rootScreenshotProvider = RootScreenshotProvider()

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
        val rootAvailability = rootControlProvider.availability()
        val accessibilitySnapshot = accessibilityStatusProvider.snapshot(
            isConnected = executionStatus.connected,
            isDispatchCapable = executionStatus.dispatchCapable
        )
        val runtimeUptimeMs = runtimeState.appStartedAtElapsedRealtimeMs?.let {
            (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L)
        }

        return JSONObject()
            .put("runtime", JSONObject()
                .put("ready", isRuntimeReady(runtimeState))
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
            .put("root", JSONObject()
                .put("implemented", rootAvailability.implemented)
                .put("available", rootAvailability.available ?: JSONObject.NULL)
                .put("healthy", rootAvailability.healthy ?: JSONObject.NULL)
                .put("status", rootAvailability.status)
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
            .put("permissions", JSONObject()
                .put("implemented", false)
                .put("usageAccess", permissionSnapshot.usageAccess ?: JSONObject.NULL)
                .put("accessibility", accessibilitySnapshot.enabled)
                .put("notifications", permissionSnapshot.notifications ?: JSONObject.NULL)
                .put("overlay", permissionSnapshot.overlay ?: JSONObject.NULL)
                .put("writeSecureSettings", permissionSnapshot.writeSecureSettings ?: JSONObject.NULL)
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

    fun createFindPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): JSONObject {
        val result = accessibilityNodeFinder.findNodes(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )

        val payload = JSONObject()
            .put("found", result.found)
            .put("matchCount", result.matches.size)
            .put("index", result.selectedIndex)

        val node = result.node
        if (node == null) {
            return payload.put("node", JSONObject.NULL)
        }

        return payload
            .put("node", accessibilityTreeSnapshotProvider.toJson(node))
            .put("text", node.text ?: "")
            .put("desc", node.contentDesc ?: "")
            .put("id", node.resourceId ?: "")
            .put("bounds", node.bounds.toBracketString())
            .put("centerX", node.centerX)
            .put("centerY", node.centerY)
            .put("clickable", node.clickable)
            .put("editable", node.editable)
            .put("scrollable", node.scrollable)
    }

    fun tapPoint(x: Int, y: Int): TapAttemptResult {
        return accessibilityTapper.tapPoint(x, y)
    }

    fun tapNode(nodeId: String): TapAttemptResult {
        return accessibilityTapper.tapNode(nodeId)
    }

    fun swipe(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): SwipeAttemptResult {
        return accessibilitySwiper.swipe(
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            durationMs = durationMs
        )
    }

    fun typeText(text: String): TypeAttemptResult {
        return accessibilityTyper.typeText(text)
    }

    fun launchApp(packageName: String, activity: String?): RootControlResult {
        return rootControlProvider.launchApp(packageName, activity)
    }

    fun stopApp(packageName: String): RootControlResult {
        return rootControlProvider.stopApp(packageName)
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
        return accessibilityClicker.clickNode(nodeId)
    }

    fun clickFirstMatch(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String,
        clickableOnly: Boolean = false,
        index: Int = 0
    ): ClickAttemptResult {
        val found = accessibilityNodeFinder.findNodes(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )

        val nodeId = found.node?.nodeId
            ?: return ClickAttemptResult.failure(
                reason = ClickFailureReason.NODE_NOT_FOUND,
                attemptedPath = "selector_lookup"
            )

        return clickNode(nodeId)
    }

    fun inputText(text: String): TypeAttemptResult {
        return accessibilityTyper.typeText(text)
    }

    fun inputText(
        text: String?,
        clear: Boolean,
        append: Boolean
    ): InputOperationResult {
        val focused = getFocusedNodeResult()
        val previousText = focused.node?.text ?: ""

        val finalText = when {
            clear && text.isNullOrEmpty() -> ""
            append -> previousText + (text ?: "")
            else -> text ?: ""
        }

        val action = when {
            clear && text.isNullOrEmpty() -> "clear"
            append -> "append"
            else -> "set"
        }

        val result = accessibilityTyper.typeText(finalText)
        return InputOperationResult(
            performed = result.performed,
            backendUsed = result.backendUsed,
            failureReason = result.failureReason,
            attemptedPath = result.attemptedPath,
            previousText = previousText,
            finalText = finalText,
            action = action
        )
    }

    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult {
        return accessibilityTyper.setTextOnNode(nodeId, text)
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
        return accessibilityScroller.scrollNode(treeResult.snapshot, nodeId, direction)
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
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return GlobalActionResult(performed = false, attemptedPath = "service_missing")
        // doGlobalAction is defined on the concrete services, not the interface
        return when (service) {
            is GhostCoreAccessibilityService -> {
                val performed = service.doGlobalAction(action)
                GlobalActionResult(performed = performed, attemptedPath = if (performed) "global_action" else "dispatch_failed")
            }
            is GhostAccessibilityService -> {
                val performed = service.doGlobalAction(action)
                GlobalActionResult(performed = performed, attemptedPath = if (performed) "global_action" else "dispatch_failed")
            }
            else -> GlobalActionResult(performed = false, attemptedPath = "unknown_service_type")
        }
    }

    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance() ?: return false
        return service.performLongPressGesture(x, y, durationMs)
    }

    fun performGesture(strokes: List<GestureStroke>): Boolean {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance() ?: return false
        return service.performGesture(strokes)
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

        val rootCapture = rootScreenshotProvider.captureScreenshot()
        if (rootCapture.available) {
            return rootCapture
        }

        return if (projectionCapture.attemptedPath != "projection_missing") {
            projectionCapture
        } else if (serviceCapture.attemptedPath != "service_disconnected") {
            serviceCapture
        } else {
            rootCapture
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
                    return WaitConditionResult(
                        satisfied = true,
                        node = found.node,
                        elapsedMs = System.currentTimeMillis() - startTime,
                        polledCount = 0, // approximate
                        attemptedPath = "condition_met"
                    )
                }
            }
            val remaining = deadline - System.currentTimeMillis()
            if (remaining > 0) {
                Thread.sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        return WaitConditionResult(
            satisfied = false,
            node = null,
            elapsedMs = timeoutMs,
            polledCount = 0,
            attemptedPath = "timeout"
        )
    }

    fun waitForUiChange(timeoutMs: Long, intervalMs: Long): WaitUiChangeResult {
        val initialTree = getTreeSnapshotResult().snapshot
        val initialForeground = foregroundAppProvider.snapshot()
        val initialToken = initialTree?.snapshotToken
        val startTime = System.currentTimeMillis()
        val deadline = startTime + timeoutMs.coerceAtLeast(0L)

        while (System.currentTimeMillis() < deadline) {
            val currentTree = getTreeSnapshotResult().snapshot
            val currentForeground = foregroundAppProvider.snapshot()
            val currentToken = currentTree?.snapshotToken

            val foregroundChanged =
                currentForeground.packageName != initialForeground.packageName ||
                    currentForeground.activity != initialForeground.activity

            val treeChanged =
                currentToken != null &&
                    initialToken != null &&
                    currentToken != initialToken

            if (foregroundChanged || treeChanged) {
                return WaitUiChangeResult(
                    changed = true,
                    elapsedMs = System.currentTimeMillis() - startTime,
                    snapshotToken = currentToken ?: initialToken,
                    packageName = currentForeground.packageName,
                    activity = currentForeground.activity
                )
            }
            val remaining = deadline - System.currentTimeMillis()
            if (remaining > 0) {
                Thread.sleep(intervalMs.coerceAtLeast(50L).coerceAtMost(remaining))
            }
        }

        return WaitUiChangeResult(
            changed = false,
            elapsedMs = timeoutMs,
            snapshotToken = initialToken,
            packageName = initialForeground.packageName,
            activity = initialForeground.activity
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
        val node: FlatAccessibilityNode?,
        val elapsedMs: Long,
        val polledCount: Int,
        val attemptedPath: String
    )

    data class WaitUiChangeResult(
        val changed: Boolean,
        val elapsedMs: Long,
        val snapshotToken: String?,
        val packageName: String?,
        val activity: String?
    )
}

data class InputOperationResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: TypeFailureReason?,
    val attemptedPath: String,
    val previousText: String,
    val finalText: String,
    val action: String
)

data class ScrollBatchResult(
    val performed: Boolean,
    val performedCount: Int,
    val failureReason: ScrollFailureReason?,
    val attemptedPath: String
)
