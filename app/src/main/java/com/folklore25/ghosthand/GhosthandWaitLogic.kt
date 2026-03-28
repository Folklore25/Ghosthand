/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class UiStateSnapshot(
    val snapshotToken: String?,
    val packageName: String?,
    val activity: String?
)

object GhosthandWaitLogic {
    fun hasUiChanged(
        initial: UiStateSnapshot,
        current: UiStateSnapshot
    ): Boolean {
        val foregroundChanged =
            current.packageName != initial.packageName ||
                current.activity != initial.activity

        val treeChanged =
            current.snapshotToken != null &&
                initial.snapshotToken != null &&
                current.snapshotToken != initial.snapshotToken

        return foregroundChanged || treeChanged
    }
}
