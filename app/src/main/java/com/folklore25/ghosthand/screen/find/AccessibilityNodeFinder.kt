/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.find

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

class AccessibilityNodeFinder {
    fun findNode(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?
    ): FindNodeResult {
        return findNodes(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = false,
            index = 0
        )
    }

    fun findNodes(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int
    ): FindNodeResult {
        val normalizedStrategy = strategy.trim()
        val normalizedQuery = query?.takeIf { it.isNotBlank() }

        val matches = snapshot.nodes.filter { node ->
            val matched = when (normalizedStrategy) {
                "text" -> node.text == normalizedQuery
                "textContains" -> normalizedQuery != null && node.text?.contains(normalizedQuery) == true
                "resourceId" -> node.resourceId == normalizedQuery
                "contentDesc" -> node.contentDesc == normalizedQuery
                "contentDescContains" -> normalizedQuery != null && node.contentDesc?.contains(normalizedQuery) == true
                "focused" -> node.focused
                else -> false
            }

            matched
        }
        val effectiveMatches = if (clickableOnly) {
            matches.mapNotNull { matchedNode ->
                resolveClickableTarget(snapshot, matchedNode)?.targetNode
            }.distinctBy { resolvedNode -> resolvedNode.nodeId }
        } else {
            matches
        }

        val selectedIndex = index.coerceAtLeast(0)
        val matchedNode = effectiveMatches.getOrNull(selectedIndex)

        return FindNodeResult(
            found = matchedNode != null,
            node = matchedNode,
            matches = effectiveMatches,
            selectedIndex = selectedIndex,
            missHint = if (matchedNode == null) {
                buildFindMissHint(
                    snapshot = snapshot,
                    strategy = normalizedStrategy,
                    query = normalizedQuery,
                    clickableOnly = clickableOnly,
                    selectorMatchCount = matches.size,
                    actionableMatchCount = effectiveMatches.size
                )
            } else {
                null
            }
        )
    }

