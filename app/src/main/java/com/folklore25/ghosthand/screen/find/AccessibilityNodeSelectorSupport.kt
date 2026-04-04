/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.find

import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.screen.read.FlatAccessibilityNode

internal fun buildFindMissHint(
    snapshot: AccessibilityTreeSnapshot,
    strategy: String,
    query: String?,
    clickableOnly: Boolean,
    selectorMatchCount: Int,
    actionableMatchCount: Int
): FindMissHint? {
    if (query.isNullOrBlank()) {
        return null
    }

    val searchedSurface = searchedSurfaceForStrategy(strategy)
    if (searchedSurface == "focused") {
        return null
    }

    val matchSemantics = matchSemanticsForStrategy(strategy)
    val hint = when (strategy) {
        "text" -> when {
            snapshot.nodes.any { it.text?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "text",
                    matchedMatchSemantics = "contains",
                    usedContainsFallback = true,
                    likelyMissReason = "visible_text_is_part_of_a_longer_text_block",
                    suggestedAlternateStrategies = listOf("textContains")
                )
            snapshot.nodes.any { it.contentDesc?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "contentDesc",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    likelyMissReason = "meaningful_label_may_live_in_content_description",
                    suggestedAlternateSurfaces = listOf("contentDesc"),
                    suggestedAlternateStrategies = listOf("contentDescContains")
                )
            snapshot.nodes.any { it.resourceId?.contains(query, ignoreCase = true) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    likelyMissReason = "resource_id_may_be_easier_to_target_than_visible_text",
                    suggestedAlternateSurfaces = listOf("resourceId")
                )
            else ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics
                )
        }

        "textContains" -> when {
            snapshot.nodes.any { it.contentDesc?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "contentDesc",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    likelyMissReason = "meaningful_label_may_live_in_content_description",
                    suggestedAlternateSurfaces = listOf("contentDesc"),
                    suggestedAlternateStrategies = listOf("contentDescContains")
                )
            else ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics
                )
        }

        "contentDesc" -> when {
            snapshot.nodes.any { it.contentDesc?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "contentDesc",
                    matchedMatchSemantics = "contains",
                    usedContainsFallback = true,
                    likelyMissReason = "visible_desc_is_part_of_a_longer_content_description",
                    suggestedAlternateStrategies = listOf("contentDescContains")
                )
            snapshot.nodes.any { it.text?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "text",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    likelyMissReason = "meaningful_label_may_live_in_text",
                    suggestedAlternateSurfaces = listOf("text"),
                    suggestedAlternateStrategies = listOf("textContains")
                )
            else ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics
                )
        }

        "contentDescContains" -> when {
            snapshot.nodes.any { it.text?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "text",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    likelyMissReason = "meaningful_label_may_live_in_text",
                    suggestedAlternateSurfaces = listOf("text"),
                    suggestedAlternateStrategies = listOf("textContains")
                )
            else ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics
                )
        }

        "resourceId" -> when {
            snapshot.nodes.any { it.text?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "text",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    likelyMissReason = "visible_label_is_not_the_same_as_a_resource_id",
                    suggestedAlternateSurfaces = listOf("text"),
                    suggestedAlternateStrategies = listOf("textContains")
                )
            snapshot.nodes.any { it.contentDesc?.contains(query) == true } ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics,
                    matchedSurface = "contentDesc",
                    matchedMatchSemantics = "contains",
                    usedSurfaceFallback = true,
                    usedContainsFallback = true,
                    likelyMissReason = "visible_label_is_not_the_same_as_a_resource_id",
                    suggestedAlternateSurfaces = listOf("contentDesc"),
                    suggestedAlternateStrategies = listOf("contentDescContains")
                )
            else ->
                FindMissHint(
                    searchedSurface = searchedSurface,
                    matchSemantics = matchSemantics
                )
        }

        else -> null
    }

    return hint?.copy(
        failureCategory = failureCategoryFor(
            clickableOnly = clickableOnly,
            selectorMatchCount = selectorMatchCount,
            actionableMatchCount = actionableMatchCount,
            usedSurfaceFallback = hint.usedSurfaceFallback,
            usedContainsFallback = hint.usedContainsFallback,
            likelyMissReason = hint.likelyMissReason
        ),
        selectorMatchCount = selectorMatchCount,
        actionableMatchCount = actionableMatchCount
    )
}

internal fun failureCategoryFor(
    clickableOnly: Boolean,
    selectorMatchCount: Int,
    actionableMatchCount: Int,
    usedSurfaceFallback: Boolean,
    usedContainsFallback: Boolean,
    likelyMissReason: String?
): String {
    if (clickableOnly && selectorMatchCount > 0 && actionableMatchCount == 0) {
        return "actionable_target_not_found"
    }
    if (usedSurfaceFallback && usedContainsFallback) {
        return "alternate_surface_contains_match_available"
    }
    if (usedSurfaceFallback) {
        return "alternate_surface_match_available"
    }
    if (usedContainsFallback) {
        return "same_surface_contains_match_available"
    }
    return when (likelyMissReason) {
        "visible_label_is_not_the_same_as_a_resource_id",
        "resource_id_may_be_easier_to_target_than_visible_text" -> "resource_id_selector_mismatch"
        else -> "no_selector_match"
    }
}

internal fun boundedFallbackChainFor(strategy: String): List<String> {
    return when (strategy) {
        "text" -> listOf("text", "textContains", "contentDesc", "contentDescContains")
        "contentDesc" -> listOf("contentDesc", "contentDescContains", "text", "textContains")
        "textContains" -> listOf("textContains", "contentDescContains")
        "contentDescContains" -> listOf("contentDescContains", "textContains")
        else -> listOf(strategy)
    }
}

internal fun matchesStrategy(
    node: FlatAccessibilityNode,
    strategy: String,
    query: String?
): Boolean {
    return when (strategy) {
        "text" -> node.text == query
        "textContains" -> query != null && node.text?.contains(query) == true
        "resourceId" -> node.resourceId == query
        "contentDesc" -> node.contentDesc == query
        "contentDescContains" -> query != null && node.contentDesc?.contains(query) == true
        "focused" -> node.focused
        else -> false
    }
}

internal fun searchedSurfaceForStrategy(strategy: String): String {
    return when (strategy) {
        "text", "textContains" -> "text"
        "contentDesc", "contentDescContains" -> "contentDesc"
        "resourceId" -> "resourceId"
        "focused" -> "focused"
        else -> strategy
    }
}

internal fun matchSemanticsForStrategy(strategy: String): String {
    return when (strategy) {
        "textContains", "contentDescContains" -> "contains"
        "focused" -> "state"
        else -> "exact"
    }
}
