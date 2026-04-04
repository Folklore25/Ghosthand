/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.observation

import com.folklore25.ghosthand.interaction.execution.ActionEffectObservation
import com.folklore25.ghosthand.screen.read.TreeUnavailableReason

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.screen.read.ScreenStateLegibilityProjector

internal class GhosthandObservationPublisher(
    private val observationLog: GhosthandObservationLog
) {
    @Volatile
    private var lastSurfaceState: ObservationSurfaceState? = null

    fun recordScreenPayload(payload: ScreenReadPayload) {
        val legibility = ScreenStateLegibilityProjector.fromPayload(payload)
        val current = ObservationSurfaceState(
            packageName = payload.packageName,
            activity = payload.activity,
            renderMode = legibility.renderMode.wireValue,
            surfaceReadability = legibility.surfaceReadability.wireValue,
            visualAvailable = legibility.visualAvailable,
            previewAvailable = legibility.previewAvailable,
            source = payload.source,
            suggestedSource = payload.retryHint?.source,
            fallbackReason = payload.retryHint?.reason,
            accessibilityTemporarilyUnavailable = false
        )
        val previous = lastSurfaceState
        lastSurfaceState = current

        if (previous?.packageName != current.packageName && current.packageName != null) {
            observationLog.append(
                type = "foreground_changed",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf("source" to current.source)
            )
        }
        if (previous?.activity != current.activity && current.activity != null) {
            observationLog.append(
                type = "window_changed",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf("source" to current.source)
            )
        }
        if (
            previous == null ||
            previous.renderMode != current.renderMode ||
            previous.surfaceReadability != current.surfaceReadability ||
            previous.visualAvailable != current.visualAvailable ||
            previous.previewAvailable != current.previewAvailable
        ) {
            observationLog.append(
                type = "screen_readability_changed",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf(
                    "source" to current.source,
                    "renderMode" to current.renderMode,
                    "surfaceReadability" to current.surfaceReadability,
                    "visualAvailable" to current.visualAvailable,
                    "previewAvailable" to current.previewAvailable
                )
            )
        }
        if (current.previewAvailable == true && previous?.previewAvailable != true) {
            observationLog.append(
                type = "preview_became_available",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf("source" to current.source)
            )
        }
        if (
            current.suggestedSource != null &&
            (previous?.suggestedSource != current.suggestedSource || previous.fallbackReason != current.fallbackReason)
        ) {
            observationLog.append(
                type = "fallback_hint_raised",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf(
                    "suggestedSource" to current.suggestedSource,
                    "fallbackReason" to current.fallbackReason,
                    "source" to current.source
                )
            )
        }
        if (previous?.accessibilityTemporarilyUnavailable == true) {
            observationLog.append(
                type = "modal_transition_ended",
                packageName = current.packageName,
                activity = current.activity,
                evidence = mapOf(
                    "source" to current.source,
                    "renderMode" to current.renderMode,
                    "surfaceReadability" to current.surfaceReadability
                )
            )
        }
    }

    fun recordAccessibilityTemporarilyUnavailable(
        reason: TreeUnavailableReason?,
        requestedSource: ScreenReadMode
    ) {
        if (requestedSource != ScreenReadMode.ACCESSIBILITY) return

        val previous = lastSurfaceState
        val nextState = (previous ?: ObservationSurfaceState()).copy(
            accessibilityTemporarilyUnavailable = true
        )
        lastSurfaceState = nextState

        if (previous?.accessibilityTemporarilyUnavailable != true) {
            observationLog.append(
                type = "modal_transition_started",
                packageName = previous?.packageName,
                activity = previous?.activity,
                evidence = mapOf(
                    "requestedSource" to requestedSource.wireValue,
                    "reason" to reason?.name?.lowercase()
                )
            )
        }
        observationLog.append(
            type = "accessibility_temporarily_unavailable",
            packageName = previous?.packageName,
            activity = previous?.activity,
            evidence = mapOf(
                "requestedSource" to requestedSource.wireValue,
                "reason" to reason?.name?.lowercase()
            )
        )
    }

    fun recordActionCompleted(
        route: String,
        attemptedPath: String? = null,
        backendUsed: String? = null,
        actionEffect: ActionEffectObservation? = null,
        postActionState: PostActionState? = null
    ) {
        val packageName = postActionState?.packageName ?: actionEffect?.finalPackageName
        val activity = postActionState?.activity ?: actionEffect?.finalActivity
        observationLog.append(
            type = "action_completed",
            packageName = packageName,
            activity = activity,
            route = route,
            evidence = mapOf(
                "attemptedPath" to attemptedPath,
                "backendUsed" to backendUsed,
                "snapshotToken" to postActionState?.snapshotToken
            )
        )
        actionEffect?.let { effect ->
            observationLog.append(
                type = "action_effect_observed",
                packageName = effect.finalPackageName ?: packageName,
                activity = effect.finalActivity ?: activity,
                route = route,
                evidence = mapOf(
                    "stateChanged" to effect.stateChanged,
                    "beforeSnapshotToken" to effect.beforeSnapshotToken,
                    "afterSnapshotToken" to effect.afterSnapshotToken,
                    "suggestedSource" to postActionState?.suggestedSource,
                    "fallbackReason" to postActionState?.fallbackReason
                )
            )
        }
    }
}

private data class ObservationSurfaceState(
    val packageName: String? = null,
    val activity: String? = null,
    val renderMode: String? = null,
    val surfaceReadability: String? = null,
    val visualAvailable: Boolean? = null,
    val previewAvailable: Boolean? = null,
    val source: String? = null,
    val suggestedSource: String? = null,
    val fallbackReason: String? = null,
    val accessibilityTemporarilyUnavailable: Boolean = false
)
