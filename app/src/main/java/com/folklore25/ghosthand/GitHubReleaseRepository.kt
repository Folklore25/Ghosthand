/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal class GitHubReleaseRepository(
    private val context: Context,
    private val latestReleaseEndpoint: String = DEFAULT_LATEST_RELEASE_ENDPOINT,
    private val openConnection: (String) -> HttpsURLConnection = { endpoint ->
        URL(endpoint).openConnection() as HttpsURLConnection
    }
) {
    fun checkForUpdate(): GitHubReleaseCheckResult {
        val installedVersion = installedAppVersion()
            ?: return GitHubReleaseCheckResult.Failed(
                installedVersion = null,
                reason = "Installed version is unavailable."
            )

        return try {
            val latestRelease = fetchLatestRelease()
            if (ReleaseVersionComparator.isUpdateAvailable(installedVersion, latestRelease)) {
                GitHubReleaseCheckResult.UpdateAvailable(installedVersion, latestRelease)
            } else {
                GitHubReleaseCheckResult.UpToDate(installedVersion, latestRelease)
            }
        } catch (error: Exception) {
            Log.w(
                LOG_TAG,
                "component=GitHubReleaseRepository operation=checkForUpdate endpoint=$latestReleaseEndpoint failure=${error.javaClass.simpleName}",
                error
            )
            GitHubReleaseCheckResult.Failed(
                installedVersion = installedVersion,
                reason = "Unable to read latest release metadata."
            )
        }
    }

    fun installedAppVersion(): InstalledAppVersion? {
        return try {
            val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
            InstalledAppVersion(
                versionName = packageInfo.versionName ?: return null,
                versionCode = packageInfo.longVersionCode
            )
        } catch (error: Exception) {
            Log.w(
                LOG_TAG,
                "component=GitHubReleaseRepository operation=installedAppVersion package=${context.packageName} failure=${error.javaClass.simpleName}",
                error
            )
            null
        }
    }

    private fun fetchLatestRelease(): GitHubReleaseInfo {
        val connection = openConnection(latestReleaseEndpoint)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = NETWORK_TIMEOUT_MS
            connection.readTimeout = NETWORK_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Ghosthand-Android")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("GitHub latest release request failed")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseRelease(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(body: String): GitHubReleaseInfo {
        val json = JSONObject(body)
        val assets = json.optJSONArray("assets")
        val apkAssetUrl = (0 until (assets?.length() ?: 0))
            .asSequence()
            .mapNotNull { index -> assets?.optJSONObject(index) }
            .firstOrNull { asset ->
                asset.optString("name").endsWith(".apk", ignoreCase = true)
            }
            ?.optString("browser_download_url")
            ?.takeIf { it.isNotBlank() }

        return GitHubReleaseInfo(
            tagName = json.getString("tag_name"),
            name = json.optString("name"),
            htmlUrl = json.getString("html_url"),
            publishedAt = json.optString("published_at"),
            apkAssetUrl = apkAssetUrl
        )
    }

    private companion object {
        private const val NETWORK_TIMEOUT_MS = 5_000
        private const val DEFAULT_LATEST_RELEASE_ENDPOINT =
            "https://api.github.com/repos/folklore25/ghosthand/releases/latest"
        private const val LOG_TAG = "GitHubReleaseRepo"
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoCompat(packageName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        getPackageInfo(packageName, 0)
    }
