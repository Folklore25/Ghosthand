/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.folklore25.ghosthand.interaction.execution.GestureStroke
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun <T> runOnMainThreadAndAwait(
    mainHandler: Handler,
    timeoutMs: Long,
    fallback: T,
    action: () -> T
): T {
    var result = fallback
    val latch = CountDownLatch(1)
    mainHandler.post {
        result = action()
        latch.countDown()
    }
    latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    return result
}

internal fun buildSingleStrokeGesture(
    x: Int,
    y: Int,
    durationMs: Long
): GestureDescription {
    return GestureDescription.Builder()
        .addStroke(
            GestureDescription.StrokeDescription(
                Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                0L,
                durationMs
            )
        )
        .build()
}

internal fun buildGesture(strokes: List<GestureStroke>): GestureDescription? {
    if (strokes.isEmpty()) return null

    val gestureBuilder = GestureDescription.Builder()
    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue

        val path = Path()
        val points = stroke.points
        path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
        for (index in 1 until points.size) {
            path.lineTo(points[index].x.toFloat(), points[index].y.toFloat())
        }

        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(path, 0L, stroke.durationMs)
        )
    }

    return gestureBuilder.build().takeIf { it.strokeCount > 0 }
}

internal fun currentActiveRoot(
    preferredPackage: String?,
    windows: List<AccessibilityWindowInfo>,
    rootInActiveWindow: AccessibilityNodeInfo?
): AccessibilityNodeInfo? {
    selectWindowRoot(
        preferredPackage = preferredPackage,
        windows = windows,
        requireApplicationWindow = true,
        requireActive = true
    )?.let { return it }

    selectWindowRoot(
        preferredPackage = preferredPackage,
        windows = windows,
        requireApplicationWindow = true,
        requireFocused = true
    )?.let { return it }

    selectWindowRoot(
        preferredPackage = preferredPackage,
        windows = windows,
        requireApplicationWindow = true
    )?.let { return it }

    rootInActiveWindow?.let { return it }

    selectWindowRoot(preferredPackage = preferredPackage, windows = windows, requireActive = true)?.let { return it }
    selectWindowRoot(preferredPackage = preferredPackage, windows = windows, requireFocused = true)?.let { return it }
    return selectWindowRoot(preferredPackage = preferredPackage, windows = windows)
}

private fun selectWindowRoot(
    preferredPackage: String? = null,
    windows: List<AccessibilityWindowInfo>,
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
