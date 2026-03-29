/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsSurfaceLayoutContractTest {
    @Test
    fun permissionsLayoutKeepsOnlyAccessibilityAndScreenshotGovernanceSwitches() {
        val layout = TestFileSupport.readProjectFile(
            "app/src/main/res/layout/activity_permissions.xml",
            "src/main/res/layout/activity_permissions.xml"
        )

        assertTrue(layout.contains("@+id/accessibilityPolicySwitch"))
        assertTrue(layout.contains("@+id/screenshotPolicySwitch"))
        assertFalse(layout.contains("@+id/rootPolicySwitch"))
        assertFalse(layout.contains("@+id/rootAuthorizeButton"))
    }

    @Test
    fun permissionsActivityBindsScreenStateAndDoesNotInstantiateStoreDirectly() {
        val activity = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/PermissionsActivity.kt",
            "src/main/java/com/folklore25/ghosthand/PermissionsActivity.kt"
        )
        val strings = TestFileSupport.readProjectFile(
            "app/src/main/res/values/strings.xml",
            "src/main/res/values/strings.xml"
        )

        assertTrue(activity.contains("PermissionsScreenUiState"))
        assertFalse(activity.contains("CapabilityPolicyStore("))
        assertTrue(strings.contains("permissions_layer_note_v2"))
    }
}
