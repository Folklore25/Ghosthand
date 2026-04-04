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

import com.folklore25.ghosthand.routes.buildJsonResponse
import com.folklore25.ghosthand.routes.buildTreeUnavailableResponse
import com.folklore25.ghosthand.routes.errorEnvelope
import com.folklore25.ghosthand.routes.successEnvelope
import com.folklore25.ghosthand.state.StateCoordinator

internal class ReadTreeRouteHandlers(
    private val stateCoordinator: StateCoordinator
) {
    fun buildTreeResponse(queryParameters: Map<String, String>): String {
        val mode = queryParameters["mode"]?.ifBlank { DEFAULT_TREE_MODE } ?: DEFAULT_TREE_MODE
        if (mode != TREE_MODE_FLAT && mode != TREE_MODE_RAW) {
            return buildJsonResponse(
                statusCode = 422,
                body = errorEnvelope(
                    code = "UNSUPPORTED_OPERATION",
                    message = "Only mode=flat and mode=raw are supported for /tree."
                )
            )
        }

        val treeSnapshotResult = stateCoordinator.getTreeSnapshotResult()
        if (!treeSnapshotResult.available) {
            return buildTreeUnavailableResponse(treeSnapshotResult.reason)
        }

        return buildJsonResponse(
            statusCode = 200,
            body = successEnvelope(
                stateCoordinator.createTreePayload(
                    treeSnapshotResult.snapshot ?: return buildTreeUnavailableResponse(TreeUnavailableReason.NO_ACTIVE_ROOT),
                    mode = mode
                )
            )
        )
    }

    private companion object {
        const val TREE_MODE_RAW = "raw"
        const val TREE_MODE_FLAT = "flat"
        const val DEFAULT_TREE_MODE = TREE_MODE_RAW
    }
}
