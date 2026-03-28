package com.folklore25.ghosthand

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class AccessibilityTreeSnapshotProvider(
    context: Context
) {
    private val foregroundAppProvider = ForegroundAppProvider(context.applicationContext)

    fun snapshot(): AccessibilityTreeSnapshotResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return AccessibilityTreeSnapshotResult.unavailable(
                reason = TreeUnavailableReason.ACCESSIBILITY_SERVICE_DISCONNECTED
            )

        repeat(MAX_SNAPSHOT_ATTEMPTS) { attempt ->
            val foregroundSnapshot = foregroundAppProvider.snapshot()
            val snapshot = service.withActiveWindowRoot { rootNode ->
                val snapshotToken = AccessibilityNodeLocator.snapshotToken(rootNode)
                val nodes = mutableListOf<FlatAccessibilityNode>()
                collectNodes(
                    node = rootNode,
                    destination = nodes,
                    path = listOf(0),
                    snapshotToken = snapshotToken
                )

                val rootPackageName = rootNode.packageName?.toString()
                val packagesAligned = foregroundSnapshot.packageName == null ||
                    rootPackageName == null ||
                    foregroundSnapshot.packageName == rootPackageName

                AccessibilityTreeSnapshot(
                    packageName = rootPackageName ?: foregroundSnapshot.packageName,
                    activity = if (packagesAligned) foregroundSnapshot.activity else null,
                    snapshotToken = snapshotToken,
                    capturedAt = Instant.now().toString(),
                    nodes = nodes
                )
            }

            if (snapshot != null) {
                val packagesAligned = foregroundSnapshot.packageName == null ||
                    snapshot.packageName == null ||
                    foregroundSnapshot.packageName == snapshot.packageName

                if (packagesAligned || attempt == MAX_SNAPSHOT_ATTEMPTS - 1) {
                    return AccessibilityTreeSnapshotResult.available(snapshot)
                }
            }

            if (attempt < MAX_SNAPSHOT_ATTEMPTS - 1) {
                Thread.sleep(SNAPSHOT_RETRY_DELAY_MS)
            }
        }

        return AccessibilityTreeSnapshotResult.unavailable(
            reason = TreeUnavailableReason.NO_ACTIVE_ROOT
        )
    }

    fun toJson(snapshot: AccessibilityTreeSnapshot): JSONObject {
        val nodesJson = JSONArray()
        snapshot.nodes.forEach { node ->
            nodesJson.put(toJson(node))
        }

        return JSONObject()
            .put("packageName", snapshot.packageName ?: JSONObject.NULL)
            .put("activity", snapshot.activity ?: JSONObject.NULL)
            .put("snapshotToken", snapshot.snapshotToken)
            .put("capturedAt", snapshot.capturedAt)
            .put("nodes", nodesJson)
    }

    fun toRawJson(snapshot: AccessibilityTreeSnapshot): JSONObject {
        return JSONObject()
            .put("packageName", snapshot.packageName ?: JSONObject.NULL)
            .put("activity", snapshot.activity ?: JSONObject.NULL)
            .put("snapshotToken", snapshot.snapshotToken)
            .put("capturedAt", snapshot.capturedAt)
            .put(
                "root",
                buildRawTreeNode(
                    snapshot = snapshot,
                    path = listOf(0)
                ) ?: JSONObject.NULL
            )
    }

    fun toScreenJson(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        val elements = JSONArray()
        snapshot.nodes
            .asSequence()
            .filter { !editableOnly || it.editable }
            .filter { !scrollableOnly || it.scrollable }
            .filter { !clickableOnly || it.clickable }
            .filter { packageFilter.isNullOrBlank() || snapshot.packageName == packageFilter }
            .forEach { node ->
                elements.put(
                    JSONObject()
                        .put("nodeId", node.nodeId)
                        .put("text", node.text ?: "")
                        .put("desc", node.contentDesc ?: "")
                        .put("id", node.resourceId ?: "")
                        .put("clickable", node.clickable)
                        .put("editable", node.editable)
                        .put("scrollable", node.scrollable)
                        .put("bounds", node.bounds.toBracketString())
                        .put("centerX", node.centerX)
                        .put("centerY", node.centerY)
                )
            }

        return JSONObject()
            .put("packageName", snapshot.packageName ?: JSONObject.NULL)
            .put("activity", snapshot.activity ?: JSONObject.NULL)
            .put("snapshotToken", snapshot.snapshotToken)
            .put("capturedAt", snapshot.capturedAt)
            .put("elements", elements)
    }

    fun toJson(node: FlatAccessibilityNode): JSONObject {
        return JSONObject()
            .put("nodeId", node.nodeId)
            .put("text", node.text ?: JSONObject.NULL)
            .put("contentDesc", node.contentDesc ?: JSONObject.NULL)
            .put("resourceId", node.resourceId ?: JSONObject.NULL)
            .put("className", node.className ?: JSONObject.NULL)
            .put("clickable", node.clickable)
            .put("editable", node.editable)
            .put("enabled", node.enabled)
            .put("scrollable", node.scrollable)
            .put("centerX", node.centerX)
            .put("centerY", node.centerY)
            .put("bounds", JSONObject()
                .put("left", node.bounds.left)
                .put("top", node.bounds.top)
                .put("right", node.bounds.right)
                .put("bottom", node.bounds.bottom)
            )
    }

    private fun buildRawTreeNode(
        snapshot: AccessibilityTreeSnapshot,
        path: List<Int>
    ): JSONObject? {
        val targetNode = snapshot.nodes.firstOrNull { node ->
            node.pathSegments() == path
        } ?: return null

        val children = JSONArray()
        snapshot.nodes
            .asSequence()
            .filter { candidate ->
                val candidatePath = candidate.pathSegments()
                candidatePath.size == path.size + 1 &&
                    candidatePath.dropLast(1) == path
            }
            .sortedBy { it.pathSegments().lastOrNull() ?: 0 }
            .forEach { child ->
                buildRawTreeNode(snapshot, child.pathSegments())?.let(children::put)
            }

        return toJson(targetNode).put("children", children)
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        destination: MutableList<FlatAccessibilityNode>,
        path: List<Int>,
        snapshotToken: String
    ) {
        destination += node.toFlatNode(
            path = path,
            snapshotToken = snapshotToken
        )

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectNodes(
                node = child,
                destination = destination,
                path = path + index,
                snapshotToken = snapshotToken
            )
        }
    }

    private fun AccessibilityNodeInfo.toFlatNode(
        path: List<Int>,
        snapshotToken: String
    ): FlatAccessibilityNode {
        val boundsRect = Rect()
        getBoundsInScreen(boundsRect)

        return FlatAccessibilityNode(
            nodeId = AccessibilityNodeLocator.createNodeId(path, snapshotToken),
            text = text?.toString(),
            contentDesc = contentDescription?.toString(),
            resourceId = viewIdResourceName,
            className = className?.toString(),
            clickable = isClickable,
            editable = isEditable,
            enabled = isEnabled,
            focused = isFocused,
            scrollable = isScrollable,
            centerX = boundsRect.centerX(),
            centerY = boundsRect.centerY(),
            bounds = NodeBounds(
                left = boundsRect.left,
                top = boundsRect.top,
                right = boundsRect.right,
                bottom = boundsRect.bottom
            )
        )
    }

    private companion object {
        const val MAX_SNAPSHOT_ATTEMPTS = 3
        const val SNAPSHOT_RETRY_DELAY_MS = 100L
    }
}

