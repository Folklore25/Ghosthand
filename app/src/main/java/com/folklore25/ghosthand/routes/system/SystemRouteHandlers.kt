/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.system

import com.folklore25.ghosthand.catalog.GhosthandCommandCatalog
import com.folklore25.ghosthand.routes.badJsonBodyResponse
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.optIntOrNull
import com.folklore25.ghosthand.routes.parseJsonBodyOrNull
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.routes.toJsonValue
import com.folklore25.ghosthand.server.LocalApiServerRoute
import com.folklore25.ghosthand.state.StateCoordinator
import org.json.JSONArray
import org.json.JSONObject

internal class SystemRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("GET", "/ping") { buildPingResponse() },
            LocalApiServerRoute("GET", "/health") { buildHealthResponse() },
            LocalApiServerRoute("GET", "/commands") { buildCommandsResponse() },
            LocalApiServerRoute("GET", "/state") { buildStateResponse() },
            LocalApiServerRoute("GET", "/device") { buildDeviceResponse() },
            LocalApiServerRoute("GET", "/foreground") { buildForegroundResponse() },
            LocalApiServerRoute("GET", "/clipboard") { buildClipboardReadResponse() },
            LocalApiServerRoute("POST", "/clipboard") { request -> buildClipboardWriteResponse(request.requestBody) },
            LocalApiServerRoute("GET", "/notify") { request -> buildNotifyReadResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/notify") { request -> buildNotifyPostResponse(request.requestBody) },
            LocalApiServerRoute("DELETE", "/notify") { request -> buildNotifyCancelResponse(request.requestBody) }
        )
    }

    private fun buildHealthResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createHealthPayload())
        )
    }

    private fun buildCommandsResponse(): String {
        val commands = JSONArray()
        GhosthandCommandCatalog.commandPayloads().forEach { command ->
            commands.put(toJsonValue(command))
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                JSONObject()
                    .put("schemaVersion", GhosthandCommandCatalog.schemaVersion)
                    .put(
                        "selectorAliases",
                        JSONObject().apply {
                            GhosthandCommandCatalog.selectorAliases.forEach { (alias, strategy) ->
                                put(alias, strategy)
                            }
                        }
                    )
                    .put("selectorStrategies", JSONArray(GhosthandCommandCatalog.selectorStrategies))
                    .put("commands", commands)
            )
        )
    }

    private fun buildPingResponse(): String {
        return buildJsonResponse(200, successEnvelope(stateCoordinator.createPingPayload()))
    }

    private fun buildStateResponse(): String {
        return buildJsonResponse(200, successEnvelope(stateCoordinator.createStatePayload()))
    }

    private fun buildDeviceResponse(): String {
        return buildJsonResponse(200, successEnvelope(stateCoordinator.createDevicePayload()))
    }

    private fun buildForegroundResponse(): String {
        return buildJsonResponse(200, successEnvelope(stateCoordinator.createForegroundPayload()))
    }

    private fun buildClipboardReadResponse(): String {
        val result = stateCoordinator.readClipboard()
        return if (result.available) {
            val payload = JSONObject().put("text", result.text ?: JSONObject.NULL)
            if (result.attemptedPath == "clipboard_cached_after_write") {
                payload.put("reason", result.attemptedPath)
            }
            buildJsonResponse(200, successEnvelope(payload))
        } else {
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject()
                        .put("text", JSONObject.NULL)
                        .put("reason", result.attemptedPath)
                )
            )
        }
    }

    private fun buildClipboardWriteResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/clipboard")
            ?: return badJsonBodyResponse()

        if (!body.has("text")) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }

        val rawText = body.opt("text")
        val text = if (rawText is String) rawText else rawText?.toString() ?: ""
        val result = stateCoordinator.writeClipboard(text)
        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("written", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("CLIPBOARD_WRITE_FAILED", "Clipboard write failed."))
        }
    }

    private fun buildNotifyReadResponse(queryParameters: Map<String, String>): String {
        val excludedPackages = queryParameters["exclude"]
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toSet()
            ?: emptySet()
        return buildJsonResponse(
            200,
            successEnvelope(
                stateCoordinator.readNotifications(
                    packageFilter = queryParameters["package"]?.ifBlank { null },
                    excludedPackages = excludedPackages
                )
            )
        )
    }

    private fun buildNotifyPostResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/notify")
            ?: return badJsonBodyResponse()
        val title = body.optString("title", "").trim()
        val text = body.optString("text", "").trim()
        if (text.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "text is required."))
        }
        val result = stateCoordinator.postNotification(title, text)
        return if (result.performed) {
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject()
                        .put("posted", true)
                        .put("notificationId", result.notificationId ?: JSONObject.NULL)
                )
            )
        } else {
            buildJsonResponse(503, errorEnvelope("NOTIFICATION_FAILED", "Failed to post notification. Reason: ${result.attemptedPath}"))
        }
    }

    private fun buildNotifyCancelResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/notify")
            ?: return badJsonBodyResponse()
        val notificationId = body.optIntOrNull("notificationId")
        if (notificationId == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "notificationId is required."))
        }
        val result = stateCoordinator.cancelNotification(notificationId)
        return if (result.performed) {
            buildJsonResponse(200, successEnvelope(JSONObject().put("canceled", true)))
        } else {
            buildJsonResponse(422, errorEnvelope("NOTIFICATION_CANCEL_FAILED", "Failed to cancel notification."))
        }
    }
}