    fun findNodesForClick(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int
    ): FindNodeResult {
        val requestedStrategy = strategy.trim()
        val chain = boundedFallbackChainFor(requestedStrategy)
        val primaryAttempt = findNodesForClickWithStrategy(
            snapshot = snapshot,
            requestedStrategy = requestedStrategy,
            effectiveStrategy = requestedStrategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
        if (primaryAttempt.found || !clickableOnly || chain.size == 1) {
            return primaryAttempt
        }

        for (fallbackStrategy in chain.drop(1)) {
            val fallbackAttempt = findNodesForClickWithStrategy(
                snapshot = snapshot,
                requestedStrategy = requestedStrategy,
                effectiveStrategy = fallbackStrategy,
                query = query,
                clickableOnly = clickableOnly,
                index = index
            )
            if (fallbackAttempt.found) {
                return fallbackAttempt
            }
        }

        return primaryAttempt
    }

    private fun findNodesForClickWithStrategy(
        snapshot: AccessibilityTreeSnapshot,
        requestedStrategy: String,
        effectiveStrategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int
    ): FindNodeResult {
        val normalizedStrategy = effectiveStrategy.trim()
        val normalizedQuery = query?.takeIf { it.isNotBlank() }
        val matches = snapshot.nodes.filter { node ->
            matchesStrategy(node = node, strategy = normalizedStrategy, query = normalizedQuery)
        }
        val requestedSurface = searchedSurfaceForStrategy(requestedStrategy)
        val matchedSurface = searchedSurfaceForStrategy(normalizedStrategy)
        val requestedMatchSemantics = matchSemanticsForStrategy(requestedStrategy)
        val matchedMatchSemantics = matchSemanticsForStrategy(normalizedStrategy)
        val usedSurfaceFallback = requestedSurface != matchedSurface
        val usedContainsFallback =
            requestedMatchSemantics != matchedMatchSemantics && matchedMatchSemantics == "contains"

        val selectedIndex = index.coerceAtLeast(0)
        if (!clickableOnly) {
            val matchedNode = matches.getOrNull(selectedIndex)
            return FindNodeResult(
                found = matchedNode != null,
                node = matchedNode,
                matches = matches,
                selectedIndex = selectedIndex,
                clickResolution = matchedNode?.let { matched ->
                    ClickSelectorResolution(
                        requestedStrategy = requestedStrategy,
                        effectiveStrategy = effectiveStrategy,
                        requestedSurface = requestedSurface,
                        matchedSurface = matchedSurface,
                        requestedMatchSemantics = requestedMatchSemantics,
                        matchedMatchSemantics = matchedMatchSemantics,
                        usedSurfaceFallback = usedSurfaceFallback,
                        usedContainsFallback = usedContainsFallback,
                        matchedNodeId = matched.nodeId,
                        matchedNodeClickable = matched.clickable,
                        resolvedNodeId = matched.nodeId,
                        resolutionKind = "matched_node",
                        ancestorDepth = null
                    )
                },
                missHint = if (matchedNode == null) {
                    buildFindMissHint(
                        snapshot = snapshot,
                        strategy = normalizedStrategy,
                        query = normalizedQuery,
                        clickableOnly = false,
                        selectorMatchCount = matches.size,
                        actionableMatchCount = matches.size
                    )
                } else {
                    null
                }
            )
        }

        val resolvedMatches = matches.mapNotNull { matchedNode ->
            resolveClickableTarget(snapshot, matchedNode)?.let { resolved ->
                ClickMatch(
                    matchedNode = matchedNode,
                    targetNode = resolved.targetNode,
                    resolutionKind = resolved.resolutionKind,
                    ancestorDepth = resolved.ancestorDepth
                )
            }
        }.distinctBy { it.targetNode.nodeId }

        val selectedMatch = resolvedMatches.getOrNull(selectedIndex)

        return FindNodeResult(
            found = selectedMatch != null,
            node = selectedMatch?.targetNode,
            matches = resolvedMatches.map { it.targetNode },
            selectedIndex = selectedIndex,
            clickResolution = selectedMatch?.let { match ->
                ClickSelectorResolution(
                    requestedStrategy = requestedStrategy,
                    effectiveStrategy = effectiveStrategy,
                    requestedSurface = requestedSurface,
                    matchedSurface = matchedSurface,
                    requestedMatchSemantics = requestedMatchSemantics,
                    matchedMatchSemantics = matchedMatchSemantics,
                    usedSurfaceFallback = usedSurfaceFallback,
                    usedContainsFallback = usedContainsFallback,
                    matchedNodeId = match.matchedNode.nodeId,
                    matchedNodeClickable = match.matchedNode.clickable,
                    resolvedNodeId = match.targetNode.nodeId,
                    resolutionKind = match.resolutionKind,
                    ancestorDepth = match.ancestorDepth
                )
            },
            missHint = if (selectedMatch == null) {
                buildFindMissHint(
                    snapshot = snapshot,
                    strategy = normalizedStrategy,
                    query = normalizedQuery,
                    clickableOnly = clickableOnly,
                    selectorMatchCount = matches.size,
                    actionableMatchCount = resolvedMatches.size
                )
            } else {
                null
            }
        )
    }

    private fun buildFindMissHint(
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

    private fun failureCategoryFor(
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

    private fun boundedFallbackChainFor(strategy: String): List<String> {
        return when (strategy) {
            "text" -> listOf("text", "textContains", "contentDesc", "contentDescContains")
            "contentDesc" -> listOf("contentDesc", "contentDescContains", "text", "textContains")
            "textContains" -> listOf("textContains", "contentDescContains")
            "contentDescContains" -> listOf("contentDescContains", "textContains")
            else -> listOf(strategy)
        }
    }

    private fun matchesStrategy(
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

    private fun searchedSurfaceForStrategy(strategy: String): String {
        return when (strategy) {
            "text", "textContains" -> "text"
            "contentDesc", "contentDescContains" -> "contentDesc"
            "resourceId" -> "resourceId"
            "focused" -> "focused"
            else -> strategy
        }
    }

    private fun matchSemanticsForStrategy(strategy: String): String {
        return when (strategy) {
            "textContains", "contentDescContains" -> "contains"
            "focused" -> "state"
            else -> "exact"
        }
    }

    private fun resolveClickableTarget(
        snapshot: AccessibilityTreeSnapshot,
        node: FlatAccessibilityNode
    ): ResolvedClickableTarget? {
        if (node.clickable) {
            return ResolvedClickableTarget(
                targetNode = node,
                resolutionKind = "matched_node",
                ancestorDepth = null
            )
        }

        val path = AccessibilityNodeLocator.pathSegments(node.nodeId)
        for (depth in path.size - 1 downTo 1) {
            val ancestorPath = path.take(depth)
            val ancestorNode = snapshot.nodes.firstOrNull { candidate ->
                AccessibilityNodeLocator.pathSegments(candidate.nodeId) == ancestorPath
            }
            if (ancestorNode?.clickable == true) {
                return ResolvedClickableTarget(
                    targetNode = ancestorNode,
                    resolutionKind = "clickable_ancestor",
                    ancestorDepth = path.size - ancestorPath.size
                )
            }
        }

        return null
    }
}

data class FindNodeResult(
    val found: Boolean,
    val node: FlatAccessibilityNode?,
    val matches: List<FlatAccessibilityNode> = emptyList(),
    val selectedIndex: Int = 0,
    val clickResolution: ClickSelectorResolution? = null,
    val missHint: FindMissHint? = null
)

data class FindMissHint(
    val searchedSurface: String,
    val matchSemantics: String,
    val requestedSurface: String = searchedSurface,
    val requestedMatchSemantics: String = matchSemantics,
    val matchedSurface: String? = null,
    val matchedMatchSemantics: String? = null,
    val usedSurfaceFallback: Boolean = false,
    val usedContainsFallback: Boolean = false,
    val likelyMissReason: String? = null,
    val failureCategory: String? = null,
    val selectorMatchCount: Int = 0,
    val actionableMatchCount: Int = 0,
    val suggestedAlternateSurfaces: List<String> = emptyList(),
    val suggestedAlternateStrategies: List<String> = emptyList()
)

data class ClickSelectorResolution(
    val requestedStrategy: String,
    val effectiveStrategy: String,
    val requestedSurface: String = "",
    val matchedSurface: String = "",
    val requestedMatchSemantics: String = "",
    val matchedMatchSemantics: String = "",
    val usedSurfaceFallback: Boolean = false,
    val usedContainsFallback: Boolean,
    val matchedNodeId: String,
    val matchedNodeClickable: Boolean,
    val resolvedNodeId: String,
    val resolutionKind: String,
    val ancestorDepth: Int?
)

private data class ClickMatch(
    val matchedNode: FlatAccessibilityNode,
    val targetNode: FlatAccessibilityNode,
    val resolutionKind: String,
    val ancestorDepth: Int?
)

private data class ResolvedClickableTarget(
    val targetNode: FlatAccessibilityNode,
    val resolutionKind: String,
    val ancestorDepth: Int?
)
