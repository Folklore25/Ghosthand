/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

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
