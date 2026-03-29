/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplanationContentTest {
    @Test
    fun explanationCatalogCoversRelevantModules() {
        assertEquals(
            setOf(
                ModuleExplanation.Update,
                ModuleExplanation.Runtime,
                ModuleExplanation.Permissions,
                ModuleExplanation.Accessibility,
                ModuleExplanation.Screenshot,
                ModuleExplanation.Diagnostics
            ),
            ModuleExplanation.entries.toSet()
        )
    }

    @Test
    fun explanationStringsExistForEachModule() {
        val strings = TestFileSupport.readProjectFile(
            "app/src/main/res/values/strings.xml",
            "src/main/res/values/strings.xml"
        )

        listOf(
            "explanation_update_body",
            "explanation_runtime_body",
            "explanation_permissions_body",
            "explanation_accessibility_body",
            "explanation_screenshot_body",
            "explanation_diagnostics_body"
        ).forEach { key ->
            assertTrue(strings.contains("name=\"$key\""))
        }
    }
}
