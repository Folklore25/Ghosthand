/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.find

import com.folklore25.ghosthand.AccessibilityNodeFinder
import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.AccessibilityTreeSnapshotProvider
import com.folklore25.ghosthand.FindNodeResult
import org.json.JSONObject

internal class ScreenFindCoordinator(
    private val treeSnapshotProvider: AccessibilityTreeSnapshotProvider,
    private val nodeFinder: AccessibilityNodeFinder
) {
    fun createFindPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int
    ): JSONObject {
        return ScreenFindPayloads.findPayload(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index,
            nodeFinder = nodeFinder
        )
    }

    fun findResult(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int
    ): FindNodeResult {
        return ScreenFindPayloads.findResult(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index,
            nodeFinder = nodeFinder
        )
    }

    fun getFocusedNodeResult(): FocusedNodeResult {
        return ScreenFindPayloads.focusedNodeResult(
            treeResult = treeSnapshotProvider.snapshot(),
            nodeFinder = nodeFinder
        )
    }

    fun createFocusedNodePayload(result: FocusedNodeResult): JSONObject {
        return ScreenFindPayloads.focusedNodePayload(
            result = result,
            nodeToJson = treeSnapshotProvider::toJson
        )
    }
}
