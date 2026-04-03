/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ActionEffectObservation
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import org.json.JSONObject

internal const val TARGET_TYPE_POINT = "point"
internal const val TARGET_TYPE_NODE = "node"
internal const val BACKEND_AUTO = "auto"
internal const val BACKEND_ACCESSIBILITY = "accessibility"
internal const val DEFAULT_BACKEND = "auto"

internal data class ScrollSurfaceObservation(
    val surfaceChanged: Boolean,
    val beforeSnapshotToken: String?,
    val afterSnapshotToken: String?,
    val finalPackageName: String?,
    val finalActivity: String?
)

internal fun unsupportedBackendResponse(): String {
    return buildJsonResponse(422, errorEnvelope("UNSUPPORTED_OPERATION", "Only backend=accessibility and backend=auto are supported."))
}

internal fun observeScrollSurfaceChange(
    beforeSnapshot: AccessibilityTreeSnapshot?,
    afterSnapshot: AccessibilityTreeSnapshot?
): ScrollSurfaceObservation {
    val beforeToken = beforeSnapshot?.snapshotToken
    val afterToken = afterSnapshot?.snapshotToken
    val beforePackage = beforeSnapshot?.packageName
    val afterPackage = afterSnapshot?.packageName
    val beforeActivity = beforeSnapshot?.activity
    val afterActivity = afterSnapshot?.activity
    val surfaceChanged = (beforeToken != null && afterToken != null && beforeToken != afterToken) ||
        beforePackage != afterPackage ||
        beforeActivity != afterActivity
    return ScrollSurfaceObservation(surfaceChanged, beforeToken, afterToken, afterPackage, afterActivity)
}

internal fun observeActionSurfaceChange(
    beforeSnapshot: AccessibilityTreeSnapshot?,
    snapshotProvider: () -> AccessibilityTreeSnapshot?
): ScrollSurfaceObservation {
    Thread.sleep(300L)
    return observeScrollSurfaceChange(beforeSnapshot, snapshotProvider())
}

internal fun ScrollSurfaceObservation.toActionEffectObservation(): ActionEffectObservation {
    return ActionEffectObservation(surfaceChanged, beforeSnapshotToken, afterSnapshotToken, finalPackageName, finalActivity)
}

internal fun JSONObject.putPostActionState(state: PostActionState?): JSONObject {
    state
        ?.let(GhosthandApiPayloads::postActionStateFields)
        ?.takeIf { it.isNotEmpty() }
        ?.let { put("postActionState", JSONObject(it)) }
    return this
}
