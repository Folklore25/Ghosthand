/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenPreviewCoordinatorTest {
    @Test
    fun previewSizingKeepsAspectRatioInsteadOfForcingTinySquareThumbnail() {
        val request = ScreenPreviewCaptureSupport.previewRequestForDisplay(
            displayWidth = 1080,
            displayHeight = 2400
        )

        assertEquals(360, request.width)
        assertEquals(800, request.height)
        assertTrue(request.height > request.width)
    }

    @Test
    fun previewPathUsesBoundedDecisionUsableDimensions() {
        val request = ScreenPreviewCaptureSupport.previewRequestForDisplay(
            displayWidth = 2400,
            displayHeight = 1080
        )

        assertEquals("/screenshot?width=800&height=360", ScreenPreviewMetadata.previewPath(
            screenshotUsableNow = true,
            previewWidth = request.width,
            previewHeight = request.height
        ))
    }
}
