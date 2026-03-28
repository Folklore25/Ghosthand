/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.graphics.BitmapFactory
import android.util.Base64
import java.io.BufferedReader
import java.io.IOException
import java.nio.charset.StandardCharsets

class RootScreenshotProvider {
    fun captureScreenshot(): ScreenshotDispatchResult {
        for (suPath in SU_CANDIDATES) {
            try {
                val process = ProcessBuilder(suPath, "-c", SCREENSHOT_COMMAND)
                    .redirectErrorStream(false)
                    .start()
                val stdout = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText).trim()
                val stderr = process.errorStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText).trim()
                val exitCode = process.waitFor()

                if (exitCode != 0 || stdout.isBlank()) {
                    continue
                }

                val pngBytes = Base64.decode(stdout, Base64.DEFAULT)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, options)

                return ScreenshotDispatchResult(
                    available = true,
                    base64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP),
                    format = "png",
                    width = options.outWidth.coerceAtLeast(0),
                    height = options.outHeight.coerceAtLeast(0),
                    attemptedPath = "root_screencap"
                )
            } catch (_: IOException) {
                continue
            }
        }

        return ScreenshotDispatchResult(
            available = false,
            base64 = null,
            format = "png",
            width = 0,
            height = 0,
            attemptedPath = "root_screencap_unavailable"
        )
    }

    private companion object {
        val SU_CANDIDATES = listOf(
            "/apex/com.android.runtime/bin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "su"
        )
        const val TEMP_FILE = "/data/local/tmp/ghosthand-screen.png"
        const val SCREENSHOT_COMMAND =
            "screencap -p " + TEMP_FILE +
                " && base64 " + TEMP_FILE + " | tr -d '\\n' && rm -f " + TEMP_FILE
    }
}
