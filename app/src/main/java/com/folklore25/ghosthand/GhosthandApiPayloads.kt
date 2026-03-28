package com.folklore25.ghosthand

import org.json.JSONArray
import org.json.JSONObject

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

    fun treeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "nodes" to snapshot.nodes.map(::nodeFields)
        )
    }

    fun rawTreeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
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
        val elements = snapshot.nodes
            .asSequence()
            .filter { !editableOnly || it.editable }
            .filter { !scrollableOnly || it.scrollable }
            .filter { !clickableOnly || it.clickable }
            .filter { packageFilter.isNullOrBlank() || snapshot.packageName == packageFilter }
            .map { node ->
                linkedMapOf(
                    "nodeId" to node.nodeId,
                    "text" to (node.text ?: ""),
                    "desc" to (node.contentDesc ?: ""),
                    "id" to (node.resourceId ?: ""),
                    "clickable" to node.clickable,
                    "editable" to node.editable,
                    "scrollable" to node.scrollable,
                    "bounds" to node.bounds.toBracketString(),
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
            "elements" to elements
        )
    }

    fun nodeFields(node: FlatAccessibilityNode): Map<String, Any?> {
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
        payload["bounds"] = node.bounds.toBracketString()
        payload["centerX"] = node.centerX
        payload["centerY"] = node.centerY
        payload["clickable"] = node.clickable
        payload["editable"] = node.editable
        payload["scrollable"] = node.scrollable
        return payload
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
}
