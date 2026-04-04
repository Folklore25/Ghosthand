/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.observation

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

import com.folklore25.ghosthand.observation.GhosthandObservationEvent
import com.folklore25.ghosthand.observation.GhosthandObservationBatch
import com.folklore25.ghosthand.observation.GhosthandObservationLog
import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.routes.toJsonValue
import com.folklore25.ghosthand.server.LocalApiServerRoute
import org.json.JSONArray
import org.json.JSONObject

internal class ObservationRouteHandlers(
    private val observationLog: GhosthandObservationLog
) {
    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("GET", "/events") { request ->
                buildEventsResponse(request.queryParameters)
            }
        )
    }

    fun buildEventsResponse(queryParameters: Map<String, String>): String {
        val queryResult = queryEvents(queryParameters)
        if (queryResult.errorMessage != null) {
            return invalidArgument(queryResult.errorMessage)
        }
        val batch = queryResult.batch ?: observationLog.readSince()
        val events = JSONArray()
        batch.events.forEach { event ->
            events.put(JSONObject(toJsonValue(eventFields(event)).toString()))
        }

        return buildJsonResponse(
            200,
            successEnvelope(
                JSONObject()
                    .put("events", events)
                    .put("requestedSinceCursor", batch.requestedSinceCursor)
                    .put("oldestCursor", batch.oldestCursor)
                    .put("latestCursor", batch.latestCursor)
                    .put("nextCursor", batch.nextCursor)
                    .put("retentionLimit", batch.retentionLimit)
                    .put("droppedBeforeCursor", batch.droppedBeforeCursor)
            )
        )
    }

    private fun invalidArgument(message: String): String {
        return buildJsonResponse(
            400,
            errorEnvelope("INVALID_ARGUMENT", message)
        )
    }

    internal fun queryEvents(queryParameters: Map<String, String>): ObservationEventsQueryResult {
        val sinceCursor = queryParameters["since"]?.toLongOrNull()
            ?: if (queryParameters.containsKey("since")) {
                return ObservationEventsQueryResult(errorMessage = "since must be an integer cursor.")
            } else {
                null
            }
        val limit = queryParameters["limit"]?.toIntOrNull()
            ?: if (queryParameters.containsKey("limit")) {
                return ObservationEventsQueryResult(errorMessage = "limit must be an integer between 1 and 100.")
            } else {
                GhosthandObservationLog.DEFAULT_PAGE_LIMIT
            }
        if (limit !in 1..GhosthandObservationLog.MAX_PAGE_LIMIT) {
            return ObservationEventsQueryResult(errorMessage = "limit must be between 1 and 100.")
        }
        return ObservationEventsQueryResult(
            batch = observationLog.readSince(sinceCursor = sinceCursor, limit = limit)
        )
    }

    private fun eventFields(event: GhosthandObservationEvent): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "cursor" to event.cursor,
            "type" to event.type,
            "timestamp" to event.timestamp,
            "packageName" to event.packageName,
            "activity" to event.activity,
            "route" to event.route,
            "evidence" to event.evidence.takeIf { it.isNotEmpty() }
        ).filterValues { it != null }
    }
}

internal data class ObservationEventsQueryResult(
    val batch: GhosthandObservationBatch? = null,
    val errorMessage: String? = null
)
