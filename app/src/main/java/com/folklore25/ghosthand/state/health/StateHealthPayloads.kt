/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.health

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

import com.folklore25.ghosthand.server.LocalApiServer
import org.json.JSONObject

object StateHealthPayloads {
    fun runtimeReady(runtimeState: RuntimeState): Boolean {
        return runtimeState.appStarted &&
            runtimeState.localApiServerRunning &&
            runtimeState.foregroundServiceRunning
    }

    fun createHealthPayload(runtimeState: RuntimeState): JSONObject {
        val ready = runtimeReady(runtimeState)
        return JSONObject()
            .put("status", if (ready) "ready" else "starting")
            .put("ready", ready)
            .put("listener", JSONObject()
                .put("host", LocalApiServer.HOST)
                .put("port", LocalApiServer.PORT)
            )
            .put("runtime", JSONObject()
                .put("appStarted", runtimeState.appStarted)
                .put("localApiServerRunning", runtimeState.localApiServerRunning)
                .put("foregroundServiceRunning", runtimeState.foregroundServiceRunning)
                .put("lastServiceAction", runtimeState.lastServiceAction)
                .put("statusText", runtimeState.statusText)
            )
    }

    fun createForegroundPayload(
        foregroundSnapshot: ForegroundAppSnapshot,
        toJson: (ForegroundAppSnapshot) -> JSONObject
    ): JSONObject {
        return toJson(foregroundSnapshot)
    }

    fun createDevicePayload(
        deviceSnapshot: DeviceSnapshot,
        foregroundSnapshot: ForegroundAppSnapshot
    ): JSONObject {
        return JSONObject()
            .put("screenOn", deviceSnapshot.screenOn)
            .put("locked", deviceSnapshot.locked ?: JSONObject.NULL)
            .put("rotation", deviceSnapshot.rotation)
            .put("batteryPercent", deviceSnapshot.batteryPercent ?: JSONObject.NULL)
            .put("charging", deviceSnapshot.charging)
            .put("foregroundPackage", foregroundSnapshot.packageName ?: JSONObject.NULL)
    }

    fun createInfoPayload(
        treeResult: AccessibilityTreeSnapshotResult,
        deviceSnapshot: DeviceSnapshot,
        foregroundSnapshot: ForegroundAppSnapshot
    ): JSONObject {
        return JSONObject()
            .put("package", foregroundSnapshot.packageName ?: JSONObject.NULL)
            .put("activity", foregroundSnapshot.activity ?: JSONObject.NULL)
            .put("label", foregroundSnapshot.label ?: JSONObject.NULL)
            .put("screen", JSONObject()
                .put("on", deviceSnapshot.screenOn)
                .put("rotation", deviceSnapshot.rotation)
                .put("batteryPercent", deviceSnapshot.batteryPercent ?: JSONObject.NULL)
                .put("charging", deviceSnapshot.charging)
            )
            .put("tree", JSONObject()
                .put("available", treeResult.available)
                .put("reason", treeResult.reason?.name ?: JSONObject.NULL)
            )
    }
}
