/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

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


data class CapabilityDefinition(
    val capabilityId: String,
    val domain: String,
    val kind: String,
    val implemented: Boolean,
    val directness: String,
    val truthType: String,
    val preconditions: List<String>,
    val failureModes: List<String>,
    val routes: List<String>
)

data class CapabilityAvailability(
    val capabilityId: String,
    val availableNow: Boolean,
    val degraded: Boolean,
    val blockers: List<String>,
    val requiredServices: List<String>,
    val requiredPermissions: List<String>,
    val currentBackend: String? = null,
    val currentMode: String? = null
)

internal object GhosthandCapabilityDefinitions {
    val definitions: List<CapabilityDefinition> = listOf(
        CapabilityDefinition(
            capabilityId = "accessibility_control",
            domain = "control",
            kind = "primitive",
            implemented = true,
            directness = "direct",
            truthType = "action_truth",
            preconditions = listOf("accessibility_required", "runtime_service_required"),
            failureModes = listOf("accessibility_unavailable", "dispatch_failed", "node_not_found", "stale_node_reference"),
            routes = listOf("/tap", "/click", "/input", "/setText", "/scroll", "/swipe", "/longpress", "/gesture", "/back", "/home", "/recents")
        ),
        CapabilityDefinition(
            capabilityId = "accessibility_observation",
            domain = "observation",
            kind = "primitive",
            implemented = true,
            directness = "direct",
            truthType = "observation_truth",
            preconditions = listOf("accessibility_required"),
            failureModes = listOf("accessibility_unavailable"),
            routes = listOf("/tree", "/screen", "/find", "/focused", "/wait")
        ),
        CapabilityDefinition(
            capabilityId = "screenshot_capture",
            domain = "preview",
            kind = "primitive",
            implemented = true,
            directness = "direct",
            truthType = "observation_truth",
            preconditions = listOf("screenshot_available"),
            failureModes = listOf("screenshot_unavailable"),
            routes = listOf("/screenshot")
        ),
        CapabilityDefinition(
            capabilityId = "preview_access",
            domain = "preview",
            kind = "derived",
            implemented = true,
            directness = "derived",
            truthType = "observation_truth",
            preconditions = listOf("preview_available"),
            failureModes = listOf("preview_unavailable"),
            routes = listOf("/screen", "/screenshot")
        ),
        CapabilityDefinition(
            capabilityId = "event_observation",
            domain = "observation",
            kind = "summary",
            implemented = true,
            directness = "derived",
            truthType = "observation_truth",
            preconditions = listOf("runtime_service_required"),
            failureModes = listOf("stale_cursor_window"),
            routes = listOf("/events")
        ),
        CapabilityDefinition(
            capabilityId = "clipboard_access",
            domain = "control",
            kind = "helper",
            implemented = true,
            directness = "direct",
            truthType = "action_truth",
            preconditions = emptyList(),
            failureModes = listOf("clipboard_write_failed"),
            routes = listOf("/clipboard")
        ),
        CapabilityDefinition(
            capabilityId = "notification_access",
            domain = "evidence",
            kind = "helper",
            implemented = true,
            directness = "direct",
            truthType = "evidence_truth",
            preconditions = listOf("notification_permission_for_post"),
            failureModes = listOf("notification_post_failed", "notification_cancel_failed"),
            routes = listOf("/notify")
        ),
        CapabilityDefinition(
            capabilityId = "route_contract_catalog",
            domain = "capability",
            kind = "summary",
            implemented = true,
            directness = "derived",
            truthType = "capability_truth",
            preconditions = emptyList(),
            failureModes = emptyList(),
            routes = listOf("/commands")
        )
    )

    fun definition(capabilityId: String): CapabilityDefinition =
        definitions.first { it.capabilityId == capabilityId }

    fun definitions(capabilityIds: List<String>): List<CapabilityDefinition> =
        capabilityIds.map(::definition)
}

