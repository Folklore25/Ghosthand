/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
            val foregroundBefore = foregroundAppProvider.snapshot()
            val capturedTree = service.withActiveWindowRoot { rootNode ->
                val snapshotToken = AccessibilityNodeLocator.snapshotToken(rootNode)
                val nodes = mutableListOf<FlatAccessibilityNode>()
                collectNodes(
                    node = rootNode,
                    destination = nodes,
                    path = listOf(0),
                    snapshotToken = snapshotToken
                )

                CapturedTree(
                    rootPackageName = rootNode.packageName?.toString(),
                    snapshotToken = snapshotToken,
                    capturedAt = Instant.now().toString(),
                    nodes = nodes
                )
            }
            val foregroundAfter = foregroundAppProvider.snapshot()

            if (capturedTree != null) {
                val freshness = assessSnapshotFreshness(
                    foregroundBefore = foregroundBefore,
                    foregroundAfter = foregroundAfter,
                    rootPackageName = capturedTree.rootPackageName,
                    finalAttempt = attempt == MAX_SNAPSHOT_ATTEMPTS - 1
                )
                val snapshot = AccessibilityTreeSnapshot(
                    packageName = capturedTree.rootPackageName ?: foregroundAfter.packageName,
                    activity = freshness.activity,
                    snapshotToken = capturedTree.snapshotToken,
                    capturedAt = capturedTree.capturedAt,
                    nodes = capturedTree.nodes,
                    foregroundStableDuringCapture = freshness.foregroundStableDuringCapture,
                    freshnessWarnings = freshness.warnings
                )

                if (freshness.acceptSnapshot) {
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

    private data class CapturedTree(
        val rootPackageName: String?,
        val snapshotToken: String,
        val capturedAt: String,
        val nodes: List<FlatAccessibilityNode>
    )
}

internal fun assessSnapshotFreshness(
    foregroundBefore: ForegroundAppSnapshot,
    foregroundAfter: ForegroundAppSnapshot,
    rootPackageName: String?,
    finalAttempt: Boolean
): SnapshotFreshnessAssessment {
    val foregroundStableDuringCapture =
        foregroundBefore.packageName == foregroundAfter.packageName &&
            foregroundBefore.activity == foregroundAfter.activity
    val packagesAligned =
        foregroundAfter.packageName == null ||
            rootPackageName == null ||
            foregroundAfter.packageName == rootPackageName

    val warnings = buildList {
        if (!foregroundStableDuringCapture) {
            add("foreground_changed_during_capture")
        }
        if (!packagesAligned) {
            add("surface_package_mismatch")
        }
    }

    return SnapshotFreshnessAssessment(
        acceptSnapshot = (foregroundStableDuringCapture && packagesAligned) || finalAttempt,
        activity = if (foregroundStableDuringCapture && packagesAligned) foregroundAfter.activity else null,
        foregroundStableDuringCapture = foregroundStableDuringCapture,
        warnings = warnings
    )
}

internal data class SnapshotFreshnessAssessment(
    val acceptSnapshot: Boolean,
    val activity: String?,
    val foregroundStableDuringCapture: Boolean,
    val warnings: List<String>
)

data class AccessibilityTreeSnapshot(
    val packageName: String?,
    val activity: String?,
    val snapshotToken: String,
    val capturedAt: String,
    val nodes: List<FlatAccessibilityNode>,
    val foregroundStableDuringCapture: Boolean = true,
    val freshnessWarnings: List<String> = emptyList()
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
