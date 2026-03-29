/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CapabilityPolicyStoreTest {
    @Test
    fun preservesAccessibilityAndScreenshotPolicyBooleans() {
        val tempFile = Files.createTempFile("ghosthand-policy", ".preferences_pb").toFile()
        val store = CapabilityPolicyStore(
            dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { tempFile }
            )
        )

        store.setAllowed(GhosthandCapability.Accessibility, true)
        store.setAllowed(GhosthandCapability.Screenshot, false)

        val snapshot = store.snapshot()
        assertTrue(snapshot.accessibilityAllowed)
        assertFalse(snapshot.screenshotAllowed)
    }

    @Test
    fun migratedSnapshotUsesOnlySupportedLegacyKeys() {
        val migrated = CapabilityPolicyStore.migratedSnapshot(
            legacyPreferences = mapOf(
                "capability.accessibility" to true,
                "capability.screenshot" to true,
                "capability.root" to false
            )
        )

        assertEquals(
            CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = true
            ),
            migrated
        )
    }
}
