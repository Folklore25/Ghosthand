package com.folklore25.ghosthand

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityNodeLocator {
    fun createNodeId(path: List<Int>, snapshotToken: String): String {
        return "p${path.joinToString(".")}@t$snapshotToken"
    }

    fun pathSegments(nodeId: String): List<Int> {
        val prefix = nodeId.substringBefore("@t")
        if (!prefix.startsWith("p")) {
            return emptyList()
        }
        return prefix.removePrefix("p").split('.').mapNotNull { it.toIntOrNull() }
    }

    fun snapshotToken(rootNode: AccessibilityNodeInfo): String {
        val bounds = Rect()
        rootNode.getBoundsInScreen(bounds)
        val rawToken = listOf(
            rootNode.windowId.toString(),
            rootNode.packageName?.toString().orEmpty(),
            rootNode.className?.toString().orEmpty(),
            bounds.left.toString(),
            bounds.top.toString(),
            bounds.right.toString(),
            bounds.bottom.toString(),
            rootNode.childCount.toString()
        ).joinToString("|")
        return rawToken.hashCode().toUInt().toString(16)
    }

    fun resolveAgainstRoot(
        nodeId: String,
        rootNode: AccessibilityNodeInfo
    ): NodeResolutionResult {
        val encoded = parseEncodedNodeId(nodeId)
        if (encoded == EncodedNodeId.Invalid) {
            return NodeResolutionResult.InvalidId
        }

        if (encoded is EncodedNodeId.PathBased) {
            if (encoded.snapshotToken != snapshotToken(rootNode)) {
                return NodeResolutionResult.StaleSnapshot
            }

            val resolvedNode = resolveByPath(rootNode, encoded.path)
                ?: return NodeResolutionResult.NotFound
            return NodeResolutionResult.Found(resolvedNode)
        }

        val traversalIndex = nodeId.removePrefix("n").toIntOrNull()
            ?: return NodeResolutionResult.InvalidId

        val resolvedNode = resolveByTraversalIndex(rootNode, traversalIndex)
            ?: return NodeResolutionResult.NotFound
        return NodeResolutionResult.Found(resolvedNode)
    }

    private fun parseEncodedNodeId(nodeId: String): EncodedNodeId? {
        if (!nodeId.startsWith("p")) {
            return null
        }

        val segments = nodeId.split("@t", limit = 2)
        if (segments.size != 2) {
            return EncodedNodeId.Invalid
        }

        val path = segments[0]
            .removePrefix("p")
            .split('.')
            .mapNotNull { it.toIntOrNull() }

        val snapshotToken = segments[1].takeIf { it.isNotBlank() }
        if (path.isEmpty() || snapshotToken == null) {
            return EncodedNodeId.Invalid
        }

        return EncodedNodeId.PathBased(
            path = path,
            snapshotToken = snapshotToken
        )
    }

    private fun resolveByPath(
        rootNode: AccessibilityNodeInfo,
        path: List<Int>
    ): AccessibilityNodeInfo? {
        if (path.firstOrNull() != 0) {
            return null
        }

        var currentNode: AccessibilityNodeInfo = rootNode
        for (childIndex in path.drop(1)) {
            currentNode = currentNode.getChild(childIndex) ?: return null
        }
        return currentNode
    }

    private fun resolveByTraversalIndex(
        node: AccessibilityNodeInfo,
        targetIndex: Int
    ): AccessibilityNodeInfo? {
        var currentIndex = -1

        fun visit(current: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            currentIndex += 1
            if (currentIndex == targetIndex) {
                return current
            }

            for (childIndex in 0 until current.childCount) {
                val child = current.getChild(childIndex) ?: continue
                val match = visit(child)
                if (match != null) {
                    return match
                }
            }
            return null
        }

        return visit(node)
    }

    private sealed interface EncodedNodeId {
        data class PathBased(
            val path: List<Int>,
            val snapshotToken: String
        ) : EncodedNodeId

        data object Invalid : EncodedNodeId
    }
}

sealed interface NodeResolutionResult {
    data class Found(val node: AccessibilityNodeInfo) : NodeResolutionResult
    data object InvalidId : NodeResolutionResult
    data object StaleSnapshot : NodeResolutionResult
    data object NotFound : NodeResolutionResult
}
