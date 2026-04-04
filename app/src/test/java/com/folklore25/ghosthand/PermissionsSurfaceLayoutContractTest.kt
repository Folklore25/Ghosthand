/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

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
