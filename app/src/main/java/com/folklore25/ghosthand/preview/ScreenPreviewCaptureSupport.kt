/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.preview

import com.folklore25.ghosthand.AccessibilityTreeSnapshot
import com.folklore25.ghosthand.ForegroundAppSnapshot
import com.folklore25.ghosthand.ScreenshotDispatchResult
import com.folklore25.ghosthand.screen.read.ScreenReadPayload

object ScreenPreviewCaptureSupport {
    fun previewTokenForSnapshot(snapshot: AccessibilityTreeSnapshot): String? {
        return snapshot.snapshotToken.let { "preview:$it" }
    }

    fun previewTokenForForeground(foregroundSnapshot: ForegroundAppSnapshot): String? {
        return foregroundSnapshot.packageName?.let {
            "preview:$it:${foregroundSnapshot.activity ?: "unknown"}"
        }
    }

    fun withPreviewMetadata(
        payload: ScreenReadPayload,
        screenshotUsableNow: Boolean,
        previewToken: String?,
        previewWidth: Int,
        previewHeight: Int
    ): ScreenReadPayload {
        return ScreenPreviewMetadata.apply(
            payload = payload,
            screenshotUsableNow = screenshotUsableNow,
            previewToken = previewToken,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }

    fun withPreviewImage(
        payload: ScreenReadPayload,
        screenshotResult: ScreenshotDispatchResult
    ): ScreenReadPayload {
        if (!screenshotResult.available || screenshotResult.base64.isNullOrBlank()) {
            return payload
        }

        return payload.copy(
            previewWidth = screenshotResult.width,
            previewHeight = screenshotResult.height,
            previewImage = "data:image/png;base64,${screenshotResult.base64}"
        )
    }
}
