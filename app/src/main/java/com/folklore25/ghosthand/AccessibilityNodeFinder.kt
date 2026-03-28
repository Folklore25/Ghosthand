package com.folklore25.ghosthand

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
                resolveClickableTarget(snapshot, matchedNode)
            }.distinctBy { it.nodeId }
        } else {
            matches
        }

        val selectedIndex = index.coerceAtLeast(0)
        val matchedNode = effectiveMatches.getOrNull(selectedIndex)

        return FindNodeResult(
            found = matchedNode != null,
            node = matchedNode,
            matches = effectiveMatches,
            selectedIndex = selectedIndex
        )
    }

    private fun resolveClickableTarget(
        snapshot: AccessibilityTreeSnapshot,
        node: FlatAccessibilityNode
    ): FlatAccessibilityNode? {
        if (node.clickable) {
            return node
        }

        val path = AccessibilityNodeLocator.pathSegments(node.nodeId)
        for (depth in path.size - 1 downTo 1) {
            val ancestorPath = path.take(depth)
            val ancestorNode = snapshot.nodes.firstOrNull { candidate ->
                AccessibilityNodeLocator.pathSegments(candidate.nodeId) == ancestorPath
            }
            if (ancestorNode?.clickable == true) {
                return ancestorNode
            }
        }

        return null
    }
}

data class FindNodeResult(
    val found: Boolean,
    val node: FlatAccessibilityNode?,
    val matches: List<FlatAccessibilityNode> = emptyList(),
    val selectedIndex: Int = 0
)
