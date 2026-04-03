/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.health

import com.folklore25.ghosthand.AccessibilityTreeSnapshotResult
import com.folklore25.ghosthand.DeviceSnapshot
import com.folklore25.ghosthand.ForegroundAppSnapshot
import com.folklore25.ghosthand.RuntimeState
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
