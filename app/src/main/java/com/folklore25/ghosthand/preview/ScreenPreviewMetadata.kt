/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.preview

import com.folklore25.ghosthand.ScreenReadPayload

object ScreenPreviewMetadata {
    fun apply(
        payload: ScreenReadPayload,
        screenshotUsableNow: Boolean,
        previewToken: String?,
        previewWidth: Int,
        previewHeight: Int
    ): ScreenReadPayload {
        return payload.copy(
            visualAvailable = screenshotUsableNow,
            previewAvailable = screenshotUsableNow,
            previewToken = previewToken,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )
    }
}
