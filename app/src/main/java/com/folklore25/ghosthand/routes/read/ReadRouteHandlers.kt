/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.FindMissHint
import com.folklore25.ghosthand.FindNodeResult
import com.folklore25.ghosthand.TreeUnavailableReason
import com.folklore25.ghosthand.payload.GhosthandApiPayloads
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.preview.ScreenPreviewCaptureSupport
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.buildTreeUnavailableResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.parseSelector
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.server.LocalApiServerRoute
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONObject

internal class ReadRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("GET", "/tree") { request -> buildTreeResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/find") { request -> buildFindResponse(request.requestBody) },
            LocalApiServerRoute("GET", "/screen") { request -> buildScreenResponse(request.queryParameters) },
            LocalApiServerRoute("GET", "/screenshot") { request -> buildScreenshotGetResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/screenshot") { request -> buildScreenshotResponse(request.requestBody) },
            LocalApiServerRoute("GET", "/info") { buildInfoResponse() },
            LocalApiServerRoute("GET", "/focused") { buildFocusedResponse() }
        )
    }

    private fun buildTreeResponse(queryParameters: Map<String, String>): String {
        val mode = queryParameters["mode"]?.ifBlank { DEFAULT_TREE_MODE } ?: DEFAULT_TREE_MODE
        if (mode != TREE_MODE_FLAT && mode != TREE_MODE_RAW) {
            return buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "UNSUPPORTED_OPERATION",
                    message = "Only mode=flat and mode=raw are supported for /tree."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                stateCoordinator.createTreePayload(
                    treeSnapshotResult.snapshot ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT),
                    mode = mode
                )
            )
        )
    }

    private fun buildFindResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/find")
            ?: return badJsonBodyResponse()

        val selector = parseSelector(body)
        if (selector == null) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "One of text, desc, id, or strategy is required."
                )
            )
        }

        if (selector.strategy !in SUPPORTED_FIND_STRATEGIES) {
            return buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "UNSUPPORTED_OPERATION",
                    message = "Unsupported /find strategy: ${selector.strategy}."
                )
            )
        }

        if (selector.strategy == FIND_STRATEGY_FOCUSED && !selector.query.isNullOrBlank()) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "query must be omitted or null for focused strategy."
                )
            )
        }

        if (selector.strategy != FIND_STRATEGY_FOCUSED && selector.query.isNullOrBlank()) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "query is required for strategy ${selector.strategy}."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
            ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)
        val clickableOnly = body.optBoolean("clickable", false)
        val index = body.optIntOrNull("index") ?: 0
        val result = stateCoordinator.findResult(
            snapshot = snapshot,
            strategy = selector.strategy,
            query = selector.query,
            clickableOnly = clickableOnly,
            index = index
        )
        val payload = GhosthandApiPayloads.findPayload(result)

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                data = payload,
                disclosure = buildFindDisclosure(
                    strategy = selector.strategy,
                    clickableOnly = clickableOnly,
                    result = result
                )
            )
        )
    }

    private fun buildScreenResponse(queryParameters: Map<String, String>): String {
        val mode = ScreenReadMode.fromWireValue(queryParameters["source"]) ?: ScreenReadMode.ACCESSIBILITY
        val summaryOnly = screenSummaryOnlyRequested(queryParameters)
        val includePreviewThumb = screenPreviewThumbRequested(queryParameters)
        val editableOnly = queryParameters["editable"] == "true"
        val scrollableOnly = queryParameters["scrollable"] == "true"
        val clickableOnly = queryParameters["clickable"] == "true"
        if (mode != ScreenReadMode.ACCESSIBILITY && (editableOnly || scrollableOnly || clickableOnly)) {
            return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "editable, scrollable, and clickable filters are only supported for source=accessibility."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (mode == ScreenReadMode.ACCESSIBILITY && !treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        val snapshot = treeSnapshotResult.snapshot
        val payload = when (mode) {
            ScreenReadMode.ACCESSIBILITY -> {
                val requiredSnapshot = snapshot
                    ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT)
                stateCoordinator.createScreenReadPayload(
                    snapshot = requiredSnapshot,
                    editableOnly = editableOnly,
                    scrollableOnly = scrollableOnly,
                    packageFilter = queryParameters["package"],
                    clickableOnly = clickableOnly
                )
            }

            ScreenReadMode.OCR -> stateCoordinator.createOcrScreenPayload()
            ScreenReadMode.HYBRID -> {
                if (snapshot != null) {
                    stateCoordinator.createHybridScreenPayload(
                        snapshot = snapshot,
                        packageFilter = queryParameters["package"]
                    )
                } else {
                    stateCoordinator.createOcrScreenPayload()
                }
            }
        }

        val payloadWithPreview = if (includePreviewThumb && payload.previewAvailable == true) {
            val preview = stateCoordinator.captureBestScreenshot(
                StateCoordinator.SCREEN_PREVIEW_WIDTH,
                StateCoordinator.SCREEN_PREVIEW_HEIGHT
            )
            ScreenPreviewCaptureSupport.withPreviewImage(payload, preview)
        } else {
            payload
        }

        val bodyPayload = if (summaryOnly) {
            GhosthandApiPayloads.screenSummaryPayload(payloadWithPreview)
        } else {
            GhosthandApiPayloads.screenReadPayload(payloadWithPreview)
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                data = bodyPayload,
                disclosure = buildScreenDisclosure(payloadWithPreview)
            )
        )
    }

    private fun buildScreenshotGetResponse(queryParameters: Map<String, String>): String {
        val width = queryParameters["width"]?.toIntOrNull() ?: 0
        val height = queryParameters["height"]?.toIntOrNull() ?: 0
        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)

        return if (screenshotResult.available) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }

    private fun buildScreenshotResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/screenshot")
            ?: return badJsonBodyResponse()

        val width = body.optIntOrNull("width") ?: 0
        val height = body.optIntOrNull("height") ?: 0

        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)
        return if (screenshotResult.available) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }

    private fun buildInfoResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createInfoPayload())
        )
    }

    private fun buildFocusedResponse(): String {
        val result = stateCoordinator.getFocusedNodeResult()
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createFocusedNodePayload(result))
        )
    }

    private companion object {
        const val TREE_MODE_RAW = "raw"
        const val TREE_MODE_FLAT = "flat"
        const val DEFAULT_TREE_MODE = TREE_MODE_RAW
        const val FIND_STRATEGY_FOCUSED = "focused"
        val SUPPORTED_FIND_STRATEGIES = setOf(
            "text",
            "textContains",
            "resourceId",
            "contentDesc",
            "contentDescContains",
            FIND_STRATEGY_FOCUSED
        )
    }
}

