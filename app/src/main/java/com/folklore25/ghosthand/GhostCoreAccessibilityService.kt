/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Build
import android.os.Bundle
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityWindowInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GhostCoreAccessibilityService : AccessibilityService(), GhostAccessibilityExecutionCore {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val foregroundAppProvider by lazy { ForegroundAppProvider(applicationContext) }

    @Volatile
    private var isConnectedForDispatch = false

    override fun onCreate() {
        super.onCreate()
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(this)
        Log.i(
            LOG_TAG,
            "event=service_created service=core instanceId=${instanceId()}"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnectedForDispatch = true
        GhostAccessibilityExecutionCoreRegistry.registerPrimary(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_connected service=core instanceId=${instanceId()} connectionId=${currentConnectionIdForDispatch()} frameworkConnectionAvailable=${frameworkConnectionAvailable()} connectedForDispatch=$isConnectedForDispatch"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Minimal execution core.
    }

    override fun onInterrupt() {
        isConnectedForDispatch = false
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_interrupted service=core instanceId=${instanceId()} connectedForDispatch=$isConnectedForDispatch"
        )
    }

    override fun onDestroy() {
        isConnectedForDispatch = false
        GhostAccessibilityExecutionCoreRegistry.unregister(this)
        RuntimeStateStore.refreshAccessibilityStatus(this)
        Log.i(
            LOG_TAG,
            "event=service_destroyed service=core instanceId=${instanceId()} connectedForDispatch=$isConnectedForDispatch"
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
        if (connectionId != -1) {
            return try {
                val clientClass = Class.forName("android.view.accessibility.AccessibilityInteractionClient")
                val getConnectionMethod =
                    clientClass.getDeclaredMethod("getConnection", Int::class.javaPrimitiveType)
                getConnectionMethod.isAccessible = true
                getConnectionMethod.invoke(null, connectionId) != null
            } catch (error: Exception) {
                Log.w(
                    LOG_TAG,
                    "event=framework_connection_check_failed service=core instanceId=${instanceId()} connectionId=$connectionId message=${error.message}"
                )
                false
            }
        }

        return hasFrameworkWindowAccess()
    }

    private fun hasFrameworkWindowAccess(): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return currentActiveRootOnMainThread() != null || windows.isNotEmpty()
        }

        var hasWindowAccess = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            hasWindowAccess = currentActiveRootOnMainThread() != null || windows.isNotEmpty()
            latch.countDown()
        }
        latch.await(ROOT_SNAPSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return hasWindowAccess
    }

    override fun currentConnectionIdForDispatch(): Int {
        resolveConnectionIdMethod()?.let { method ->
            try {
                return (method.invoke(this) as? Int) ?: -1
            } catch (error: Exception) {
                Log.w(
                    LOG_TAG,
                    "event=connection_id_method_invoke_failed service=core instanceId=${instanceId()} method=${method.name} message=${error.message}"
                )
            }
        }

        resolveConnectionIdField()?.let { field ->
            try {
                return field.getInt(this)
            } catch (error: Exception) {
                Log.w(
                    LOG_TAG,
                    "event=connection_id_field_read_failed service=core instanceId=${instanceId()} field=${field.name} message=${error.message}"
                )
            }
        }

        logConnectionIdLookupUnavailable()
        return -1
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

    private fun resolveConnectionIdMethod(): Method? {
        try {
            val getConnectionIdMethod =
                AccessibilityService::class.java.getDeclaredMethod("getConnectionId")
            getConnectionIdMethod.isAccessible = true
            return getConnectionIdMethod
        } catch (_: Exception) {
            // Fall through to runtime scanning for vendor-specific variants.
        }

        classHierarchy().forEach { klass ->
            klass.declaredMethods
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.returnType == Int::class.javaPrimitiveType &&
                        method.name.contains("connection", ignoreCase = true)
                }
                ?.let { method ->
                    method.isAccessible = true
                    Log.i(
                        LOG_TAG,
                        "event=connection_id_method_resolved service=core instanceId=${instanceId()} owner=${klass.name} method=${method.name}"
                    )
                    return method
                }
        }

        return null
    }

    private fun resolveConnectionIdField(): Field? {
        classHierarchy().forEach { klass ->
            klass.declaredFields
                .firstOrNull { field ->
                    field.type == Int::class.javaPrimitiveType &&
                        field.name.contains("connection", ignoreCase = true)
                }
                ?.let { field ->
                    field.isAccessible = true
                    Log.i(
                        LOG_TAG,
                        "event=connection_id_field_resolved service=core instanceId=${instanceId()} owner=${klass.name} field=${field.name}"
                    )
                    return field
                }
        }

        return null
    }

    private fun classHierarchy(): Sequence<Class<*>> {
        return generateSequence<Class<*>>(AccessibilityService::class.java) { it.superclass }
    }

    private fun logConnectionIdLookupUnavailable() {
        val candidateMethods = classHierarchy()
            .flatMap { klass ->
                klass.declaredMethods
                    .filter { it.name.contains("connection", ignoreCase = true) }
                    .map { "${klass.simpleName}.${it.name}" }
                    .asSequence()
            }
            .toList()
        val candidateFields = classHierarchy()
            .flatMap { klass ->
                klass.declaredFields
                    .filter { it.name.contains("connection", ignoreCase = true) }
                    .map { "${klass.simpleName}.${it.name}" }
                    .asSequence()
            }
            .toList()
        Log.w(
            LOG_TAG,
            "event=connection_id_lookup_unavailable service=core instanceId=${instanceId()} candidateMethods=$candidateMethods candidateFields=$candidateFields"
        )
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

    override fun takeScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "not_supported"
            )
        }

        if (!dispatchConnectionActive()) {
            return ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "service_disconnected"
            )
        }

        var result = ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = "png",
            width = 0,
            height = 0,
            attemptedPath = "not_dispatched"
        )
        val latch = CountDownLatch(1)

        mainHandler.post {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        val hardwareBitmap = android.graphics.Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        if (hardwareBitmap == null) {
                            result = ScreenshotDispatchResult(
                                available = false,
                                base64 = null,
                                format = "png",
                                width = 0,
                                height = 0,
                                attemptedPath = "bitmap_wrap_failed"
                            )
                            screenshot.hardwareBuffer.close()
                            latch.countDown()
                            return
                        }

                        val finalBitmap = try {
                            val targetWidth = if (width > 0) width else hardwareBitmap.width
                            val targetHeight = if (height > 0) height else hardwareBitmap.height
                            if (targetWidth != hardwareBitmap.width || targetHeight != hardwareBitmap.height) {
                                android.graphics.Bitmap.createScaledBitmap(
                                    hardwareBitmap,
                                    targetWidth,
                                    targetHeight,
                                    true
                                )
                            } else {
                                hardwareBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            }
                        } catch (_: Exception) {
                            null
                        }

                        if (finalBitmap != null) {
                            try {
                                val baos = java.io.ByteArrayOutputStream()
                                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, baos)
                                val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                                result = ScreenshotDispatchResult(
                                    available = true,
                                    base64 = b64,
                                    format = "png",
                                    width = finalBitmap.width,
                                    height = finalBitmap.height,
                                    attemptedPath = "accessibility_screenshot"
                                )
                            } finally {
                                finalBitmap.recycle()
                            }
                        } else {
                            result = ScreenshotDispatchResult(
                                available = false,
                                base64 = null,
                                format = "png",
                                width = 0,
                                height = 0,
                                attemptedPath = "bitmap_prepare_failed"
                            )
                        }
                        hardwareBitmap.recycle()
                        screenshot.hardwareBuffer.close()
                        latch.countDown()
                    }

                    override fun onFailure(errorCode: Int) {
                        result = ScreenshotDispatchResult(
                            available = false,
                            base64 = null,
                            format = "png",
                            width = 0,
                            height = 0,
                            attemptedPath = "screenshot_failure_$errorCode"
                        )
                        latch.countDown()
                    }
                }
                )
            } catch (securityError: SecurityException) {
                result = ScreenshotDispatchResult(
                    available = false,
                    base64 = null,
                    format = "png",
                    width = 0,
                    height = 0,
                    attemptedPath = "screenshot_capability_unavailable"
                )
                latch.countDown()
            } catch (_: Exception) {
                result = ScreenshotDispatchResult(
                    available = false,
                    base64 = null,
                    format = "png",
                    width = 0,
                    height = 0,
                    attemptedPath = "screenshot_exception"
                )
                latch.countDown()
            }
        }

        latch.await((SCREENSHOT_TIMEOUT_MS + ACTION_TIMEOUT_BUFFER_MS).coerceAtLeast(ACTION_TIMEOUT_MS), TimeUnit.MILLISECONDS)
        return result
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
        findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf(::isEditableTarget)
            ?.let { return it }

        findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?.takeIf(::isEditableTarget)
            ?.let { return it }

        return currentActiveRootOnMainThread()?.let(::findFocusedEditableNode)
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isEditableTarget(node) && (node.isFocused || node.isAccessibilityFocused)) {
            return node
        }

        for (childIndex in 0 until node.childCount) {
            val child = node.getChild(childIndex) ?: continue
            val match = findFocusedEditableNode(child)
            if (match != null) {
                return match
            }
        }
        return null
    }

    private fun isEditableTarget(node: AccessibilityNodeInfo): Boolean {
        return node.isEditable && node.isEnabled
    }

    private fun findClickableParent(
        node: AccessibilityNodeInfo,
        maxDepth: Int
    ): AccessibilityNodeInfo? {
        var depth = 0
        var current = node.parent
        while (current != null && depth < maxDepth) {
            if (current.isClickable && current.isEnabled) {
                return current
            }
            current = current.parent
            depth += 1
        }
        return null
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
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    stroke.durationMs
                )
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
        // Estimate timeout: duration + buffer per stroke
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
        const val LOG_TAG = "GhostAccessibilityCore"
        const val MAX_CLICKABLE_PARENT_DEPTH = 5
        const val ROOT_SNAPSHOT_TIMEOUT_MS = 500L
        const val ACTION_TIMEOUT_MS = 1500L
        const val ACTION_TIMEOUT_BUFFER_MS = 500L
        const val TAP_DURATION_MS = 50L
        const val SCREENSHOT_TIMEOUT_MS = 3000L
    }
}

private fun GhostCoreAccessibilityService.instanceId(): Int = System.identityHashCode(this)
