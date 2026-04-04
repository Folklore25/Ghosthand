/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

import com.folklore25.ghosthand.capability.GhosthandCapabilityDefinitions

internal object GhosthandReadCommandCatalog {
    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "ping",
            category = "read",
            method = "GET",
            path = "/ping",
            capabilityIds = listOf("route_contract_catalog"),
            description = "Health check with current Ghosthand version",
            responseFields = listOf("service", "version"),
            exampleResponse = mapOf("ok" to true, "data" to mapOf("service" to "ghosthand", "version" to "1.0 (1)"))
        ),
        GhosthandCommandDescriptor(
            id = "foreground",
            category = "read",
            method = "GET",
            path = "/foreground",
            capabilityIds = listOf("accessibility_observation"),
            description = "Current foreground app/activity summary; observer context only, not the sole visible-surface truth source",
            responseFields = listOf("packageName", "activity", "label", "timestamp"),
            stateTruth = "observer_context",
            operatorUses = listOf("observer_context")
        ),
        GhosthandCommandDescriptor(
            id = "events",
            category = "read",
            method = "GET",
            path = "/events",
            capabilityIds = listOf("event_observation"),
            description = "Poll recent high-value Ghosthand runtime events through a cursor-based observation window. This is the bounded observation plane for recent foreground, readability, preview, fallback, and action-adjacent state changes; it does not replace the primitive HTTP control surface or require a streaming transport.",
            responseFields = listOf("requestedSinceCursor", "oldestCursor", "latestCursor", "nextCursor", "retentionLimit", "droppedBeforeCursor", "events"),
            params = listOf(
                GhosthandCommandParam("since", "long", "query", false, "Return events with cursor values greater than this cursor"),
                GhosthandCommandParam("limit", "int", "query", false, "Maximum number of events to return")
            ),
            exampleRequest = mapOf("since" to 12, "limit" to 20)
        ),
        GhosthandCommandDescriptor(
            id = "screen",
            category = "read",
            method = "GET",
            path = "/screen",
            capabilityIds = listOf("accessibility_observation", "preview_access"),
            description = "Current actionable surface snapshot. `source=accessibility` keeps the default structured tree-first read, while explicit `ocr` or bounded `hybrid` modes expose OCR-derived elements with source provenance when accessibility output is operationally insufficient. `summaryOnly=true` returns a compact orientation summary instead of the full element payload. When `previewAvailable=true`, the response publishes `previewPath` as an explicit lightweight retrieval path using `/screenshot?width={previewWidth}&height={previewHeight}` instead of embedding image bytes in `/screen`. Responses publish explicit render mode, surface readability, visual availability, preview availability, and bounded fallback recommendation signals through `suggestedSource` and `fallbackReason` without auto-switching observation modes. During modal transitions, accessibility availability can briefly dip, so a short wait-and-retry is often the right next move before treating the read as terminally unavailable.",
            responseFields = GhosthandSelectorCatalog.screenResponseFields,
            stateTruth = "structured_actionable_surface_snapshot",
            operatorUses = listOf("structured_actionable_surface_snapshot", "selector_planning"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("source", "string", "query", false, "Read source mode", listOf("accessibility", "ocr", "hybrid")),
                GhosthandCommandParam("summaryOnly", "boolean", "query", false, "Return compact orientation summary instead of full elements"),
                GhosthandCommandParam("editable", "boolean", "query", false, "Filter to editable elements only"),
                GhosthandCommandParam("scrollable", "boolean", "query", false, "Filter to scrollable elements only"),
                GhosthandCommandParam("clickable", "boolean", "query", false, "Filter to clickable elements only"),
                GhosthandCommandParam("package", "string", "query", false, "Restrict results to a package name")
            )
        ),
        GhosthandCommandDescriptor(
            id = "tree",
            category = "read",
            method = "GET",
            path = "/tree",
            capabilityIds = listOf("accessibility_observation"),
            description = "Current accessibility tree snapshot with explicit trust signaling for invalid bounds, low-signal nodes, and whether the current output is structurally full",
            responseFields = listOf("packageName", "activity", "snapshotToken", "capturedAt", "foregroundStableDuringCapture", "partialOutput", "returnedNodeCount", "warnings", "invalidBoundsCount", "lowSignalCount"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(GhosthandCommandParam("mode", "string", "query", false, "Tree shape to return", listOf("raw", "flat")))
        ),
        GhosthandCommandDescriptor(
            id = "info",
            category = "read",
            method = "GET",
            path = "/info",
            capabilityIds = listOf("route_contract_catalog"),
            description = "Current foreground package, activity, and tree availability",
            responseFields = listOf("package", "activity", "label", "screen", "tree"),
            stateTruth = "mixed_state_summary"
        ),
        GhosthandCommandDescriptor(
            id = "focused",
            category = "read",
            method = "GET",
            path = "/focused",
            capabilityIds = listOf("accessibility_observation"),
            description = "Currently focused accessibility node",
            responseFields = listOf("available", "node", "reason"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            exampleResponse = mapOf("ok" to true, "data" to mapOf("available" to true, "node" to mapOf("resourceId" to "android:id/input", "focused" to true)))
        )
    )
}

