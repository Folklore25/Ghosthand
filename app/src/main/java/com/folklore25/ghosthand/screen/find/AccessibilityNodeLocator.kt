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
        val parts = ArrayList<String>(MAX_TOKEN_NODES * 6)

        fun appendNodeFingerprint(node: AccessibilityNodeInfo) {
            if (parts.size >= MAX_TOKEN_NODES * 10) {
                return
            }

            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            parts += node.windowId.toString()
            parts += node.packageName?.toString().orEmpty()
            parts += node.className?.toString().orEmpty()
            parts += node.viewIdResourceName.orEmpty()
            parts += node.text?.toString().orEmpty()
            parts += node.contentDescription?.toString().orEmpty()
            parts += bounds.flattenToString()
            parts += node.childCount.toString()
            parts += node.isClickable.toString()
            parts += node.isFocused.toString()

            for (index in 0 until node.childCount.coerceAtMost(MAX_CHILDREN_PER_TOKEN_NODE)) {
                if (parts.size >= MAX_TOKEN_NODES * 10) {
                    break
                }
                val child = node.getChild(index) ?: continue
                appendNodeFingerprint(child)
            }
        }

        appendNodeFingerprint(rootNode)
        val rawToken = parts.joinToString("|")
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

    private const val MAX_TOKEN_NODES = 64
    private const val MAX_CHILDREN_PER_TOKEN_NODE = 8
}

sealed interface NodeResolutionResult {
    data class Found(val node: AccessibilityNodeInfo) : NodeResolutionResult
    data object InvalidId : NodeResolutionResult
    data object StaleSnapshot : NodeResolutionResult
    data object NotFound : NodeResolutionResult
}
