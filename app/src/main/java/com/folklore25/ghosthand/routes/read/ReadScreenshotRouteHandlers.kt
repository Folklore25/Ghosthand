/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

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

        return if (screenshotResult.available) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }

    fun buildScreenshotResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/screenshot")
            ?: return badJsonBodyResponse()

        val width = body.optIntOrNull("width") ?: 0
        val height = body.optIntOrNull("height") ?: 0

        val screenshotResult = stateCoordinator.captureBestScreenshot(width, height)
        return if (screenshotResult.available) {
            buildJsonResponse(
                statusCode = 200,
                body = successEnvelope(
                    JSONObject()
                        .put("image", "data:image/png;base64,${screenshotResult.base64 ?: ""}")
                        .put("width", screenshotResult.width)
                        .put("height", screenshotResult.height)
                )
            )
        } else {
            buildJsonResponse(
                statusCode = 503,
                body = errorEnvelope(
                    code = "SCREENSHOT_FAILED",
                    message = "Screenshot capture failed. Reason: ${screenshotResult.attemptedPath}"
                )
            )
        }
    }
}
