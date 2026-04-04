/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.accessibility

import com.folklore25.ghosthand.screen.find.AccessibilityNodeLocator
import com.folklore25.ghosthand.state.device.ForegroundAppProvider
import com.folklore25.ghosthand.interaction.execution.GestureStroke
import com.folklore25.ghosthand.interaction.execution.GhostAccessibilityExecutionCore
import com.folklore25.ghosthand.interaction.execution.GhostAccessibilityExecutionCoreRegistry
import com.folklore25.ghosthand.interaction.execution.KeyInputDispatchResult
import com.folklore25.ghosthand.interaction.execution.NodeClickDispatchResult
import com.folklore25.ghosthand.screen.find.NodeResolutionResult
import com.folklore25.ghosthand.interaction.execution.NodeTextDispatchResult
import com.folklore25.ghosthand.state.runtime.RuntimeStateStore
import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.SwipeGestureDispatchDiagnostic
import com.folklore25.ghosthand.interaction.execution.TextInputDispatchResult

import com.folklore25.ghosthand.R

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GhostAccessibilityService : AccessibilityService(), GhostAccessibilityExecutionCore {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val foregroundAppProvider by lazy { ForegroundAppProvider(applicationContext) }

    @Volatile
    private var isConnectedForDispatch = false

    override fun onCreate() {
        super.onCreate()
        isConnectedForDispatch = false
        GhostAccessibilityExecutionCoreRegistry.registerLegacy(this)
        Log.i(
            LOG_TAG,
            "event=service_created service=legacy instanceId=${instanceId()}"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        isConnectedForDispatch = true
        GhostAccessibilityExecutionCoreRegistry.registerLegacy(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_connected service=legacy instanceId=${instanceId()} connectionId=${currentConnectionIdForDispatch()} frameworkConnectionAvailable=${frameworkConnectionAvailable()} connectedForDispatch=$isConnectedForDispatch"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // P2 Task 1 intentionally exposes status only.
    }

    override fun onInterrupt() {
        isConnectedForDispatch = false
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_interrupted service=legacy instanceId=${instanceId()} connectedForDispatch=$isConnectedForDispatch"
        )
    }

    override fun onDestroy() {
        isConnectedForDispatch = false
        GhostAccessibilityExecutionCoreRegistry.unregister(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_destroyed service=legacy instanceId=${instanceId()} connectedForDispatch=$isConnectedForDispatch"
        )
        super.onDestroy()
    }

    override fun <T> withActiveWindowRoot(block: (AccessibilityNodeInfo) -> T): T? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return currentActiveRootOnMainThread()?.let(block)
        }

        var result: T? = null
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = currentActiveRootOnMainThread()?.let(block)
            latch.countDown()
        }
        latch.await(ROOT_SNAPSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    override fun dispatchConnectionActive(): Boolean = isConnectedForDispatch

    override fun frameworkConnectionAvailable(): Boolean {
        val connectionId = currentConnectionIdForDispatch()
        if (connectionId == -1) {
            return false
        }

        return try {
            val clientClass = Class.forName("android.view.accessibility.AccessibilityInteractionClient")
            val getConnectionMethod = clientClass.getDeclaredMethod("getConnection", Int::class.javaPrimitiveType)
            getConnectionMethod.isAccessible = true
            getConnectionMethod.invoke(null, connectionId) != null
        } catch (_: Exception) {
            false
        }
    }

    override fun currentConnectionIdForDispatch(): Int {
        return try {
            val field = AccessibilityService::class.java.getDeclaredField("mConnectionId")
            field.isAccessible = true
            field.getInt(this)
        } catch (_: Exception) {
            -1
        }
    }

    override fun performSetText(text: CharSequence): TextInputDispatchResult {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return performSetTextInternal(text)
        }

        var result = TextInputDispatchResult(
            targetFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = performSetTextInternal(text)
            latch.countDown()
        }
        latch.await(ACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    override fun performImeEnterAction(): KeyInputDispatchResult {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return performImeEnterActionInternal()
        }

        var result = KeyInputDispatchResult(
            targetFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = performImeEnterActionInternal()
            latch.countDown()
        }
        latch.await(ACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    override fun performNodeClick(nodeId: String): NodeClickDispatchResult {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return performNodeClickInternal(nodeId)
        }

        var result = NodeClickDispatchResult(
            nodeFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = performNodeClickInternal(nodeId)
            latch.countDown()
        }
        latch.await(ROOT_SNAPSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    override fun performTapGesture(x: Int, y: Int): Boolean {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                    0L,
                    TAP_DURATION_MS
                )
            )
            .build()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return dispatchGesture(gesture, null, null)
        }

        var performed = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            performed = dispatchGesture(gesture, null, null)
            latch.countDown()
        }
        latch.await(ACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return performed
    }

    override fun performSwipeGesture(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): Boolean {
        return performSwipeGestureDiagnostic(
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            durationMs = durationMs
        ).dispatched
    }

    override fun performSwipeGestureDiagnostic(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): SwipeGestureDispatchDiagnostic {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply {
                        moveTo(fromX.toFloat(), fromY.toFloat())
                        lineTo(toX.toFloat(), toY.toFloat())
                    },
                    0L,
                    durationMs
                )
            )
            .build()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            val dispatched = dispatchGesture(gesture, null, null)
            return SwipeGestureDispatchDiagnostic(
                dispatched = dispatched,
                callbackResult = "not_observed",
                completed = false
            )
        }

        var result = SwipeGestureDispatchDiagnostic(
            dispatched = false,
            callbackResult = "not_observed",
            completed = false
        )
        val latch = CountDownLatch(1)
        mainHandler.post {
            val dispatched = dispatchGesture(gesture, null, null)
            result = SwipeGestureDispatchDiagnostic(
                dispatched = dispatched,
                callbackResult = "not_observed",
                completed = false
            )
            latch.countDown()
        }
        latch.await((durationMs + ACTION_TIMEOUT_BUFFER_MS).coerceAtLeast(ACTION_TIMEOUT_MS), TimeUnit.MILLISECONDS)
        return result
    }

    override fun takeScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        return ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = "png",
            width = 0,
            height = 0,
            attemptedPath = "not_supported_on_legacy_service"
        )
    }

    override fun setTextOnNode(nodeId: String, text: CharSequence): NodeTextDispatchResult {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return setTextOnNodeInternal(nodeId, text)
        }

        var result = NodeTextDispatchResult(
            nodeFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )
        val latch = CountDownLatch(1)
        mainHandler.post {
            result = setTextOnNodeInternal(nodeId, text)
            latch.countDown()
        }
        latch.await(ACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return result
    }

    private fun setTextOnNodeInternal(nodeId: String, text: CharSequence): NodeTextDispatchResult {
        val rootNode = currentActiveRootOnMainThread() ?: return NodeTextDispatchResult(
            nodeFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )

        val targetNode = when (val resolved = AccessibilityNodeLocator.resolveAgainstRoot(nodeId, rootNode)) {
            NodeResolutionResult.InvalidId -> return NodeTextDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "invalid_node_id"
            )
            NodeResolutionResult.StaleSnapshot -> return NodeTextDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "stale_snapshot"
            )
            NodeResolutionResult.NotFound -> return NodeTextDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "node_lookup"
            )
            is NodeResolutionResult.Found -> resolved.node
        }

        if (!targetNode.isEditable || !targetNode.isEnabled) {
            return NodeTextDispatchResult(
                nodeFound = true,
                performed = false,
                attemptedPath = "node_not_editable"
            )
        }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        return NodeTextDispatchResult(
            nodeFound = true,
            performed = performed,
            attemptedPath = "node_set_text"
        )
    }

    private fun performNodeClickInternal(nodeId: String): NodeClickDispatchResult {
        val rootNode = currentActiveRootOnMainThread() ?: return NodeClickDispatchResult(
            nodeFound = false,
            performed = false,
            attemptedPath = "root_unavailable"
        )

        val targetNode = when (val resolved = AccessibilityNodeLocator.resolveAgainstRoot(nodeId, rootNode)) {
            NodeResolutionResult.InvalidId -> return NodeClickDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "invalid_node_id"
            )
            NodeResolutionResult.StaleSnapshot -> return NodeClickDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "stale_snapshot"
            )
            NodeResolutionResult.NotFound -> return NodeClickDispatchResult(
                nodeFound = false,
                performed = false,
                attemptedPath = "node_lookup"
            )
            is NodeResolutionResult.Found -> resolved.node
        }

        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return NodeClickDispatchResult(
                nodeFound = true,
                performed = true,
                attemptedPath = "node_click"
            )
        }

        val clickableParent = findClickableParent(targetNode, MAX_CLICKABLE_PARENT_DEPTH)
            ?: return NodeClickDispatchResult(
                nodeFound = true,
                performed = false,
                attemptedPath = "clickable_parent_missing"
            )

        val performed = clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return NodeClickDispatchResult(
            nodeFound = true,
            performed = performed,
            attemptedPath = "clickable_parent_click"
        )
    }

    private fun performSetTextInternal(text: CharSequence): TextInputDispatchResult {
        val targetNode = findEditableInputFocusNode()
            ?: return TextInputDispatchResult(
                targetFound = false,
                performed = false,
                attemptedPath = "focused_editable_missing"
            )

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        return TextInputDispatchResult(
            targetFound = true,
            performed = performed,
            attemptedPath = "focused_set_text"
        )
    }

    private fun performImeEnterActionInternal(): KeyInputDispatchResult {
        val targetNode = findEditableInputFocusNode()
            ?: return KeyInputDispatchResult(
                targetFound = false,
                performed = false,
                attemptedPath = "focused_editable_missing"
            )

        val performed = targetNode.performAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
        )
        return KeyInputDispatchResult(
            targetFound = true,
            performed = performed,
            attemptedPath = "focused_ime_enter"
        )
    }

    private fun findEditableInputFocusNode(): AccessibilityNodeInfo? {
        return findEditableInputFocusNode(this, ::currentActiveRootOnMainThread)
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return com.folklore25.ghosthand.service.accessibility.findFocusedEditableNode(node)
    }

    private fun isEditableTarget(node: AccessibilityNodeInfo): Boolean {
        return com.folklore25.ghosthand.service.accessibility.isEditableTarget(node)
    }

    private fun findClickableParent(
        node: AccessibilityNodeInfo,
        maxDepth: Int
    ): AccessibilityNodeInfo? {
        return com.folklore25.ghosthand.service.accessibility.findClickableParent(node, maxDepth)
    }

    // Uses AccessibilityService.performGlobalAction directly — performGlobalAction is final
    fun doGlobalAction(action: Int): Boolean {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatchGlobalActionDirect(action)
        } else {
            var performed = false
            val latch = CountDownLatch(1)
            mainHandler.post {
                performed = dispatchGlobalActionDirect(action)
                latch.countDown()
            }
            latch.await(ACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            performed
        }
    }

    @Suppress("DEPRECATION")
    private fun dispatchGlobalActionDirect(action: Int): Boolean {
        return try {
            performGlobalAction(action)
        } catch (_: Exception) {
            false
        }
    }

    override fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                    0L,
                    durationMs
                )
            )
            .build()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return dispatchGesture(gesture, null, null)
        }

        var performed = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            performed = dispatchGesture(gesture, null, null)
            latch.countDown()
        }
        latch.await((durationMs + ACTION_TIMEOUT_BUFFER_MS).coerceAtLeast(ACTION_TIMEOUT_MS), TimeUnit.MILLISECONDS)
        return performed
    }

    override fun performGesture(strokes: List<GestureStroke>): Boolean {
        if (strokes.isEmpty()) return false

        val gestureBuilder = GestureDescription.Builder()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val path = Path()
            val pts = stroke.points
            path.moveTo(pts[0].x.toFloat(), pts[0].y.toFloat())
            for (i in 1 until pts.size) {
                path.lineTo(pts[i].x.toFloat(), pts[i].y.toFloat())
            }
            gestureBuilder.addStroke(
                GestureDescription.StrokeDescription(path, 0L, stroke.durationMs)
            )
        }

        val gesture = gestureBuilder.build()
        if (gesture.strokeCount == 0) return false

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return dispatchGesture(gesture, null, null)
        }

        var performed = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            performed = dispatchGesture(gesture, null, null)
            latch.countDown()
        }
        val estimatedTimeout = (strokes.maxOfOrNull { it.durationMs } ?: 0L) + ACTION_TIMEOUT_BUFFER_MS
        latch.await(estimatedTimeout.coerceAtLeast(ACTION_TIMEOUT_MS), TimeUnit.MILLISECONDS)
        return performed
    }

    private fun currentActiveRootOnMainThread(): AccessibilityNodeInfo? {
        val foregroundPackage = foregroundAppProvider.snapshot().packageName

        selectWindowRoot(
            preferredPackage = foregroundPackage,
            requireApplicationWindow = true,
            requireActive = true
        )?.let { return it }

        selectWindowRoot(
            preferredPackage = foregroundPackage,
            requireApplicationWindow = true,
            requireFocused = true
        )?.let { return it }

        selectWindowRoot(
            preferredPackage = foregroundPackage,
            requireApplicationWindow = true
        )?.let { return it }

        rootInActiveWindow?.let { return it }

        selectWindowRoot(preferredPackage = foregroundPackage, requireActive = true)?.let { return it }
        selectWindowRoot(preferredPackage = foregroundPackage, requireFocused = true)?.let { return it }
        return selectWindowRoot(preferredPackage = foregroundPackage)
    }

    private fun selectWindowRoot(
        preferredPackage: String? = null,
        requireApplicationWindow: Boolean = false,
        requireActive: Boolean = false,
        requireFocused: Boolean = false
    ): AccessibilityNodeInfo? {
        return windows
            .asSequence()
            .mapNotNull { window ->
                val root = window.root ?: return@mapNotNull null
                if (requireApplicationWindow && window.type != AccessibilityWindowInfo.TYPE_APPLICATION) {
                    return@mapNotNull null
                }
                if (requireActive && !window.isActive) {
                    return@mapNotNull null
                }
                if (requireFocused && !window.isFocused) {
                    return@mapNotNull null
                }
                WindowRootCandidate(
                    root = root,
                    packageName = root.packageName?.toString(),
                    layer = window.layer,
                    active = window.isActive,
                    focused = window.isFocused
                )
            }
            .sortedWith(
                compareByDescending<WindowRootCandidate> { candidate ->
                    preferredPackage != null && candidate.packageName == preferredPackage
                }
                    .thenByDescending { it.active }
                    .thenByDescending { it.focused }
                    .thenByDescending { it.layer }
            )
            .map { it.root }
            .firstOrNull()
    }

    private data class WindowRootCandidate(
        val root: AccessibilityNodeInfo,
        val packageName: String?,
        val layer: Int,
        val active: Boolean,
        val focused: Boolean
    )

    private companion object {
        const val LOG_TAG = "GhostAccessibility"
        private const val MAX_CLICKABLE_PARENT_DEPTH = 5
        private const val ROOT_SNAPSHOT_TIMEOUT_MS = 500L
        private const val ACTION_TIMEOUT_MS = 1500L
        private const val ACTION_TIMEOUT_BUFFER_MS = 500L
        private const val TAP_DURATION_MS = 50L
    }
}

private fun GhostAccessibilityService.instanceId(): Int = System.identityHashCode(this)
