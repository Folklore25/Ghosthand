/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchProviderTest {
    @Test
    fun returnsPackageNotInstalledWhenPackageIsMissing() {
        val provider = AppLaunchProvider(
            isInstalled = { false },
            resolveLabel = { null },
            resolveLaunchIntent = { null },
            startActivity = { error("should not launch") }
        )

        val result = provider.launch("com.example.missing")

        assertFalse(result.launched)
        assertEquals("package_not_installed", result.reason)
        assertEquals("com.example.missing", result.packageName)
    }

    @Test
    fun returnsLaunchIntentUnavailableWhenInstalledPackageHasNoLauncherIntent() {
        val provider = AppLaunchProvider(
            isInstalled = { true },
            resolveLabel = { "Example" },
            resolveLaunchIntent = { null },
            startActivity = { error("should not launch") }
        )

        val result = provider.launch("com.example.headless")

        assertFalse(result.launched)
        assertEquals("launch_intent_unavailable", result.reason)
        assertEquals("Example", result.label)
    }

    @Test
    fun launchesInstalledPackageThroughPackageLaunchIntentStrategy() {
        val provider = AppLaunchProvider(
            isInstalled = { true },
            resolveLabel = { "Settings" },
            resolveLaunchIntent = { Intent(Intent.ACTION_MAIN) },
            startActivity = { }
        )

        val result = provider.launch("com.android.settings")

        assertTrue(result.launched)
        assertEquals("package_launch_intent", result.strategy)
        assertEquals("launched", result.reason)
        assertEquals("Settings", result.label)
    }
}
