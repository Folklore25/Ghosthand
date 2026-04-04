/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

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
