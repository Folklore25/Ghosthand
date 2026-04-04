/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

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

import com.folklore25.ghosthand.capability.GhosthandCapabilityDefinitions

internal data class GhosthandCapabilityPlaneMetadata(
    val plane: String,
    val availabilityModel: String,
    val preconditions: List<String>,
    val failureModes: List<String>,
    val truthType: String,
    val directness: String
)

internal object GhosthandCapabilityPlaneCatalog {
    private val accessibilityControlPreconditions = listOf(
        "accessibility_policy_allowed",
        "accessibility_dispatch_capable"
    )
    private val accessibilityObservationPreconditions = listOf(
        "accessibility_policy_allowed",
        "accessibility_connected"
    )
    private val screenshotPreconditions = listOf(
        "screenshot_policy_allowed",
        "screenshot_capture_available"
    )

    fun metadataFor(command: GhosthandCommandDescriptor): GhosthandCapabilityPlaneMetadata {
        if (command.capabilityIds.size == 1) {
            val capability = GhosthandCapabilityDefinitions.definition(command.capabilityIds.single())
            val availabilityModel = when (capability.capabilityId) {
                "accessibility_control", "accessibility_observation" -> "accessibility_runtime_gated"
                "screenshot_capture", "preview_access" -> "screenshot_runtime_gated"
                else -> "always_available"
            }
            return GhosthandCapabilityPlaneMetadata(
                plane = capability.domain,
                availabilityModel = availabilityModel,
                preconditions = capability.preconditions,
                failureModes = capability.failureModes,
                truthType = capability.truthType,
                directness = capability.directness
            )
        }

        return when (command.id) {
            "screen" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "source_dependent_runtime_gated",
                preconditions = listOf(
                    "accessibility_when_source=accessibility",
                    "screenshot_capture_when_source=ocr_or_hybrid"
                ),
                failureModes = listOf("accessibility_unavailable", "ocr_capture_unavailable"),
                truthType = "structured_surface_observation",
                directness = "mixed"
            )

            "tree", "focused" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "accessibility_runtime_gated",
                preconditions = accessibilityObservationPreconditions,
                failureModes = listOf("accessibility_unavailable"),
                truthType = "structured_surface_observation",
                directness = "direct"
            )

            "foreground" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "always_available",
                preconditions = emptyList(),
                failureModes = emptyList(),
                truthType = "observer_context",
                directness = "direct"
            )

            "info", "state", "device", "health", "ping" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "always_available",
                preconditions = emptyList(),
                failureModes = emptyList(),
                truthType = "runtime_state_summary",
                directness = "derived"
            )

            else -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "always_available",
                preconditions = emptyList(),
                failureModes = emptyList(),
                truthType = "runtime_surface",
                directness = "derived"
            )
        }
    }

}
