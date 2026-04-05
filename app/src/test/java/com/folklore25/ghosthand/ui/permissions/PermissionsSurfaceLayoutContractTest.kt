/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.permissions

import com.folklore25.ghosthand.TestFileSupport
import com.folklore25.ghosthand.capability.CapabilityPolicyStore
import com.folklore25.ghosthand.ui.common.dialog.ModuleExplanationDialogFragment

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
        assertTrue(layout.contains("@+id/permissionsInfoButton"))
        assertTrue(layout.contains("@+id/accessibilityInfoButton"))
        assertTrue(layout.contains("@+id/screenshotInfoButton"))
        assertFalse(layout.contains("@+id/permissionsBackButton"))
        assertFalse(layout.contains("@android:drawable/ic_dialog_info"))
        assertFalse(layout.contains("@+id/rootPolicySwitch"))
        assertFalse(layout.contains("@+id/rootAuthorizeButton"))
    }

    @Test
    fun permissionsActivityBindsScreenStateAndDoesNotInstantiateStoreDirectly() {
        val activity = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/ui/permissions/PermissionsActivity.kt",
            "src/main/java/com/folklore25/ghosthand/ui/permissions/PermissionsActivity.kt"
        )
        val strings = TestFileSupport.readProjectFile(
            "app/src/main/res/values/strings.xml",
            "src/main/res/values/strings.xml"
        )

        assertTrue(activity.contains("PermissionsScreenUiState"))
        assertFalse(activity.contains("CapabilityPolicyStore("))
        assertTrue(activity.contains("ModuleExplanationDialogFragment.show"))
        assertTrue(strings.contains("permissions_layer_note"))
        assertFalse(strings.contains("permissions_layer_note_v2"))
    }
}
