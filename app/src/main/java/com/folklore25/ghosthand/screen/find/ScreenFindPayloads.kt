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

import com.folklore25.ghosthand.payload.GhosthandPayloadJsonSupport
import com.folklore25.ghosthand.payload.GhosthandScreenPayloads
import org.json.JSONObject

data class FocusedNodeResult(
    val available: Boolean,
    val node: FlatAccessibilityNode?,
    val reason: String?
)

object ScreenFindPayloads {
    fun findPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int,
        nodeFinder: AccessibilityNodeFinder
    ): JSONObject {
        return GhosthandPayloadJsonSupport.fieldsToJson(
            GhosthandScreenPayloads.findFields(
                findResult(
                    snapshot = snapshot,
                    strategy = strategy,
                    query = query,
                    clickableOnly = clickableOnly,
                    index = index,
                    nodeFinder = nodeFinder
                )
            )
        )
    }

    fun findResult(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int,
        nodeFinder: AccessibilityNodeFinder
    ): FindNodeResult {
        return nodeFinder.findNodes(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
    }

    fun focusedNodeResult(
        treeResult: AccessibilityTreeSnapshotResult,
        nodeFinder: AccessibilityNodeFinder
    ): FocusedNodeResult {
        if (!treeResult.available || treeResult.snapshot == null) {
            return FocusedNodeResult(
                available = false,
                node = null,
                reason = "accessibility_unavailable"
            )
        }

        val found = nodeFinder.findNode(
            snapshot = treeResult.snapshot,
            strategy = "focused",
            query = null
        )

        return FocusedNodeResult(
            available = true,
            node = found.node,
            reason = null
        )
    }

    fun focusedNodePayload(
        result: FocusedNodeResult,
        nodeToJson: (FlatAccessibilityNode) -> JSONObject
    ): JSONObject {
        return JSONObject()
            .put("available", result.available)
            .put("node", result.node?.let(nodeToJson) ?: JSONObject.NULL)
            .put("reason", result.reason ?: JSONObject.NULL)
    }
}
