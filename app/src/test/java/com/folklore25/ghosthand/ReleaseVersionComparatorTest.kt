/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionComparatorTest {
    @Test
    fun detectsUpdateWhenReleaseVersionIsHigher() {
        assertTrue(
            ReleaseVersionComparator.isUpdateAvailable(
                InstalledAppVersion("1.0.0", 1),
                GitHubReleaseInfo("v1.1.0", "1.1.0", "https://example.com", "2026-03-30T00:00:00Z", null)
            )
        )
    }

    @Test
    fun ignoresPrefixAndNameNoiseWhenRenderingDisplayVersion() {
        assertEquals(
            "1.2.3",
            ReleaseVersionComparator.releaseDisplayVersion("release-v1.2.3", "Release 1.2.3")
        )
    }

    @Test
    fun doesNotReportUpdateWhenInstalledVersionMatchesRelease() {
        assertFalse(
            ReleaseVersionComparator.isUpdateAvailable(
                InstalledAppVersion("1.2.3", 7),
                GitHubReleaseInfo("v1.2.3", "1.2.3", "https://example.com", "2026-03-30T00:00:00Z", null)
            )
        )
    }
}
