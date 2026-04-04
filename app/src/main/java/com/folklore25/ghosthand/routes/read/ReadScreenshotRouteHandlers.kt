/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.R

import com.folklore25.ghosthand.interaction.execution.ScreenshotDispatchResult
import com.folklore25.ghosthand.interaction.execution.hasUsableImage
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONObject

internal class ReadScreenshotRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun buildScreenshotGetResponse(queryParameters: Map<String, String>): String {
        val width = queryParameters["width"]?.toIntOrNull() ?: 0
        val height = queryParameters["height"]?.toIntOrNull() ?: 0
        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)

        return buildScreenshotResponseFor(screenshotResult)
    }

    fun buildScreenshotResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/screenshot")
            ?: return badJsonBodyResponse()

        val width = body.optIntOrNull("width") ?: 0
        val height = body.optIntOrNull("height") ?: 0

        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)
        return buildScreenshotResponseFor(screenshotResult)
    }

    private fun buildScreenshotResponseFor(screenshotResult: ScreenshotDispatchResult): String {
        return if (screenshotResult.hasUsableImage) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            val failure = screenshotResult.classifyFailure()
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = failure.code,
                    message = failure.message
                )
            )
        }
    }
}

internal data class ScreenshotFailureClassification(
    val code: String,
    val message: String
)

internal fun ScreenshotDispatchResult.classifyFailure(): ScreenshotFailureClassification {
    return when {
        attemptedPath == "invalid_resize_request" -> ScreenshotFailureClassification(
            code = "INVALID_SCREENSHOT_DIMENSIONS",
            message = "Screenshot request dimensions are invalid. Provide both width and height or neither."
        )
        attemptedPath in setOf("service_missing", "service_disconnected", "not_supported", "screenshot_capability_unavailable") ->
            ScreenshotFailureClassification(
                code = "SCREENSHOT_UNAVAILABLE",
                message = "Screenshot capability is not currently available."
            )
        attemptedPath == "projection_missing" -> ScreenshotFailureClassification(
            code = "SCREENSHOT_PROJECTION_UNAVAILABLE",
            message = "MediaProjection session is not active for screenshot capture."
        )
        attemptedPath == "root_unavailable" -> ScreenshotFailureClassification(
            code = "SCREENSHOT_ACTIVITY_UNAVAILABLE",
            message = "No active window root was available during screenshot capture."
        )
        attemptedPath == "image_acquire_timeout" -> ScreenshotFailureClassification(
            code = "SCREENSHOT_FRAME_TIMEOUT",
            message = "Screenshot capture timed out while waiting for a real frame."
        )
        attemptedPath in setOf("bitmap_wrap_failed", "bitmap_prepare_failed", "screenshot_exception") ||
            attemptedPath.startsWith("screenshot_failure_") ||
            attemptedPath.startsWith("capture_exception_") ->
            ScreenshotFailureClassification(
                code = "SCREENSHOT_ENCODE_FAILED",
                message = "Screenshot capture failed while preparing or encoding image data."
            )
        attemptedPath == "empty_encoded_image" -> ScreenshotFailureClassification(
            code = "SCREENSHOT_EMPTY_OUTPUT",
            message = "Screenshot capture produced no image bytes."
        )
        else -> ScreenshotFailureClassification(
            code = "SCREENSHOT_CAPTURE_FAILED",
            message = "Screenshot capture failed. Reason: $attemptedPath"
        )
    }
}
