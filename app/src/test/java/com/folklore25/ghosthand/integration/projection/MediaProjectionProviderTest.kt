/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.integration.projection

import com.folklore25.ghosthand.TestFileSupport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaProjectionProviderTest {
    @Test
    fun projectionCaptureWaitsForAnActualFrameBeforeReadingImage() {
        val provider = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt",
            "src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt"
        )

        assertTrue(provider.contains("setOnImageAvailableListener"))
    }

    @Test
    fun projectionCaptureDoesNotAcceptEmptyEncodedBytesAsSuccess() {
        val provider = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt",
            "src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt"
        )

        assertFalse(provider.contains("available = true,\n                        base64 = b64"))
    }

    @Test
    fun projectionCaptureDefaultsToDisplayResolutionWhenRequestIsMissing() {
        val provider = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt",
            "src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt"
        )

        assertTrue(provider.contains("val width = if (requestWidth > 0) requestWidth else screenWidth"))
        assertTrue(provider.contains("val height = if (requestHeight > 0) requestHeight else screenHeight"))
    }
}
