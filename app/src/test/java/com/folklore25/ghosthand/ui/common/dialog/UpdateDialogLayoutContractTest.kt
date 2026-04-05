/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.common.dialog

import com.folklore25.ghosthand.TestFileSupport

import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDialogLayoutContractTest {
    @Test
    fun updateDialogLayoutExposesVersionRowsAndDedicatedAction() {
        val layout = TestFileSupport.readProjectFile(
            "app/src/main/res/layout/dialog_update.xml",
            "src/main/res/layout/dialog_update.xml"
        )

        assertTrue(layout.contains("@+id/updateDialogInstalledValue"))
        assertTrue(layout.contains("@+id/updateDialogLatestValue"))
        assertTrue(layout.contains("@+id/updateDialogStatusValue"))
        assertTrue(layout.contains("@+id/updateDialogActionButton"))
    }
}
