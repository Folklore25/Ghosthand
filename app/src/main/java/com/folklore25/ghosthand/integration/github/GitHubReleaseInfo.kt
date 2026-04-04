/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.integration.github

import com.folklore25.ghosthand.R
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

internal data class InstalledAppVersion(
    val versionName: String,
    val versionCode: Long
)

internal data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkAssetUrl: String?
) {
    val displayVersion: String
        get() = ReleaseVersionComparator.releaseDisplayVersion(tagName, name)
}

internal sealed interface GitHubReleaseCheckResult {
    data class Checking(
        val installedVersion: InstalledAppVersion?
    ) : GitHubReleaseCheckResult

    data class UpToDate(
        val installedVersion: InstalledAppVersion,
        val latestRelease: GitHubReleaseInfo
    ) : GitHubReleaseCheckResult

    data class UpdateAvailable(
        val installedVersion: InstalledAppVersion,
        val latestRelease: GitHubReleaseInfo
    ) : GitHubReleaseCheckResult

    data class Failed(
        val installedVersion: InstalledAppVersion?,
        val reason: String
    ) : GitHubReleaseCheckResult
}

internal object ReleaseVersionComparator {
    fun isUpdateAvailable(
        installedVersion: InstalledAppVersion,
        latestRelease: GitHubReleaseInfo
    ): Boolean {
        val installedParts = normalizeVersion(installedVersion.versionName) ?: return false
        val releaseParts = normalizeVersion(latestRelease.tagName)
            ?: normalizeVersion(latestRelease.name)
            ?: return false

        return compareParts(installedParts, releaseParts) < 0
    }

    fun releaseDisplayVersion(tagName: String, name: String): String {
        return sanitizeVersionLabel(tagName)
            ?: sanitizeVersionLabel(name)
            ?: tagName.ifBlank { name }
    }

    private fun normalizeVersion(raw: String?): List<Int>? {
        val sanitized = sanitizeVersionLabel(raw) ?: return null
        val parts = sanitized.split('.')
            .mapNotNull { token ->
                val digits = token.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
        return parts.takeIf { it.isNotEmpty() }
    }

    private fun compareParts(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val lhs = left.getOrElse(index) { 0 }
            val rhs = right.getOrElse(index) { 0 }
            if (lhs != rhs) {
                return lhs.compareTo(rhs)
            }
        }
        return 0
    }

    private fun sanitizeVersionLabel(raw: String?): String? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val firstDigit = raw.indexOfFirst { it.isDigit() }
        if (firstDigit < 0) {
            return null
        }

        val candidate = raw.substring(firstDigit)
            .takeWhile { it.isDigit() || it == '.' }
            .trim('.')

        return candidate.takeIf { it.isNotBlank() }
    }
}
