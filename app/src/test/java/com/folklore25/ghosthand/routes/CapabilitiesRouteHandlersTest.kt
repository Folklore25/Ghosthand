/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

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

import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesRouteHandlersTest {
    @Test
    fun capabilitiesPayloadIsCapabilityCentric() {
        val payload = GhosthandCapabilityPresentation.capabilitiesFields(
            capabilityAccess = CapabilityAccessSnapshot(
                accessibility = GovernedCapabilitySnapshot(
                    system = AccessibilitySystemAuthorizationState(
                        enabled = true,
                        connected = true,
                        dispatchCapable = true,
                        healthy = true,
                        status = "enabled_connected"
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(
                        usableNow = true,
                        reason = "accessibility_connected"
                    )
                ),
                screenshot = GovernedCapabilitySnapshot(
                    system = ScreenshotSystemAuthorizationState(
                        accessibilityCaptureReady = true,
                        mediaProjectionGranted = false
                    ),
                    policy = AppCapabilityPolicyState(allowed = true),
                    effective = CapabilityEffectiveState(
                        usableNow = true,
                        reason = "accessibility_capture_ready"
                    )
                )
            ),
            permissionSnapshot = PermissionSnapshot(
                usageAccess = true,
                notifications = true,
                overlay = null,
                writeSecureSettings = false
            )
        )

        assertEquals("1.0", payload["schemaVersion"])
        val capabilities = payload["capabilities"] as List<*>
        val accessibilityControl = capabilities
            .map { it as Map<*, *> }
            .first { it["capabilityId"] == "accessibility_control" }
        val availability = accessibilityControl["availability"] as Map<*, *>

        assertEquals("control", accessibilityControl["domain"])
        assertEquals("primitive", accessibilityControl["kind"])
        assertEquals("action_truth", accessibilityControl["truthType"])
        assertTrue((accessibilityControl["routes"] as List<*>).contains("/click"))
        assertEquals(true, availability["availableNow"])
        assertTrue((availability["requiredServices"] as List<*>).contains("accessibility_service"))
    }
}
