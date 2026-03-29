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
    fun homeLayoutHasNoRootEntryAndNoInlineRootSummary() {
        val layout = TestFileSupport.readProjectFile(
            "app/src/main/res/layout/activity_main.xml",
            "src/main/res/layout/activity_main.xml"
        )

        assertFalse(layout.contains("@+id/rootEntryButton"))
        assertFalse(layout.contains("@+id/homeRootSummaryValue"))
        assertFalse(layout.contains("@+id/homeRootEffectiveValue"))
        assertFalse(layout.contains("@+id/homeGithubButton"))
        assertFalse(layout.contains("@+id/homeUpdateCard"))
        assertFalse(layout.contains("@+id/homeUpdateInfoButton"))
        assertFalse(layout.contains("@+id/homeEyebrow"))
        assertTrue(layout.contains("@+id/homeUpdateButton"))
        assertTrue(layout.contains("@+id/homeRuntimeSignalsStack"))
        assertTrue(layout.contains("@+id/homeRuntimeInfoButton"))
        assertTrue(layout.contains("@+id/homePermissionsInfoButton"))
        assertTrue(layout.contains("@+id/homeDiagnosticsInfoButton"))
        assertTrue(layout.contains("@+id/openPermissionsButton"))
        assertTrue(layout.contains("@+id/openDiagnosticsButton"))
    }
}