internal fun screenSummaryOnlyRequested(queryParameters: Map<String, String>): Boolean {
    return queryParameters["summaryOnly"] == "true"
}

internal fun screenPreviewThumbRequested(queryParameters: Map<String, String>): Boolean {
    return queryParameters["includePreview"] == "thumb"
}

internal fun buildScreenDisclosure(payload: JSONObject): GhosthandDisclosure? {
    return buildScreenDisclosure(
        partialOutput = payload.optBoolean("partialOutput", false),
        foregroundStableDuringCapture = payload.optBoolean("foregroundStableDuringCapture", true)
    )
}

internal fun buildScreenDisclosure(payload: ScreenReadPayload): GhosthandDisclosure? {
    return buildScreenDisclosure(
        partialOutput = payload.partialOutput,
        foregroundStableDuringCapture = payload.foregroundStableDuringCapture
    )
}

internal fun buildScreenDisclosure(
    partialOutput: Boolean,
    foregroundStableDuringCapture: Boolean
): GhosthandDisclosure? {
    val unstableCapture = !foregroundStableDuringCapture
    if (!partialOutput && !unstableCapture) {
        return null
    }
    return GhosthandDisclosure(
        kind = if (partialOutput) "shaped_output" else "constraint",
        summary = if (partialOutput) {
            "This /screen result is a reduced actionable view; omitted elements are not proof of absence."
        } else {
            "This /screen capture completed under foreground drift, so absence should be treated cautiously."
        },
        assumptionToCorrect = "Anything not returned by /screen was not present.",
        nextBestActions = listOf(
            "Use /tree when you need fuller structural truth.",
            "Use /screenshot when visual truth and structured output disagree."
        )
    )
}

