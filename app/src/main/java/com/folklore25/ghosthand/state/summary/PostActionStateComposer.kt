/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.summary

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.screen.read.ScreenStateLegibilityProjector

object PostActionStateComposer {
    fun fromObservedEffect(
        actionEffect: ActionEffectObservation?,
        fallbackSnapshot: AccessibilityTreeSnapshot?
    ): PostActionState? {
        val packageName = actionEffect?.finalPackageName ?: fallbackSnapshot?.packageName
        val activity = actionEffect?.finalActivity ?: fallbackSnapshot?.activity
        val snapshotToken = actionEffect?.afterSnapshotToken ?: fallbackSnapshot?.snapshotToken
        val legibility = fallbackSnapshot?.let(ScreenStateLegibilityProjector::fromAccessibilitySnapshot)
        if (packageName == null &&
            activity == null &&
            snapshotToken == null &&
            legibility == null
        ) {
            return null
        }

        return PostActionState(
            packageName = packageName,
            activity = activity,
            snapshotToken = snapshotToken,
            focusedEditablePresent = legibility?.focusedEditablePresent,
            renderMode = legibility?.renderMode?.wireValue,
            surfaceReadability = legibility?.surfaceReadability?.wireValue,
            visualAvailable = legibility?.visualAvailable
        )
    }

    fun fields(state: PostActionState): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            state.packageName?.let { put("packageName", it) }
            state.activity?.let { put("activity", it) }
            state.snapshotToken?.let { put("snapshotToken", it) }
            state.focusedEditablePresent?.let { put("focusedEditablePresent", it) }
            state.renderMode?.let { put("renderMode", it) }
            state.surfaceReadability?.let { put("surfaceReadability", it) }
            state.visualAvailable?.let { put("visualAvailable", it) }
        }
    }
}
