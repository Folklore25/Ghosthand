/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen

import com.folklore25.ghosthand.ForegroundAppSnapshot
import com.folklore25.ghosthand.assessSnapshotFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTreeSnapshotProviderTest {
    @Test
    fun acceptsStableAlignedForegroundCapture() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = false
        )

        assertTrue(assessment.acceptSnapshot)
        assertTrue(assessment.foregroundStableDuringCapture)
        assertEquals("SettingsActivity", assessment.activity)
        assertTrue(assessment.warnings.isEmpty())
    }

    @Test
    fun retriesWhenForegroundChangesDuringCapture() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.miui.home",
                activity = "Launcher"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = false
        )

        assertFalse(assessment.acceptSnapshot)
        assertFalse(assessment.foregroundStableDuringCapture)
        assertEquals(null, assessment.activity)
        assertTrue(assessment.warnings.contains("foreground_changed_during_capture"))
        assertTrue(assessment.warnings.contains("surface_package_mismatch"))
    }

    @Test
    fun finalAttemptReturnsSnapshotWithFreshnessWarning() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "NextActivity"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = true
        )

        assertTrue(assessment.acceptSnapshot)
        assertFalse(assessment.foregroundStableDuringCapture)
        assertEquals(null, assessment.activity)
        assertEquals(listOf("foreground_changed_during_capture"), assessment.warnings)
    }
}
