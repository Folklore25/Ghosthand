/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

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

import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.server.LocalApiServerRoute
import com.folklore25.ghosthand.state.StateCoordinator

internal class ReadRouteHandlers(
    internal val stateCoordinator: StateCoordinator,
    observationPublisher: GhosthandObservationPublisher
) {
    private val treeHandlers = ReadTreeRouteHandlers(stateCoordinator)
    private val findHandlers = ReadFindRouteHandlers(stateCoordinator)
    private val screenHandlers = ReadScreenRouteHandlers(stateCoordinator, observationPublisher)
    private val screenshotHandlers = ReadScreenshotRouteHandlers(stateCoordinator)
    private val stateHandlers = ReadStateRouteHandlers(stateCoordinator)

    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("GET", "/tree") { request -> treeHandlers.buildTreeResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/find") { request -> findHandlers.buildFindResponse(request.requestBody) },
            LocalApiServerRoute("GET", "/screen") { request -> screenHandlers.buildScreenResponse(request.queryParameters) },
            LocalApiServerRoute("GET", "/screenshot") { request -> screenshotHandlers.buildScreenshotGetResponse(request.queryParameters) },
            LocalApiServerRoute("POST", "/screenshot") { request -> screenshotHandlers.buildScreenshotResponse(request.requestBody) },
            LocalApiServerRoute("GET", "/info") { stateHandlers.buildInfoResponse() },
            LocalApiServerRoute("GET", "/focused") { stateHandlers.buildFocusedResponse() }
        )
    }
}
