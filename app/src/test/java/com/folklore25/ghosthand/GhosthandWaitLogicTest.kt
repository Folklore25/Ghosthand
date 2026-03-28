package com.folklore25.ghosthand

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
