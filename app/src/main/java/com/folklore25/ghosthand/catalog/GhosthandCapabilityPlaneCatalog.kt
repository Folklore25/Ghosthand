/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

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
        return when (command.id) {
            "tap" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "accessibility_action_failed", "node_not_found"))
            "click" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "node_not_found", "stale_node_reference", "accessibility_action_failed"))
            "input" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "focused_editable_missing", "text_mutation_failed", "key_dispatch_failed"))
            "set_text" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "node_not_found", "text_mutation_failed"))
            "scroll" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "node_not_found", "invalid_direction", "dispatch_failed"))
            "swipe" -> controlMetadata(failureModes = listOf("accessibility_unavailable", "dispatch_failed"))
            "longpress", "gesture", "back", "home", "recents" ->
                controlMetadata(failureModes = listOf("accessibility_unavailable", "dispatch_failed"))

            "find" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "accessibility_runtime_gated",
                preconditions = accessibilityObservationPreconditions,
                failureModes = listOf("accessibility_unavailable", "no_selector_match"),
                truthType = "structured_selector_observation",
                directness = "derived"
            )

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

            "events" -> GhosthandCapabilityPlaneMetadata(
                plane = "observation",
                availabilityModel = "always_available",
                preconditions = emptyList(),
                failureModes = listOf("stale_cursor_window"),
                truthType = "recent_event_observation",
                directness = "derived"
            )

            "capabilities", "commands" -> GhosthandCapabilityPlaneMetadata(
                plane = "capability",
                availabilityModel = "always_available",
                preconditions = emptyList(),
                failureModes = emptyList(),
                truthType = "capability_truth",
                directness = "derived"
            )
            "screenshot" -> GhosthandCapabilityPlaneMetadata(
                plane = "preview",
                availabilityModel = "screenshot_runtime_gated",
                preconditions = screenshotPreconditions,
                failureModes = listOf("screenshot_unavailable"),
                truthType = "visual_truth",
                directness = "direct"
            )

            "wait_ui_change", "wait_condition" -> GhosthandCapabilityPlaneMetadata(
                plane = "evidence",
                availabilityModel = "accessibility_runtime_gated",
                preconditions = accessibilityObservationPreconditions,
                failureModes = listOf("timed_out", "accessibility_unavailable"),
                truthType = "settled_state_evidence",
                directness = "derived"
            )

            "clipboard_read", "clipboard_write", "notify_read", "notify_post", "notify_cancel" ->
                GhosthandCapabilityPlaneMetadata(
                    plane = "utility",
                    availabilityModel = "always_available",
                    preconditions = emptyList(),
                    failureModes = emptyList(),
                    truthType = "local_runtime_utility",
                    directness = "direct"
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

    private fun controlMetadata(failureModes: List<String>): GhosthandCapabilityPlaneMetadata {
        return GhosthandCapabilityPlaneMetadata(
            plane = "control",
            availabilityModel = "accessibility_runtime_gated",
            preconditions = accessibilityControlPreconditions,
            failureModes = failureModes,
            truthType = "execution_truth_with_effect_evidence",
            directness = "direct"
        )
    }
}