internal object GhosthandInteractionCommandCatalog {
    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "tap", category = "interaction", method = "POST", path = "/tap", capabilityIds = listOf("accessibility_control"),
            description = "Tap exact screen coordinates and return a compact post-action state summary when Ghosthand can cheaply observe the resulting surface",
            responseFields = listOf("performed", "attemptedPath", "backendUsed", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            transportContract = "prompt_completion",
            params = listOf(
                GhosthandCommandParam("x", "int", "body", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", "body", true, "Screen Y coordinate")
            ),
            exampleRequest = mapOf("x" to 540, "y" to 1200)
        ),
        GhosthandCommandDescriptor(
            id = "click", category = "interaction", method = "POST", path = "/click", capabilityIds = listOf("accessibility_control"),
            description = "Click by nodeId or first-class selector (text, contentDesc, resourceId); selector-based click resolves to an actionable clickable target by default, can cross between text and contentDesc through a bounded fallback chain, reports the requested-vs-matched selector truth on the dispatched target, returns bounded failure categories plus selector/actionability evidence on selector misses, and may classify a stale nodeId reference separately from an ordinary miss when the saved snapshot expired. Successful responses also include a compact post-action state summary as descriptive shorthand beside the primary observed effect fields. During modal transitions, accessibility availability can briefly dip, so a short wait-and-retry or selector re-resolution is often more truthful than treating an immediate miss as terminal.",
            responseFields = listOf("performed", "stateChanged", "backendUsed", "attemptedPath", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "resolution", "failureCategory", "selectorMatchCount", "actionableMatchCount", "disclosure"),
            transportContract = "prompt_completion",
            operatorUses = listOf("text_selector", "content_desc_selector", "resource_id_selector"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("text", "string", "body", false, "Exact visible text selector"),
                GhosthandCommandParam("desc", "string", "body", false, "Exact content description selector; use when the meaningful label lives in contentDesc"),
                GhosthandCommandParam("id", "string", "body", false, "Exact resource id selector"),
                GhosthandCommandParam("clickable", "boolean", "body", false, "Override actionable-target resolution; default behavior is true for selector-based click")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = listOf("text", "resourceId", "contentDesc"),
                primaryStrategies = listOf("text", "contentDesc", "resourceId")
            ),
            exampleRequest = mapOf("desc" to "Settings"),
            exampleResponse = mapOf("ok" to true, "data" to mapOf("performed" to true, "attemptedPath" to "node_click", "resolution" to mapOf("requestedStrategy" to "contentDesc", "effectiveStrategy" to "contentDesc", "requestedSurface" to "contentDesc", "matchedSurface" to "contentDesc", "requestedMatchSemantics" to "exact", "matchedMatchSemantics" to "exact", "usedSurfaceFallback" to false, "usedContainsFallback" to false, "matchedNodeClickable" to true, "resolutionKind" to "matched_node")))
        ),
        GhosthandCommandDescriptor(
            id = "find", category = "interaction", method = "POST", path = "/find", capabilityIds = listOf("accessibility_observation"),
            description = "Find by first-class selector surface (text, contentDesc, resourceId); exact strategies stay exact, contains strategies stay explicit, and miss responses expose requested-vs-matched selector truth so surface mismatch, mode mismatch, and real absence are easier to distinguish",
            responseFields = listOf("found", "matchCount", "index", "node", "text", "desc", "id", "bounds", "centerX", "centerY", "clickable", "editable", "scrollable", "searchedSurface", "matchSemantics", "requestedSurface", "requestedMatchSemantics", "matchedSurface", "matchedMatchSemantics", "usedSurfaceFallback", "usedContainsFallback", "suggestedAlternateSurfaces", "suggestedAlternateStrategies", "disclosure"),
            transportContract = "prompt_completion",
            operatorUses = listOf("text_selector", "content_desc_selector", "resource_id_selector", "index_disambiguation"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("text", "string", "body", false, "Exact visible text selector"),
                GhosthandCommandParam("desc", "string", "body", false, "Exact content description selector; use when the meaningful label lives in contentDesc"),
                GhosthandCommandParam("id", "string", "body", false, "Exact resource id selector"),
                GhosthandCommandParam("strategy", "string", "body", false, "Explicit strategy name", GhosthandSelectorCatalog.strategies),
                GhosthandCommandParam("query", "string", "body", false, "Explicit strategy query"),
                GhosthandCommandParam("clickable", "boolean", "body", false, "Resolve up to a clickable target"),
                GhosthandCommandParam("index", "int", "body", false, "Bounded aid to select one match when a selector returns multiple results")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = GhosthandSelectorCatalog.strategies,
                primaryStrategies = listOf("text", "contentDesc", "resourceId"),
                boundedAids = listOf("index")
            ),
            exampleRequest = mapOf("desc" to "Settings", "clickable" to true),
            exampleResponse = mapOf("ok" to true, "data" to mapOf("matchCount" to 1, "centerX" to 540, "centerY" to 640))
        ),
        GhosthandCommandDescriptor(
            id = "input", category = "interaction", method = "POST", path = "/input", capabilityIds = listOf("accessibility_control"),
            description = "Explicit focused-input interaction route: mutate text, dispatch Enter, or request both in sequence without implicitly clearing existing text, with a compact post-action state summary when Ghosthand can cheaply observe the resulting surface",
            responseFields = listOf("performed", "attemptedPath", "backendUsed", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "textChanged", "keyDispatched", "textMutation", "keyDispatch", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("text", "string", "body", false, "Text payload for explicit mutation"),
                GhosthandCommandParam("textAction", "string", "body", false, "Text mutation mode", listOf("set", "append", "clear")),
                GhosthandCommandParam("key", "string", "body", false, "Explicit key dispatch", listOf("enter")),
                GhosthandCommandParam("append", "boolean", "body", false, "Legacy alias for textAction=append"),
                GhosthandCommandParam("clear", "boolean", "body", false, "Legacy alias for textAction=clear")
            ),
            focusRequirement = "focused_editable",
            exampleRequest = mapOf("textAction" to "set", "text" to "wifi", "key" to "enter")
        ),
        GhosthandCommandDescriptor(
            id = "set_text", category = "interaction", method = "POST", path = "/setText", capabilityIds = listOf("accessibility_control"),
            description = "Set text on a specific editable node; nodeId is snapshot-ephemeral and should only be used within the same trusted snapshot context, and successful responses add a compact post-action state summary when cheap observation is available",
            responseFields = listOf("performed", "attemptedPath", "backendUsed", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", true, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("text", "string", "body", true, "Replacement text")
            ),
            exampleRequest = mapOf("nodeId" to "snap:abc123:path:0.1", "text" to "wifi")
        ),
        GhosthandCommandDescriptor(
            id = "scroll", category = "interaction", method = "POST", path = "/scroll", capabilityIds = listOf("accessibility_control"),
            description = "Scroll a target node or matching container; use contentChanged as the primary same-activity effect signal, keep before/after snapshot tokens for supporting detail, and add a compact post-action state summary as descriptive shorthand",
            responseFields = listOf("performed", "count", "direction", "attemptedPath", "stateChanged", "contentChanged", "surfaceChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("target", "string", "body", false, "Text target used to locate a scroll container"),
                GhosthandCommandParam("direction", "string", "body", true, "Scroll direction", listOf("up", "down", "left", "right")),
                GhosthandCommandParam("count", "int", "body", false, "Repeat count")
            ),
            selectorSupport = GhosthandSelectorSupport(aliases = listOf("text"), strategies = listOf("text"), primaryStrategies = listOf("text")),
            delayedAcceptance = "recommended",
            exampleRequest = mapOf("direction" to "down", "count" to 1)
        ),
        GhosthandCommandDescriptor(
            id = "swipe", category = "interaction", method = "POST", path = "/swipe", capabilityIds = listOf("accessibility_control"),
            description = "Swipe between two coordinates; canonical request uses from/to point objects, x1/y1/x2/y2 aliases are accepted for discoverability, contentChanged is the primary same-activity effect signal, and successful responses add a compact post-action state summary as descriptive shorthand",
            responseFields = listOf("performed", "attemptedPath", "backendUsed", "requestShape", "stateChanged", "contentChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("from", "point", "body", true, "Start coordinate object"),
                GhosthandCommandParam("to", "point", "body", true, "End coordinate object"),
                GhosthandCommandParam("x1", "int", "body", false, "Alias start X coordinate"),
                GhosthandCommandParam("y1", "int", "body", false, "Alias start Y coordinate"),
                GhosthandCommandParam("x2", "int", "body", false, "Alias end X coordinate"),
                GhosthandCommandParam("y2", "int", "body", false, "Alias end Y coordinate"),
                GhosthandCommandParam("durationMs", "long", "body", true, "Swipe duration in milliseconds")
            ),
            delayedAcceptance = "recommended",
            exampleRequest = mapOf("from" to mapOf("x" to 540, "y" to 1700), "to" to mapOf("x" to 540, "y" to 500), "durationMs" to 300)
        ),
        GhosthandCommandDescriptor(
            id = "longpress", category = "interaction", method = "POST", path = "/longpress", capabilityIds = listOf("accessibility_control"),
            description = "Long press at coordinates and return a compact post-action state summary when Ghosthand can cheaply observe the resulting surface",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("x", "int", "body", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", "body", true, "Screen Y coordinate"),
                GhosthandCommandParam("durationMs", "long", "body", false, "Press duration in milliseconds")
            )
        ),
        GhosthandCommandDescriptor(
            id = "gesture", category = "interaction", method = "POST", path = "/gesture", capabilityIds = listOf("accessibility_control"),
            description = "Composite gesture or multi-stroke dispatch with a compact post-action state summary when Ghosthand can cheaply observe the resulting surface",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("type", "string", "body", false, "Named gesture type", listOf("pinch_in", "pinch_out")),
                GhosthandCommandParam("strokes", "stroke_array", "body", false, "Custom stroke descriptors")
            ),
            delayedAcceptance = "recommended"
        ),
        GhosthandCommandDescriptor(
            id = "back", category = "interaction", method = "POST", path = "/back", capabilityIds = listOf("accessibility_control"),
            description = "Perform system back and report bounded observed effect fields alongside dispatch truth, with a compact post-action state summary as descriptive shorthand",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            transportContract = "prompt_completion"
        ),
        GhosthandCommandDescriptor(
            id = "home", category = "interaction", method = "POST", path = "/home", capabilityIds = listOf("accessibility_control"),
            description = "Go to launcher home and report bounded observed effect fields alongside dispatch truth, with a compact post-action state summary as descriptive shorthand",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            transportContract = "prompt_completion"
        ),
        GhosthandCommandDescriptor(
            id = "recents", category = "interaction", method = "POST", path = "/recents", capabilityIds = listOf("accessibility_control"),
            description = "Open system recents and report a compact post-action state summary when Ghosthand can observe the resulting surface cheaply",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "postActionState", "suggestedSource", "fallbackReason", "disclosure"),
            transportContract = "prompt_completion"
        )
    )
}

