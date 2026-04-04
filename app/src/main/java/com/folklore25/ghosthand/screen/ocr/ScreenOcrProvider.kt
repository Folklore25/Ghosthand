/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.ocr

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

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.folklore25.ghosthand.screen.read.ScreenOcrResult
import com.folklore25.ghosthand.screen.read.ScreenReadElement
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun interface ScreenOcrEngine {
    fun recognize(base64Png: String): ScreenOcrResult
}

class ScreenOcrProvider(
    private val engine: ScreenOcrEngine = MlKitScreenOcrEngine()
) {
    fun read(screenshot: ScreenshotDispatchResult): ScreenOcrResult {
        if (!screenshot.available || screenshot.base64.isNullOrBlank()) {
            return ScreenOcrResult(
                elements = emptyList(),
                attemptedPath = screenshot.attemptedPath,
                warnings = listOf("ocr_screenshot_unavailable")
            )
        }

        return engine.recognize(screenshot.base64)
    }
}

class MlKitScreenOcrEngine : ScreenOcrEngine {
    override fun recognize(base64Png: String): ScreenOcrResult {
        val imageBytes = try {
            Base64.decode(base64Png, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            return ScreenOcrResult(
                elements = emptyList(),
                attemptedPath = "ocr_invalid_base64",
                warnings = listOf("ocr_invalid_base64")
            )
        }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return ScreenOcrResult(
                elements = emptyList(),
                attemptedPath = "ocr_decode_failed",
                warnings = listOf("ocr_decode_failed")
            )

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val latch = CountDownLatch(1)
        val successRef = AtomicReference<ScreenOcrResult?>()
        val failureRef = AtomicReference<String?>()

        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { textResult ->
                val elements = textResult.textBlocks.flatMap { block ->
                    block.lines.mapNotNull { line ->
                        val bounds = line.boundingBox ?: return@mapNotNull null
                        val text = line.text.trim()
                        if (text.isEmpty()) {
                            return@mapNotNull null
                        }
                        ScreenReadElement(
                            bounds = "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]",
                            centerX = bounds.centerX(),
                            centerY = bounds.centerY(),
                            source = ScreenReadMode.OCR.wireValue,
                            text = text
                        )
                    }
                }
                successRef.set(
                    ScreenOcrResult(
                        elements = elements,
                        attemptedPath = if (elements.isEmpty()) "ocr_no_text_detected" else "ocr_text_recognition",
                        warnings = if (elements.isEmpty()) listOf("ocr_no_text_detected") else emptyList()
                    )
                )
                latch.countDown()
            }
            .addOnFailureListener { error ->
                failureRef.set(error.javaClass.simpleName)
                latch.countDown()
            }

        latch.await(OCR_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        recognizer.close()

        successRef.get()?.let { return it }
        failureRef.get()?.let { failure ->
            return ScreenOcrResult(
                elements = emptyList(),
                attemptedPath = "ocr_failed",
                warnings = listOf("ocr_failed:$failure")
            )
        }

        return ScreenOcrResult(
            elements = emptyList(),
            attemptedPath = "ocr_timeout",
            warnings = listOf("ocr_timeout")
        )
    }

    private companion object {
        private const val OCR_TIMEOUT_MS = 5000L
    }
}
