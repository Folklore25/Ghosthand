/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.action

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