internal object GhosthandSensingCommandCatalog {
    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "screenshot", category = "sensing", method = "GET", path = "/screenshot",
            capabilityIds = listOf("screenshot_capture", "preview_access"),
            description = "Return current screenshot as base64 PNG; primary visual truth for debugging and verification when structured surface output is stale, invalid, or ambiguous. `/screen` preview metadata points back to this route through `previewPath`.",
            responseFields = listOf("image", "width", "height"),
            stateTruth = "visual_truth",
            operatorUses = listOf("visual_truth", "debugging", "verification")
        ),
        GhosthandCommandDescriptor(
            id = "notify_read", category = "sensing", method = "GET", path = "/notify",
            capabilityIds = listOf("notification_access"),
            description = "Read buffered notifications",
            responseFields = listOf("notifications"),
            params = listOf(
                GhosthandCommandParam("package", "string", "query", false, "Restrict results to one package"),
                GhosthandCommandParam("exclude", "csv", "query", false, "Comma-separated packages to exclude")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_post", category = "sensing", method = "POST", path = "/notify",
            capabilityIds = listOf("notification_access"),
            description = "Post a local notification",
            responseFields = listOf("posted", "notificationId"),
            params = listOf(
                GhosthandCommandParam("title", "string", "body", false, "Notification title"),
                GhosthandCommandParam("text", "string", "body", true, "Notification body")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_cancel", category = "sensing", method = "DELETE", path = "/notify",
            capabilityIds = listOf("notification_access"),
            description = "Cancel a posted local notification",
            responseFields = listOf("canceled"),
            params = listOf(GhosthandCommandParam("notificationId", "int", "body", true, "Notification identifier"))
        ),
        GhosthandCommandDescriptor(
            id = "wait_ui_change", category = "sensing", method = "GET", path = "/wait",
            capabilityIds = listOf("accessibility_observation"),
            description = "Wait for UI change; changed is kept for compatibility, while conditionMet, stateChanged, and timedOut separate wait outcome truth from the final observed settled state. This is the preferred short retry-oriented settle path when modal transitions briefly reduce accessibility availability.",
            responseFields = listOf("changed", "conditionMet", "stateChanged", "timedOut", "elapsedMs", "snapshotToken", "packageName", "activity", "disclosure"),
            stateTruth = "final_settled_state",
            changeSignal = "transition_observed_during_window",
            params = listOf(
                GhosthandCommandParam("timeout", "long", "query", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", "query", false, "Polling interval in milliseconds")
            ),
            delayedAcceptance = "required",
            exampleRequest = mapOf("timeout" to 3000, "intervalMs" to 200)
        ),
        GhosthandCommandDescriptor(
            id = "wait_condition", category = "sensing", method = "POST", path = "/wait",
            capabilityIds = listOf("accessibility_observation"),
            description = "Wait for a matching tree condition using the same selector aliases and strategies as `/find`; satisfied is kept for compatibility, while conditionMet, stateChanged, and timedOut separate selector success from broader surface change. During modal transitions, a short retry-oriented wait is often the right response when accessibility briefly drops before the surface settles.",
            responseFields = listOf("satisfied", "conditionMet", "stateChanged", "timedOut", "elapsedMs", "node", "reason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("condition", "selector", "body", true, "Selector object for the awaited condition; use text/desc/id aliases or explicit strategy+query"),
                GhosthandCommandParam("timeoutMs", "long", "body", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", "body", false, "Polling interval in milliseconds")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = GhosthandSelectorCatalog.strategies,
                primaryStrategies = listOf("text", "contentDesc", "resourceId")
            ),
            delayedAcceptance = "required",
            exampleRequest = mapOf("condition" to mapOf("text" to "Settings"), "timeoutMs" to 3000)
        ),
        GhosthandCommandDescriptor(
            id = "clipboard_read", category = "sensing", method = "GET", path = "/clipboard",
            capabilityIds = listOf("clipboard_access"),
            description = "Read current clipboard text, including a one-read fallback to the last successful Ghosthand write if Android reports the clipboard empty immediately afterward",
            responseFields = listOf("text", "reason"),
            exampleResponse = mapOf("ok" to true, "data" to mapOf("text" to "ghosthand clip path", "reason" to "clipboard_cached_after_write"))
        ),
        GhosthandCommandDescriptor(
            id = "clipboard_write", category = "sensing", method = "POST", path = "/clipboard",
            capabilityIds = listOf("clipboard_access"),
            description = "Write clipboard text",
            responseFields = listOf("written"),
            params = listOf(GhosthandCommandParam("text", "string", "body", true, "Clipboard payload"))
        )
    )
}

internal object GhosthandIntrospectionCommandCatalog {
    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "commands",
            category = "introspection",
            method = "GET",
            path = "/commands",
            capabilityIds = listOf("route_contract_catalog"),
            description = "Machine-readable Ghosthand capability catalog for local agents",
            responseFields = listOf("schemaVersion", "selectorAliases", "selectorStrategies", "commands"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "schemaVersion" to GhosthandCommandCatalog.schemaVersion,
                    "commands" to listOf(mapOf("id" to "click", "method" to "POST", "path" to "/click"))
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "capabilities",
            category = "introspection",
            method = "GET",
            path = "/capabilities",
            capabilityIds = GhosthandCapabilityDefinitions.definitions.map { it.capabilityId },
            description = "Capability-centric Ghosthand substrate surface with canonical definitions, current availability, blockers, preconditions, failure modes, directness, truth type, and route exposure.",
            responseFields = listOf("schemaVersion", "capabilities"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "schemaVersion" to "1.0",
                    "capabilities" to listOf(mapOf("capabilityId" to "accessibility_control", "domain" to "control"))
                )
            )
        )
    )
}
