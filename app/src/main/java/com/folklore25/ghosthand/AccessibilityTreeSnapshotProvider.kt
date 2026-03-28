package com.folklore25.ghosthand

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
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
        return GhosthandApiPayloads.treePayload(snapshot)
    }

    fun toRawJson(snapshot: AccessibilityTreeSnapshot): JSONObject {
        return GhosthandApiPayloads.rawTreePayload(snapshot)
    }

    fun toScreenJson(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        return GhosthandApiPayloads.screenPayload(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    }

    fun toJson(node: FlatAccessibilityNode): JSONObject {
        return GhosthandApiPayloads.nodePayload(node)
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
