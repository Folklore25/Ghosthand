/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.accessibility

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

import android.util.Log

class AccessibilityClicker {
    fun clickNode(nodeId: String): ClickAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return ClickAttemptResult.failure(
                reason = ClickFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val clickResult = service.performNodeClick(nodeId)

        return when {
            !clickResult.nodeFound -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=false reason=node_not_found")
                ClickAttemptResult.failure(
                    reason = when (clickResult.attemptedPath) {
                        "root_unavailable" -> ClickFailureReason.ACCESSIBILITY_UNAVAILABLE
                        else -> ClickFailureReason.NODE_NOT_FOUND
                    },
                    attemptedPath = clickResult.attemptedPath
                )
            }
            clickResult.performed -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=true")
                ClickAttemptResult.success(attemptedPath = clickResult.attemptedPath)
            }
            else -> {
                Log.i(LOG_TAG, "event=click_node nodeId=$nodeId path=${clickResult.attemptedPath} success=false reason=action_failed")
                ClickAttemptResult.failure(
                    reason = ClickFailureReason.ACTION_FAILED,
                    attemptedPath = clickResult.attemptedPath
                )
            }
        }
    }

    private companion object {
        const val LOG_TAG = "GhostClick"
    }
}

data class ClickAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: ClickFailureReason?,
    val attemptedPath: String,
    val selectorResolution: ClickSelectorResolution? = null,
    val selectorMissHint: FindMissHint? = null,
    val effect: ActionEffectObservation? = null
) {
    companion object {
        fun success(
            attemptedPath: String,
            selectorResolution: ClickSelectorResolution? = null,
            effect: ActionEffectObservation? = null
        ): ClickAttemptResult = ClickAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath,
            selectorResolution = selectorResolution,
            effect = effect
        )

        fun failure(
            reason: ClickFailureReason,
            attemptedPath: String,
            selectorResolution: ClickSelectorResolution? = null,
            selectorMissHint: FindMissHint? = null,
            effect: ActionEffectObservation? = null
        ): ClickAttemptResult =
            ClickAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath,
                selectorResolution = selectorResolution,
                selectorMissHint = selectorMissHint,
                effect = effect
            )
    }
}

enum class ClickFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NODE_NOT_FOUND,
    ACTION_FAILED
}