data class AccessibilityTreeSnapshot(
    val packageName: String?,
    val activity: String?,
    val snapshotToken: String,
    val capturedAt: String,
    val nodes: List<FlatAccessibilityNode>
)

data class AccessibilityTreeSnapshotResult(
    val available: Boolean,
    val snapshot: AccessibilityTreeSnapshot?,
    val reason: TreeUnavailableReason?
) {
    companion object {
        fun available(snapshot: AccessibilityTreeSnapshot): AccessibilityTreeSnapshotResult =
            AccessibilityTreeSnapshotResult(
                available = true,
                snapshot = snapshot,
                reason = null
            )

        fun unavailable(reason: TreeUnavailableReason): AccessibilityTreeSnapshotResult =
            AccessibilityTreeSnapshotResult(
                available = false,
                snapshot = null,
                reason = reason
            )
    }
}

enum class TreeUnavailableReason {
    ACCESSIBILITY_SERVICE_DISCONNECTED,
    NO_ACTIVE_ROOT
}

data class FlatAccessibilityNode(
    val nodeId: String,
    val text: String?,
    val contentDesc: String?,
    val resourceId: String?,
    val className: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val enabled: Boolean,
    val focused: Boolean,
    val scrollable: Boolean,
    val centerX: Int,
    val centerY: Int,
    val bounds: NodeBounds
)

data class NodeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

fun NodeBounds.toBracketString(): String {
    return "[$left,$top][$right,$bottom]"
}

private fun FlatAccessibilityNode.pathSegments(): List<Int> {
    return AccessibilityNodeLocator.pathSegments(nodeId)
}
