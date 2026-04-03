/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.wait

import com.folklore25.ghosthand.wait.GhosthandWaitLogic
import com.folklore25.ghosthand.wait.UiStateSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandWaitLogicTest {
    @Test
    fun detectsForegroundPackageChange() {
        val initial = UiStateSnapshot("snap1", "com.example.one", "A")
        val current = UiStateSnapshot("snap1", "com.example.two", "A")
        assertTrue(GhosthandWaitLogic.hasUiChanged(initial, current))
    }

    @Test
    fun detectsActivityChange() {
        val initial = UiStateSnapshot("snap1", "com.example", "A")
        val current = UiStateSnapshot("snap1", "com.example", "B")
        assertTrue(GhosthandWaitLogic.hasUiChanged(initial, current))
    }

    @Test
    fun detectsSnapshotTokenChangeWhenForegroundIsSame() {
        val initial = UiStateSnapshot("snap1", "com.example", "A")
        val current = UiStateSnapshot("snap2", "com.example", "A")
        assertTrue(GhosthandWaitLogic.hasUiChanged(initial, current))
    }

    @Test
    fun ignoresNullSnapshotTokenWhenForegroundIsUnchanged() {
        val initial = UiStateSnapshot("snap1", "com.example", "A")
        val current = UiStateSnapshot(null, "com.example", "A")
        assertFalse(GhosthandWaitLogic.hasUiChanged(initial, current))
    }

    @Test
    fun returnsFalseWhenNothingChanges() {
        val initial = UiStateSnapshot("snap1", "com.example", "A")
        val current = UiStateSnapshot("snap1", "com.example", "A")
        assertFalse(GhosthandWaitLogic.hasUiChanged(initial, current))
    }
}
