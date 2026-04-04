/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.execution

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.payload.InputKey

internal interface GhosthandInteractionPlane {
    fun tapPoint(x: Int, y: Int): TapAttemptResult
    fun tapNode(nodeId: String): TapAttemptResult
    fun clickNode(nodeId: String): ClickAttemptResult
    fun swipe(fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long): SwipeAttemptResult
    fun typeText(text: String): TypeAttemptResult
    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult
    fun dispatchKey(key: InputKey): InputKeyAttemptResult
    fun scrollNode(snapshot: AccessibilityTreeSnapshot, nodeId: String, direction: String): ScrollAttemptResult
    fun performGlobalAction(action: Int): GlobalActionResult
    fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean
    fun performGesture(strokes: List<GestureStroke>): Boolean
}

internal class AccessibilityInteractionPlane(
    private val tapper: AccessibilityTapper = AccessibilityTapper(),
    private val clicker: AccessibilityClicker = AccessibilityClicker(),
    private val swiper: AccessibilitySwiper = AccessibilitySwiper(),
    private val typer: AccessibilityTyper = AccessibilityTyper(),
    private val scroller: AccessibilityScroller = AccessibilityScroller()
) : GhosthandInteractionPlane {
    override fun tapPoint(x: Int, y: Int): TapAttemptResult = tapper.tapPoint(x, y)

    override fun tapNode(nodeId: String): TapAttemptResult = tapper.tapNode(nodeId)

    override fun clickNode(nodeId: String): ClickAttemptResult = clicker.clickNode(nodeId)

    override fun swipe(fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long): SwipeAttemptResult =
        swiper.swipe(fromX = fromX, fromY = fromY, toX = toX, toY = toY, durationMs = durationMs)

    override fun typeText(text: String): TypeAttemptResult = typer.typeText(text)

    override fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult = typer.setTextOnNode(nodeId, text)

    override fun dispatchKey(key: InputKey): InputKeyAttemptResult = typer.dispatchKey(key)

    override fun scrollNode(
        snapshot: AccessibilityTreeSnapshot,
        nodeId: String,
        direction: String
    ): ScrollAttemptResult = scroller.scrollNode(snapshot, nodeId, direction)

    override fun performGlobalAction(action: Int): GlobalActionResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return GlobalActionResult(performed = false, attemptedPath = "service_missing")
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

    override fun performLongPressGesture(x: Int, y: Int, durationMs: Long): Boolean {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance() ?: return false
        return service.performLongPressGesture(x, y, durationMs)
    }

    override fun performGesture(strokes: List<GestureStroke>): Boolean {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance() ?: return false
        return service.performGesture(strokes)
    }
}