internal object GhosthandCapabilityAvailabilityResolver {
    fun resolveAll(
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): List<CapabilityAvailability> {
        val accessibilityBlockers = buildList {
            if (!capabilityAccess.accessibility.policy.allowed) add("policy_blocked")
            if (!capabilityAccess.accessibility.system.enabled) add("service_disabled")
            if (!capabilityAccess.accessibility.system.connected) add("service_disconnected")
            if (!capabilityAccess.accessibility.system.dispatchCapable) add("dispatch_unavailable")
        }
        val screenshotBlockers = buildList {
            if (!capabilityAccess.screenshot.policy.allowed) add("policy_blocked")
            if (!capabilityAccess.screenshot.system.accessibilityCaptureReady && !capabilityAccess.screenshot.system.mediaProjectionGranted) {
                add("capture_path_unavailable")
            }
        }

        return listOf(
            CapabilityAvailability(
                capabilityId = "accessibility_control",
                availableNow = capabilityAccess.accessibility.effective.usableNow,
                degraded = capabilityAccess.accessibility.policy.allowed && !capabilityAccess.accessibility.effective.usableNow,
                blockers = accessibilityBlockers,
                requiredServices = listOf("accessibility_service"),
                requiredPermissions = emptyList(),
                currentBackend = "accessibility"
            ),
            CapabilityAvailability(
                capabilityId = "accessibility_observation",
                availableNow = capabilityAccess.accessibility.policy.allowed && capabilityAccess.accessibility.system.connected,
                degraded = capabilityAccess.accessibility.policy.allowed && capabilityAccess.accessibility.system.enabled && !capabilityAccess.accessibility.system.connected,
                blockers = accessibilityBlockers,
                requiredServices = listOf("accessibility_service"),
                requiredPermissions = emptyList(),
                currentMode = "accessibility"
            ),
            CapabilityAvailability(
                capabilityId = "screenshot_capture",
                availableNow = capabilityAccess.screenshot.effective.usableNow,
                degraded = capabilityAccess.screenshot.policy.allowed && !capabilityAccess.screenshot.effective.usableNow,
                blockers = screenshotBlockers,
                requiredServices = emptyList(),
                requiredPermissions = listOf("media_projection_or_accessibility_capture")
            ),
            CapabilityAvailability(
                capabilityId = "preview_access",
                availableNow = capabilityAccess.screenshot.effective.usableNow,
                degraded = capabilityAccess.screenshot.policy.allowed && !capabilityAccess.screenshot.effective.usableNow,
                blockers = screenshotBlockers,
                requiredServices = emptyList(),
                requiredPermissions = listOf("media_projection_or_accessibility_capture"),
                currentMode = "preview_path"
            ),
            CapabilityAvailability(
                capabilityId = "event_observation",
                availableNow = true,
                degraded = false,
                blockers = emptyList(),
                requiredServices = listOf("runtime_service"),
                requiredPermissions = emptyList(),
                currentMode = "cursor_polling"
            ),
            CapabilityAvailability(
                capabilityId = "clipboard_access",
                availableNow = true,
                degraded = false,
                blockers = emptyList(),
                requiredServices = emptyList(),
                requiredPermissions = emptyList(),
                currentBackend = "android_clipboard"
            ),
            CapabilityAvailability(
                capabilityId = "notification_access",
                availableNow = permissionSnapshot.notifications != false,
                degraded = permissionSnapshot.notifications == false,
                blockers = if (permissionSnapshot.notifications == false) listOf("notification_permission_denied") else emptyList(),
                requiredServices = listOf("notification_listener_for_read"),
                requiredPermissions = listOf("post_notifications_for_post"),
                currentBackend = "android_notification_manager"
            ),
            CapabilityAvailability(
                capabilityId = "route_contract_catalog",
                availableNow = true,
                degraded = false,
                blockers = emptyList(),
                requiredServices = emptyList(),
                requiredPermissions = emptyList()
            )
        )
    }
}

internal object GhosthandCapabilityPresentation {
    fun commandCapabilityFields(capabilityIds: List<String>): List<Map<String, Any?>> {
        return GhosthandCapabilityDefinitions.definitions(capabilityIds).map { definition ->
            linkedMapOf(
                "capabilityId" to definition.capabilityId,
                "domain" to definition.domain,
                "kind" to definition.kind,
                "implemented" to definition.implemented,
                "directness" to definition.directness,
                "truthType" to definition.truthType,
                "preconditions" to definition.preconditions,
                "failureModes" to definition.failureModes
            )
        }
    }

    fun stateSummaryFields(
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): Map<String, Any?> {
        return GhosthandCapabilityAvailabilityResolver.resolveAll(capabilityAccess, permissionSnapshot)
            .associate { availability ->
                availability.capabilityId to linkedMapOf(
                    "availableNow" to availability.availableNow,
                    "degraded" to availability.degraded,
                    "blockers" to availability.blockers,
                    "requiredServices" to availability.requiredServices,
                    "requiredPermissions" to availability.requiredPermissions,
                    "currentBackend" to availability.currentBackend,
                    "currentMode" to availability.currentMode
                ).filterValues { it != null }
            }
    }

    fun capabilitiesFields(
        capabilityAccess: CapabilityAccessSnapshot,
        permissionSnapshot: PermissionSnapshot
    ): Map<String, Any?> {
        val availability = GhosthandCapabilityAvailabilityResolver.resolveAll(capabilityAccess, permissionSnapshot)
            .associateBy { it.capabilityId }
        return linkedMapOf(
            "schemaVersion" to "1.0",
            "capabilities" to GhosthandCapabilityDefinitions.definitions.map { definition ->
                val live = availability.getValue(definition.capabilityId)
                linkedMapOf(
                    "capabilityId" to definition.capabilityId,
                    "domain" to definition.domain,
                    "kind" to definition.kind,
                    "implemented" to definition.implemented,
                    "directness" to definition.directness,
                    "truthType" to definition.truthType,
                    "preconditions" to definition.preconditions,
                    "failureModes" to definition.failureModes,
                    "routes" to definition.routes,
                    "availability" to linkedMapOf(
                        "availableNow" to live.availableNow,
                        "degraded" to live.degraded,
                        "blockers" to live.blockers,
                        "requiredServices" to live.requiredServices,
                        "requiredPermissions" to live.requiredPermissions,
                        "currentBackend" to live.currentBackend,
                        "currentMode" to live.currentMode
                    ).filterValues { it != null }
                )
            }
        )
    }
}
