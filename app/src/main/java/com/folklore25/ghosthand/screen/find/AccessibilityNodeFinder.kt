/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.find

import com.folklore25.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.screen.read.FlatAccessibilityNode

import com.folklore25.ghosthand.R

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
