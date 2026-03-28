/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

interface GhostAccessibilityExecutionCore {
    fun <T> withActiveWindowRoot(block: (AccessibilityNodeInfo) -> T): T?
    fun dispatchConnectionActive(): Boolean
    fun frameworkConnectionAvailable(): Boolean
    fun currentConnectionIdForDispatch(): Int
    fun performNodeClick(nodeId: String): NodeClickDispatchResult
    fun performSetText(text: CharSequence): TextInputDispatchResult
    fun performTapGesture(x: Int, y: Int): Boolean
    fun performSwipeGesture(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): Boolean
    fun performSwipeGestureDiagnostic(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        durationMs: Long
    ): SwipeGestureDispatchDiagnostic

    fun takeScreenshot(width: Int, height: Int): ScreenshotDispatchResult

    fun setTextOnNode(nodeId: String, text: CharSequence): NodeTextDispatchResult

    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean

    fun performGesture(strokes: List<GestureStroke>): Boolean
}

data class NodeClickDispatchResult(
    val nodeFound: Boolean,
    val performed: Boolean,
    val attemptedPath: String
)

data class TextInputDispatchResult(
    val targetFound: Boolean,
    val performed: Boolean,
    val attemptedPath: String
)

data class GhostAccessibilityExecutionStatus(
    val connected: Boolean,
    val dispatchCapable: Boolean
)

data class SwipeGestureDispatchDiagnostic(
    val dispatched: Boolean,
    val callbackResult: String,
    val completed: Boolean
)

data class ScreenshotDispatchResult(
    val available: Boolean,
    val base64: String?,
    val format: String,
    val width: Int,
    val height: Int,
    val attemptedPath: String
)

data class NodeTextDispatchResult(
    val nodeFound: Boolean,
    val performed: Boolean,
    val attemptedPath: String
)

data class GestureStroke(
    val points: List<GesturePoint>,
    val durationMs: Long
)

data class GesturePoint(
    val x: Int,
    val y: Int
)

data class GlobalActionResult(
    val performed: Boolean,
    val attemptedPath: String
)

object GhostAccessibilityExecutionCoreRegistry {
    @Volatile
    private var primaryCoreReference: WeakReference<GhostAccessibilityExecutionCore>? = null

    @Volatile
    private var legacyCoreReference: WeakReference<GhostAccessibilityExecutionCore>? = null

    fun registerPrimary(core: GhostAccessibilityExecutionCore) {
        primaryCoreReference = WeakReference(core)
    }

    fun registerLegacy(core: GhostAccessibilityExecutionCore) {
        legacyCoreReference = WeakReference(core)
    }

    fun unregister(core: GhostAccessibilityExecutionCore) {
        if (primaryCoreReference?.get() === core) {
            primaryCoreReference = null
        }
        if (legacyCoreReference?.get() === core) {
            legacyCoreReference = null
        }
    }

    fun primaryInstance(): GhostAccessibilityExecutionCore? = primaryCoreReference?.get()

    fun legacyInstance(): GhostAccessibilityExecutionCore? = legacyCoreReference?.get()

    fun currentInstance(): GhostAccessibilityExecutionCore? {
        return connectedCore(primaryCoreReference) ?: primaryCoreReference?.get()
    }

    fun currentStatus(): GhostAccessibilityExecutionStatus {
        val currentCore = primaryInstance()
        val connected = currentCore?.dispatchConnectionActive() == true
        val dispatchCapable = connected && currentCore.frameworkConnectionAvailable()
        return GhostAccessibilityExecutionStatus(
            connected = connected,
            dispatchCapable = dispatchCapable
        )
    }

    private fun connectedCore(
        reference: WeakReference<GhostAccessibilityExecutionCore>?
    ): GhostAccessibilityExecutionCore? {
        return reference?.get()?.takeIf { it.dispatchConnectionActive() }
    }
}

object GhostAccessibilityServiceComponents {
    fun primaryComponentName(context: Context): ComponentName {
        return ComponentName(context, GhostCoreAccessibilityService::class.java)
    }

    fun legacyComponentName(context: Context): ComponentName {
        return ComponentName(context, GhostAccessibilityService::class.java)
    }

    fun probeComponentName(context: Context): ComponentName {
        return ComponentName(context, GhostProbeAccessibilityService::class.java)
    }

    fun primaryAcceptedEnabledNames(context: Context): Set<String> {
        return flattenedNames(primaryComponentName(context), context)
    }

    fun managedComponentNames(context: Context): Set<String> {
        return listOf(
            primaryComponentName(context),
            legacyComponentName(context),
            probeComponentName(context)
        ).flatMap { flattenedNames(it, context) }.toSet()
    }

    private fun flattenedNames(componentName: ComponentName, context: Context): Set<String> {
        return setOf(
            componentName.flattenToString(),
            componentName.flattenToShortString(),
            "${context.packageName}/${componentName.shortClassName}"
        )
    }
}
