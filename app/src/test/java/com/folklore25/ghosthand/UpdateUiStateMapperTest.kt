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
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateUiStateMapperTest {
    @Test
    fun checkingStateKeepsInstalledVersionVisible() {
        val uiState = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.Checking(
                installedVersion = InstalledAppVersion(versionName = "1.0", versionCode = 1)
            )
        )

        assertEquals(UpdateStatus.CHECKING, uiState.status)
        assertEquals("1.0", uiState.installedVersionText)
        assertNull(uiState.latestReleaseText)
    }

    @Test
    fun installedVersionMatchingLatestReleaseMapsToUpToDateWithoutInstallCta() {
        val uiState = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.UpToDate(
                installedVersion = InstalledAppVersion(versionName = "1.0", versionCode = 1),
                latestRelease = GitHubReleaseInfo(
                    tagName = "v1.0.0",
                    name = "1.0 release",
                    htmlUrl = "https://github.com/folklore25/ghosthand/releases/tag/v1.0.0",
                    publishedAt = "2026-03-29T00:00:00Z",
                    apkAssetUrl = null
                )
            )
        )

        assertEquals(UpdateStatus.UP_TO_DATE, uiState.status)
        assertNull(uiState.actionUrl)
    }

    @Test
    fun olderInstalledVersionMapsToGithubReleaseHandoff() {
        val uiState = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.UpdateAvailable(
                installedVersion = InstalledAppVersion(versionName = "1.0", versionCode = 1),
                latestRelease = GitHubReleaseInfo(
                    tagName = "v1.1.0",
                    name = "1.1 release",
                    htmlUrl = "https://github.com/folklore25/ghosthand/releases/tag/v1.1.0",
                    publishedAt = "2026-03-30T00:00:00Z",
                    apkAssetUrl = "https://github.com/folklore25/ghosthand/releases/download/v1.1.0/app-release.apk"
                )
            )
        )

        assertEquals(UpdateStatus.UPDATE_AVAILABLE, uiState.status)
        assertEquals(
            "https://github.com/folklore25/ghosthand/releases/tag/v1.1.0",
            uiState.actionUrl
        )
    }

    @Test
    fun fetchOrParseFailureMapsToCheckFailedWithoutPretendInstallPath() {
        val uiState = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.Failed(
                installedVersion = InstalledAppVersion(versionName = "1.0", versionCode = 1),
                reason = "Unable to read latest release metadata."
            )
        )

        assertEquals(UpdateStatus.CHECK_FAILED, uiState.status)
        assertNull(uiState.actionUrl)
        assertFalse(uiState.installedVersionText.isBlank())
    }
}
