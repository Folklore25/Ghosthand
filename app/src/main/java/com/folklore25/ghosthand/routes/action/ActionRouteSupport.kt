/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

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

import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.state.summary.PostActionStateComposer
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
    val finalActivity: String?,
    val afterSnapshot: AccessibilityTreeSnapshot? = null
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
    return ScrollSurfaceObservation(
        surfaceChanged = surfaceChanged,
        beforeSnapshotToken = beforeToken,
        afterSnapshotToken = afterToken,
        finalPackageName = afterPackage,
        finalActivity = afterActivity,
        afterSnapshot = afterSnapshot
    )
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
        ?.let(PostActionStateComposer::fields)
        ?.takeIf { it.isNotEmpty() }
        ?.let { put("postActionState", JSONObject(it)) }
    return this
}
