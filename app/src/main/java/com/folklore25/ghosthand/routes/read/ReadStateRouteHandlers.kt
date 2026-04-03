/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes.read

import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator

internal class ReadStateRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun buildInfoResponse(): String {
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createInfoPayload())
        )
    }

    fun buildFocusedResponse(): String {
        val result = stateCoordinator.getFocusedNodeResult()
        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(stateCoordinator.createFocusedNodePayload(result))
        )
    }
}
