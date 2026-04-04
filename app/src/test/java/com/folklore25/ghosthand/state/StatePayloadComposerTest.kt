/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state

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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatePayloadComposerTest {
    @Test
    fun permissionsPayloadPublishesCapabilityPlanesAndCurrentBlockers() {
        val permissions = StatePayloadComposer.permissionsPayload(
            accessibilityEnabled = false,
            capabilityAccess = CapabilityAccessSnapshot(
                accessibility = GovernedCapabilitySnapshot(
                    system = AccessibilitySystemAuthorizationState(
                        enabled = false,
                        connected = false,
                        dispatchCapable = false,
                        healthy = false,
                        status = "disabled"
                    ),
                    policy = AppCapabilityPolicyState(allowed = false),
                    effective = CapabilityEffectiveState(
                        usableNow = false,
                        reason = "policy_blocked"
                    )
                ),
                screenshot = GovernedCapabilitySnapshot(
                    system = ScreenshotSystemAuthorizationState(
                        accessibilityCaptureReady = false,
                        mediaProjectionGranted = false
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(
                        usableNow = false,
                        reason = "capture_unavailable"
                    )
                )
            ),
            permissionSnapshot = PermissionSnapshot(
                usageAccess = true,
                notifications = false,
                overlay = null,
                writeSecureSettings = false
            )
        )

        val capabilitySummary = permissions["capabilitySummary"] as Map<*, *>
        val accessibility = capabilitySummary["accessibility_control"] as Map<*, *>
        val screenshot = capabilitySummary["screenshot_capture"] as Map<*, *>

        assertEquals(false, accessibility["availableNow"])
        assertEquals(false, screenshot["availableNow"])
        assertTrue((accessibility["blockers"] as List<*>).contains("policy_blocked"))
        assertTrue((screenshot["blockers"] as List<*>).contains("capture_path_unavailable"))
        assertEquals(listOf("accessibility_service"), accessibility["requiredServices"])
        assertEquals(listOf("media_projection_or_accessibility_capture"), screenshot["requiredPermissions"])
    }
}
