/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.read

import com.folklore25.ghosthand.R

import org.json.JSONObject

internal class ScreenSnapshotCoordinator(
    private val treeSnapshotProvider: AccessibilityTreeSnapshotProvider
) {
    fun getTreeSnapshotResult(): AccessibilityTreeSnapshotResult {
        return treeSnapshotProvider.snapshot()
    }

    fun createTreePayload(snapshot: AccessibilityTreeSnapshot, mode: String): JSONObject {
        return if (mode == "raw") {
            treeSnapshotProvider.toRawJson(snapshot)
        } else {
            treeSnapshotProvider.toJson(snapshot)
        }
    }

    fun createScreenPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject {
        return treeSnapshotProvider.toScreenJson(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    }
}
