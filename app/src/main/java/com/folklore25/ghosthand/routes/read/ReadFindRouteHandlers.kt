/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.screen.find.FindMissHint
import com.folklore25.ghosthand.screen.find.FindNodeResult
import com.folklore25.ghosthand.screen.read.TreeUnavailableReason

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.payload.GhosthandPayloadJsonSupport
import com.folklore25.ghosthand.payload.GhosthandScreenPayloads
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.buildTreeUnavailableResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.parseSelector
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator

internal class ReadFindRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun buildFindResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/find")
            ?: return badJsonBodyResponse()

        val selector = parseSelector(body)
            ?: return buildJsonResponse(
                statusCode = 400,
                body = errorEnvelope(
                    code = "INVALID_ARGUMENT",
                    message = "One of text, desc, id, or strategy is required."
                )
            )

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
        val payload = GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.findFields(result))

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

    private companion object {
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
