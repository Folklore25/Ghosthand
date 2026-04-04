/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.read

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

import android.os.SystemClock
import com.folklore25.ghosthand.capability.CapabilityAccessResolver
import com.folklore25.ghosthand.state.StatePayloadComposer
import com.folklore25.ghosthand.state.health.StateHealthPayloads
import org.json.JSONObject

internal class StateReadCoordinator(
    private val runtimeStateProvider: () -> RuntimeState,
    private val treeSnapshotProvider: () -> AccessibilityTreeSnapshotResult,
    private val homeDiagnosticsProvider: HomeDiagnosticsProvider,
    private val deviceSnapshotProvider: DeviceSnapshotProvider,
    private val foregroundAppProvider: ForegroundAppProvider,
    private val permissionSnapshotProvider: PermissionSnapshotProvider,
    private val accessibilityStatusProvider: AccessibilityStatusProvider,
    private val capabilityAccessResolver: CapabilityAccessResolver
) {
    fun capabilityAccessSnapshot(): CapabilityAccessSnapshot {
        return capabilityAccessResolver.capabilityAccessSnapshot()
    }

    fun createStatePayload(): JSONObject {
        val runtimeState = runtimeStateProvider()
        val diagnosticsSnapshot = homeDiagnosticsProvider.snapshot()
        val deviceSnapshot = deviceSnapshotProvider.snapshot()
        val foregroundSnapshot = foregroundAppProvider.snapshot()
        val permissionSnapshot = permissionSnapshotProvider.snapshot()
        val accessibilitySnapshot = capabilityAccessResolver.accessibilityStatusSnapshot()
        val capabilityAccess = capabilityAccessResolver.capabilityAccessSnapshot(accessibilitySnapshot)
        val runtimeUptimeMs = runtimeState.appStartedAtElapsedRealtimeMs?.let {
            (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L)
        }

        return StatePayloadComposer.createStatePayload(
            runtimeState = runtimeState,
            runtimeReady = StateHealthPayloads.runtimeReady(runtimeState),
            runtimeUptimeMs = runtimeUptimeMs,
            diagnosticsSnapshot = diagnosticsSnapshot,
            deviceSnapshot = deviceSnapshot,
            foregroundSnapshot = foregroundSnapshot,
            accessibilitySnapshot = accessibilitySnapshot,
            capabilityAccess = capabilityAccess,
            permissionSnapshot = permissionSnapshot
        )
    }

    fun createForegroundPayload(): JSONObject {
        return StateHealthPayloads.createForegroundPayload(
            foregroundSnapshot = foregroundAppProvider.snapshot(),
            toJson = foregroundAppProvider::toJson
        )
    }

    fun createDevicePayload(): JSONObject {
        return StateHealthPayloads.createDevicePayload(
            deviceSnapshot = deviceSnapshotProvider.snapshot(),
            foregroundSnapshot = foregroundAppProvider.snapshot()
        )
    }

    fun createInfoPayload(): JSONObject {
        return StateHealthPayloads.createInfoPayload(
            treeResult = treeSnapshotProvider(),
            deviceSnapshot = deviceSnapshotProvider.snapshot(),
            foregroundSnapshot = foregroundAppProvider.snapshot()
        )
    }
}
