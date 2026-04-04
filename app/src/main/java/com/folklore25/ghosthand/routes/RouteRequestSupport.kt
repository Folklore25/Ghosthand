/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

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

import android.util.Log
import com.folklore25.ghosthand.payload.GhosthandDisclosure
import com.folklore25.ghosthand.screen.read.ScreenReadMode
import com.folklore25.ghosthand.server.LocalApiServerEnvelope
import com.folklore25.ghosthand.server.LocalApiServer
import org.json.JSONException
import org.json.JSONObject

internal fun successEnvelope(data: JSONObject, disclosure: GhosthandDisclosure? = null): JSONObject {
    return LocalApiServerEnvelope.success(data, disclosure)
}

internal fun errorEnvelope(
    code: String,
    message: String,
    details: JSONObject = JSONObject(),
    disclosure: GhosthandDisclosure? = null
): JSONObject {
    return LocalApiServerEnvelope.error(code, message, details, disclosure)
}

internal fun buildJsonResponse(statusCode: Int, body: JSONObject): String {
    return LocalApiServerEnvelope.httpResponse(statusCode, body)
}

internal fun toJsonValue(value: Any?): Any {
    return LocalApiServerEnvelope.toJsonValue(value)
}

internal fun parseSelector(body: JSONObject): SelectorQuery? {
    val query = body.opt("query").let { value ->
        when {
            value == null || value == JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    return GhosthandSelectors.normalize(
        text = body.optString("text").ifBlank { null },
        desc = body.optString("desc").ifBlank { null },
        id = body.optString("id").ifBlank { null },
        strategy = body.optString("strategy").ifBlank { null },
        query = query
    )
}

internal fun JSONObject.optIntOrNull(key: String): Int? {
    val value = opt(key)
    return if (value is Number) value.toInt() else null
}

internal fun JSONObject.optLongOrNull(key: String): Long? {
    val value = opt(key)
    return if (value is Number) value.toLong() else null
}

internal fun parseJsonBodyOrNull(requestBody: String, endpoint: String): JSONObject? {
    return try {
        JSONObject(requestBody.ifBlank { "{}" })
    } catch (error: JSONException) {
        Log.w(
            LocalApiServer.LOG_TAG,
            "component=LocalApiServer operation=parseJsonBody endpoint=$endpoint failure=${error.javaClass.simpleName}",
            error
        )
        null
    }
}

internal fun badJsonBodyResponse(): String {
    return buildJsonResponse(
        400,
        errorEnvelope("BAD_REQUEST", "Request body must be valid JSON.")
    )
}

internal fun buildTreeUnavailableResponse(reason: TreeUnavailableReason?): String {
    val message = when (reason) {
        TreeUnavailableReason.ACCESSIBILITY_SERVICE_DISCONNECTED ->
            "Accessibility service is unavailable or not connected."
        TreeUnavailableReason.NO_ACTIVE_ROOT ->
            "Accessibility tree is unavailable because no active window root is available."
        null ->
            "Accessibility tree is unavailable."
    }

    return buildJsonResponse(
        statusCode = 503,
        body = errorEnvelope(
            code = "ACCESSIBILITY_UNAVAILABLE",
            message = message
        )
    )
}