internal fun buildFindDisclosure(
    strategy: String,
    clickableOnly: Boolean,
    result: FindNodeResult
): GhosthandDisclosure? {
    if (result.found) {
        return null
    }
    val missHint = result.missHint
    val searchedSurface = missHint?.searchedSurface ?: selectorAliasForStrategy(strategy)
    val matchSemantics = missHint?.matchSemantics ?: selectorMatchSemantics(strategy)
    if (clickableOnly) {
        return GhosthandDisclosure(
            kind = "discoverability",
            summary = "This $matchSemantics $searchedSurface lookup only returned actionable targets because `clickable=true` was enabled.",
            assumptionToCorrect = "A visible label must itself be directly clickable to be discoverable.",
            nextBestActions = listOf(
                "Retry /find without clickable=true to inspect child labels first.",
                findAlternateAction(strategy, missHint)
            )
        )
    }
    val summary = when (missHint?.likelyMissReason) {
        "visible_text_is_part_of_a_longer_text_block" ->
            "This lookup used exact text matching, so a visible prefix can still miss when the real node text is longer."
        "visible_desc_is_part_of_a_longer_content_description" ->
            "This lookup used exact content-description matching, so a visible prefix can still miss when the real description is longer."
        "meaningful_label_may_live_in_content_description" ->
            "This lookup searched exact text only; visible content can instead live in content descriptions."
        "meaningful_label_may_live_in_text" ->
            "This lookup searched $matchSemantics content descriptions only; the meaningful label can instead live in text."
        "visible_label_is_not_the_same_as_a_resource_id" ->
            "This lookup searched exact resource ids only; a visible label is not the same thing as a resource id."
        "resource_id_may_be_easier_to_target_than_visible_text" ->
            "This lookup searched exact text only; the element may be easier to target by resource id."
        else ->
            "This lookup searched the $searchedSurface surface using $matchSemantics matching only."
    }
    val assumptionToCorrect = when (missHint?.likelyMissReason) {
        "visible_text_is_part_of_a_longer_text_block",
        "visible_desc_is_part_of_a_longer_content_description" ->
            "/screen-visible text always matches exact /find text."
        "meaningful_label_may_live_in_content_description",
        "meaningful_label_may_live_in_text" ->
            "The visible label always lives on the selector surface I just searched."
        "visible_label_is_not_the_same_as_a_resource_id" ->
            "A visible label should be discoverable as an exact resource id."
        else ->
            "A /find miss means the platform cannot see the node."
    }
    val nextActions = mutableListOf<String>()
    missHint?.suggestedAlternateStrategies?.firstOrNull()?.let { nextActions += "Retry with $it." }
    if (nextActions.size < 2) {
        nextActions += when {
            missHint?.suggestedAlternateSurfaces?.contains("contentDesc") == true ->
                "Inspect /screen for desc or retry with contentDesc."
            missHint?.suggestedAlternateSurfaces?.contains("text") == true ->
                "Inspect /screen for text or retry with text."
            missHint?.suggestedAlternateSurfaces?.contains("resourceId") == true ->
                "Inspect /screen for id or retry with resourceId."
            else ->
                findAlternateAction(strategy, missHint)
        }
    }
    return GhosthandDisclosure(
        kind = "discoverability",
        summary = summary,
        assumptionToCorrect = assumptionToCorrect,
        nextBestActions = nextActions.distinct().take(2)
    )
}

private fun selectorAliasForStrategy(strategy: String): String {
    return when (strategy) {
        "contentDesc", "contentDescContains" -> "desc"
        "resourceId" -> "id"
        "focused" -> "focused"
        else -> "text"
    }
}

private fun alternateSelectorAction(strategy: String): String {
    return when (strategy) {
        "text", "textContains" -> "Retry with desc if the meaningful label lives in content descriptions."
        "contentDesc", "contentDescContains" -> "Retry with text if the visible label is rendered as text."
        "resourceId" -> "Retry with text or desc if you only know the visible label."
        else -> "Retry with text, desc, or id based on the surface you can actually observe."
    }
}

private fun findAlternateAction(strategy: String, missHint: FindMissHint?): String {
    return when {
        missHint?.suggestedAlternateSurfaces?.contains("contentDesc") == true ->
            "Retry with contentDesc if the meaningful label lives in desc."
        missHint?.suggestedAlternateSurfaces?.contains("text") == true ->
            "Retry with text if the meaningful label lives in visible text."
        missHint?.suggestedAlternateSurfaces?.contains("resourceId") == true ->
            "Retry with resourceId if the element is easier to target by id."
        else -> alternateSelectorAction(strategy)
    }
}

private fun selectorMatchSemantics(strategy: String): String {
    return when (strategy) {
        "textContains", "contentDescContains" -> "contains"
        "focused" -> "state"
        else -> "exact"
    }
}
