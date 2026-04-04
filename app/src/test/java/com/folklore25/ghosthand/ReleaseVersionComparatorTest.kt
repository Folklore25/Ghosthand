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
