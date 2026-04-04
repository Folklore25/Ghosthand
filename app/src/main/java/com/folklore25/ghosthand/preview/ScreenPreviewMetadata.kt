/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.preview

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.screen.read.ScreenReadPayload

object ScreenPreviewMetadata {
    fun previewPath(
        screenshotUsableNow: Boolean,
        previewWidth: Int?,
        previewHeight: Int?
    ): String? {
        if (!screenshotUsableNow || previewWidth == null || previewHeight == null) {
            return null
        }
        return "/screenshot?width=$previewWidth&height=$previewHeight"
    }

    fun apply(
        payload: ScreenReadPayload,
        screenshotUsableNow: Boolean,
        previewWidth: Int?,
        previewHeight: Int?
    ): ScreenReadPayload {
        val advertisedPreviewWidth = previewWidth.takeIf { screenshotUsableNow }
        val advertisedPreviewHeight = previewHeight.takeIf { screenshotUsableNow }
        return payload.copy(
            visualAvailable = screenshotUsableNow,
            previewAvailable = screenshotUsableNow,
            previewPath = previewPath(
                screenshotUsableNow = screenshotUsableNow,
                previewWidth = advertisedPreviewWidth,
                previewHeight = advertisedPreviewHeight
            ),
            previewWidth = advertisedPreviewWidth,
            previewHeight = advertisedPreviewHeight
        )
    }
}
