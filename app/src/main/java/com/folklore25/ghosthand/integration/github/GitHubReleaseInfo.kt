/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.integration.github

import com.folklore25.ghosthand.R

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
