/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSurfaceLayoutContractTest {
    @Test
    fun homeLayoutKeepsSingleBottomRootEntryAndRemovesInlineRootSummary() {
        val layout = TestFileSupport.readProjectFile(
            "app/src/main/res/layout/activity_main.xml",
            "src/main/res/layout/activity_main.xml"
        )

        assertTrue(layout.contains("@+id/rootEntryButton"))
        assertFalse(layout.contains("@+id/homeRootSummaryValue"))
        assertFalse(layout.contains("@+id/homeRootEffectiveValue"))
        assertTrue(layout.contains("@+id/openPermissionsButton"))
        assertTrue(layout.contains("@+id/openDiagnosticsButton"))
    }

    @Test
    fun homeContractKeepsAdvancedRootNavigationAndDangerColorToken() {
        val activity = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/MainActivity.kt",
            "src/main/java/com/folklore25/ghosthand/MainActivity.kt"
        )
        val colors = TestFileSupport.readProjectFile(
            "app/src/main/res/values/colors.xml",
            "src/main/res/values/colors.xml"
        )

        assertTrue(activity.contains("PermissionsActivity.createRootIntent"))
        assertTrue(colors.contains("gh_danger_red"))
    }
}
