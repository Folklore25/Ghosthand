/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

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

import android.accessibilityservice.AccessibilityService
import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.server.LocalApiServerRoute
import com.folklore25.ghosthand.state.StateCoordinator

internal class ActionRouteHandlers(
    internal val stateCoordinator: StateCoordinator,
    observationPublisher: GhosthandObservationPublisher
) {
    private val tapClickHandlers = ActionTapClickRouteHandlers(stateCoordinator, observationPublisher)
    private val motionHandlers = ActionMotionRouteHandlers(stateCoordinator, observationPublisher)
    private val gestureHandlers = ActionGestureRouteHandlers(stateCoordinator, observationPublisher)
    private val navigationHandlers = ActionNavigationRouteHandlers(stateCoordinator, observationPublisher)

    fun routes(): List<LocalApiServerRoute> {
        return listOf(
            LocalApiServerRoute("POST", "/tap") { request -> tapClickHandlers.buildTapResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/swipe") { request -> motionHandlers.buildSwipeResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/click") { request -> tapClickHandlers.buildClickResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/scroll") { request -> motionHandlers.buildScrollResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/longpress") { request -> gestureHandlers.buildLongpressResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/gesture") { request -> gestureHandlers.buildGestureResponse(request.requestBody) },
            LocalApiServerRoute("POST", "/back") { navigationHandlers.buildGlobalActionResponse("back", AccessibilityService.GLOBAL_ACTION_BACK) },
            LocalApiServerRoute("POST", "/home") { navigationHandlers.buildGlobalActionResponse("home", AccessibilityService.GLOBAL_ACTION_HOME) },
            LocalApiServerRoute("POST", "/recents") { navigationHandlers.buildGlobalActionResponse("recents", AccessibilityService.GLOBAL_ACTION_RECENTS) }
        )
    }
}
