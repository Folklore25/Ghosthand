/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.diagnostics

import com.folklore25.ghosthand.integration.github.GitHubReleaseCheckResult

import com.folklore25.ghosthand.R

internal enum class UpdateStatus {
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CHECK_FAILED
}

internal data class UpdateUiState(
    val status: UpdateStatus,
    val installedVersionText: String,
    val latestReleaseText: String?,
    val failureReason: String? = null,
    val actionUrl: String?
)

internal object UpdateUiStateFactory {
    fun fromReleaseCheck(result: GitHubReleaseCheckResult): UpdateUiState {
        return when (result) {
            is GitHubReleaseCheckResult.Checking -> UpdateUiState(
                status = UpdateStatus.CHECKING,
                installedVersionText = result.installedVersion?.versionName.orEmpty(),
                latestReleaseText = null,
                actionUrl = null
            )

            is GitHubReleaseCheckResult.UpToDate -> UpdateUiState(
                status = UpdateStatus.UP_TO_DATE,
                installedVersionText = result.installedVersion.versionName,
                latestReleaseText = result.latestRelease.displayVersion,
                actionUrl = null
            )

            is GitHubReleaseCheckResult.UpdateAvailable -> UpdateUiState(
                status = UpdateStatus.UPDATE_AVAILABLE,
                installedVersionText = result.installedVersion.versionName,
                latestReleaseText = result.latestRelease.displayVersion,
                actionUrl = result.latestRelease.htmlUrl
            )

            is GitHubReleaseCheckResult.Failed -> UpdateUiState(
                status = UpdateStatus.CHECK_FAILED,
                installedVersionText = result.installedVersion?.versionName.orEmpty(),
                latestReleaseText = null,
                failureReason = result.reason,
                actionUrl = null
            )
        }
    }
}
