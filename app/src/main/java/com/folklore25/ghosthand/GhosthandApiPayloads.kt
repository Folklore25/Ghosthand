/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.json.JSONArray
import org.json.JSONObject

data class GhosthandDisclosure(
    val kind: String,
    val summary: String,
    val assumptionToCorrect: String? = null,
    val nextBestActions: List<String> = emptyList()
)

object GhosthandApiPayloads {
    fun treePayload(snapshot: AccessibilityTreeSnapshot): JSONObject {
        return fieldsToJson(treeFields(snapshot))
    }

    fun rawTreePayload(snapshot: AccessibilityTreeSnapshot): JSONObject {
        return fieldsToJson(rawTreeFields(snapshot))
    }

    fun screenPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        return fieldsToJson(
            screenFields(
                snapshot = snapshot,
                editableOnly = editableOnly,
                scrollableOnly = scrollableOnly,
                packageFilter = packageFilter,
                clickableOnly = clickableOnly
            )
        )
    }

    fun nodePayload(node: FlatAccessibilityNode): JSONObject {
        return fieldsToJson(nodeFields(node))
    }

    fun findPayload(result: FindNodeResult): JSONObject {
        return fieldsToJson(findFields(result))
    }

    fun clickPayload(result: ClickAttemptResult): JSONObject {
        val payload = linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "backendUsed" to result.backendUsed,
            "attemptedPath" to result.attemptedPath
        )
        result.selectorResolution?.let { resolution ->
            payload["resolution"] = clickResolutionFields(resolution)
        }
        return fieldsToJson(payload)
    }

    fun treeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        val invalidBoundsCount = snapshot.nodes.count { !it.bounds.isValidGeometry() }
        val lowSignalCount = snapshot.nodes.count { it.isLowSignalNode() }
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "foregroundStableDuringCapture" to snapshot.foregroundStableDuringCapture,
            "partialOutput" to false,
            "returnedNodeCount" to snapshot.nodes.size,
            "warnings" to combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(invalidBoundsCount, "tree"),
                readabilityWarnings = warningsForLowSignal(lowSignalCount, "tree")
            ),
            "invalidBoundsCount" to invalidBoundsCount,
            "lowSignalCount" to lowSignalCount,
            "nodes" to snapshot.nodes.map(::nodeFields)
        )
    }

    fun rawTreeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        val invalidBoundsCount = snapshot.nodes.count { !it.bounds.isValidGeometry() }
        val lowSignalCount = snapshot.nodes.count { it.isLowSignalNode() }
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "foregroundStableDuringCapture" to snapshot.foregroundStableDuringCapture,
            "partialOutput" to false,
            "returnedNodeCount" to snapshot.nodes.size,
            "warnings" to combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(invalidBoundsCount, "tree"),
                readabilityWarnings = warningsForLowSignal(lowSignalCount, "tree")
            ),
            "invalidBoundsCount" to invalidBoundsCount,
            "lowSignalCount" to lowSignalCount,
            "root" to buildRawTreeFields(snapshot, listOf(0))
        )
    }

    fun screenFields(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): Map<String, Any?> {
        val filteredNodes = snapshot.nodes
            .asSequence()
            .filter { !editableOnly || it.editable }
            .filter { !scrollableOnly || it.scrollable }
            .filter { !clickableOnly || it.clickable }
            .filter { packageFilter.isNullOrBlank() || snapshot.packageName == packageFilter }
            .toList()
        val actionableNodes = filteredNodes.filter { it.hasActionableBounds() }
        val omittedInvalidBoundsCount = filteredNodes.size - actionableNodes.size
        val readableNodes = actionableNodes.filterNot { it.isLowSignalNode() }
        val omittedLowSignalCount = actionableNodes.size - readableNodes.size
        val omittedNodeCount = omittedInvalidBoundsCount + omittedLowSignalCount
        val partialOutput = omittedNodeCount > 0
        val elements = readableNodes
            .asSequence()
            .map { node ->
                linkedMapOf(
                    "nodeId" to node.nodeId,
                    "text" to (node.text ?: ""),
                    "desc" to (node.contentDesc ?: ""),
                    "id" to (node.resourceId ?: ""),
                    "clickable" to node.clickable,
                    "editable" to node.editable,
                    "scrollable" to node.scrollable,
                    "bounds" to "[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]",
                    "centerX" to node.centerX,
                    "centerY" to node.centerY
                )
            }
            .toList()

        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "foregroundStableDuringCapture" to snapshot.foregroundStableDuringCapture,
            "partialOutput" to partialOutput,
            "candidateNodeCount" to filteredNodes.size,
            "returnedElementCount" to elements.size,
            "warnings" to combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(omittedInvalidBoundsCount, "screen"),
                readabilityWarnings = warningsForLowSignal(omittedLowSignalCount, "screen"),
                partialWarnings = warningsForPartialOutput(partialOutput)
            ),
            "omittedInvalidBoundsCount" to omittedInvalidBoundsCount,
            "omittedLowSignalCount" to omittedLowSignalCount,
            "omittedNodeCount" to omittedNodeCount,
            "elements" to elements
        )
    }

    fun nodeFields(node: FlatAccessibilityNode): Map<String, Any?> {
        val boundsValid = node.bounds.isValidGeometry()
        val actionableBounds = node.hasActionableBounds()
        val lowSignal = node.isLowSignalNode()
        return linkedMapOf(
            "nodeId" to node.nodeId,
            "text" to node.text,
            "contentDesc" to node.contentDesc,
            "resourceId" to node.resourceId,
            "className" to node.className,
            "clickable" to node.clickable,
            "editable" to node.editable,
            "enabled" to node.enabled,
            "scrollable" to node.scrollable,
            "boundsValid" to boundsValid,
            "actionableBounds" to actionableBounds,
            "lowSignal" to lowSignal,
            "centerX" to node.centerX,
            "centerY" to node.centerY,
            "bounds" to linkedMapOf(
                "left" to node.bounds.left,
                "top" to node.bounds.top,
                "right" to node.bounds.right,
                "bottom" to node.bounds.bottom
            )
        )
    }

    fun findFields(result: FindNodeResult): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "found" to result.found,
            "matchCount" to result.matches.size,
            "index" to result.selectedIndex
        )

        val node = result.node
        if (node == null) {
            payload["node"] = null
            return payload
        }

        payload["node"] = nodeFields(node)
        payload["text"] = node.text ?: ""
        payload["desc"] = node.contentDesc ?: ""
        payload["id"] = node.resourceId ?: ""
        payload["bounds"] = "[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]"
        payload["centerX"] = node.centerX
        payload["centerY"] = node.centerY
        payload["clickable"] = node.clickable
        payload["editable"] = node.editable
        payload["scrollable"] = node.scrollable
        return payload
    }

    fun clickResolutionFields(resolution: ClickSelectorResolution): Map<String, Any?> {
        return linkedMapOf(
            "requestedStrategy" to resolution.requestedStrategy,
            "effectiveStrategy" to resolution.effectiveStrategy,
            "usedContainsFallback" to resolution.usedContainsFallback,
            "matchedNodeId" to resolution.matchedNodeId,
            "matchedNodeClickable" to resolution.matchedNodeClickable,
            "resolvedNodeId" to resolution.resolvedNodeId,
            "resolutionKind" to resolution.resolutionKind,
            "ancestorDepth" to resolution.ancestorDepth
        )
    }

    fun disclosureFields(disclosure: GhosthandDisclosure): Map<String, Any?> {
        return linkedMapOf(
            "kind" to disclosure.kind,
            "summary" to disclosure.summary,
            "assumptionToCorrect" to disclosure.assumptionToCorrect,
            "nextBestActions" to disclosure.nextBestActions
        )
    }

    fun disclosureJson(disclosure: GhosthandDisclosure): JSONObject {
        return fieldsToJson(disclosureFields(disclosure))
    }

    private fun buildRawTreeFields(
        snapshot: AccessibilityTreeSnapshot,
        path: List<Int>
    ): Map<String, Any?>? {
        val targetNode = snapshot.nodes.firstOrNull { node ->
            AccessibilityNodeLocator.pathSegments(node.nodeId) == path
        } ?: return null

        val children = snapshot.nodes
            .asSequence()
            .filter { candidate ->
                val candidatePath = AccessibilityNodeLocator.pathSegments(candidate.nodeId)
                candidatePath.size == path.size + 1 &&
                    candidatePath.dropLast(1) == path
            }
            .sortedBy { AccessibilityNodeLocator.pathSegments(it.nodeId).lastOrNull() ?: 0 }
            .mapNotNull { child ->
                buildRawTreeFields(snapshot, AccessibilityNodeLocator.pathSegments(child.nodeId))
            }
            .toList()

        return nodeFields(targetNode) + ("children" to children)
    }

    private fun fieldsToJson(fields: Map<String, Any?>): JSONObject {
        return JSONObject().apply {
            fields.forEach { (key, value) ->
                put(key, toJsonValue(value))
            }
        }
    }

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> {
                JSONObject().apply {
                    value.forEach { (key, nestedValue) ->
                        put(key as String, toJsonValue(nestedValue))
                    }
                }
            }
            is List<*> -> {
                JSONArray().apply {
                    value.forEach { item -> put(toJsonValue(item)) }
                }
            }
            else -> value
        }
    }

    private fun warningsForInvalidBounds(count: Int, route: String): List<String> {
        if (count <= 0) {
            return emptyList()
        }
        return when (route) {
            "screen" -> listOf("omitted_nodes_with_invalid_bounds")
            else -> listOf("invalid_bounds_present")
        }
    }

    private fun combinedWarnings(
        freshnessWarnings: List<String>,
        geometryWarnings: List<String>,
        readabilityWarnings: List<String>,
        partialWarnings: List<String> = emptyList()
    ): List<String> {
        return (freshnessWarnings + geometryWarnings + readabilityWarnings + partialWarnings).distinct()
    }

    private fun warningsForLowSignal(count: Int, route: String): List<String> {
        if (count <= 0) {
            return emptyList()
        }
        return when (route) {
            "screen" -> listOf("omitted_low_signal_nodes")
            else -> listOf("low_signal_nodes_present")
        }
    }

    private fun warningsForPartialOutput(partialOutput: Boolean): List<String> {
        return if (partialOutput) listOf("partial_output") else emptyList()
    }
}

private fun NodeBounds.isValidGeometry(): Boolean {
    return right > left && bottom > top
}

private fun FlatAccessibilityNode.hasActionableBounds(): Boolean {
    if (!bounds.isValidGeometry()) {
        return false
    }
    if (bounds.left < 0 || bounds.top < 0 || bounds.right < 0 || bounds.bottom < 0) {
        return false
    }
    return centerX in bounds.left..bounds.right && centerY in bounds.top..bounds.bottom
}

private fun FlatAccessibilityNode.isLowSignalNode(): Boolean {
    val hasMeaningfulLabel = !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !resourceId.isNullOrBlank()
    if (hasMeaningfulLabel) {
        return false
    }
    if (clickable || editable || scrollable || focused) {
        return false
    }
    return true
}
