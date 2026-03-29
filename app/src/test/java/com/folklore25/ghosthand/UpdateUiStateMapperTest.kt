/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateUiStateMapperTest {
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
    }
}
