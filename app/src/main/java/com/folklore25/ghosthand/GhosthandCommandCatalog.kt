/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class GhosthandCommandDescriptor(
    val id: String,
    val category: String,
    val method: String,
    val path: String,
    val description: String,
    val params: List<GhosthandCommandParam> = emptyList(),
    val responseFields: List<String> = emptyList(),
    val selectorSupport: GhosthandSelectorSupport? = null,
    val focusRequirement: String = "none",
    val delayedAcceptance: String = "none",
    val transportContract: String = "default",
    val stateTruth: String = "none",
    val changeSignal: String = "none",
    val operatorUses: List<String> = emptyList(),
    val referenceStability: String = "not_applicable",
    val snapshotScope: String = "not_applicable",
    val recommendedInteractionModel: String = "none",
    val stability: String = "stable",
    val exampleRequest: Map<String, Any?>? = null,
    val exampleResponse: Map<String, Any?>? = null
)

data class GhosthandCommandParam(
    val name: String,
    val type: String,
    val location: String,
    val required: Boolean,
    val description: String,
    val allowedValues: List<String> = emptyList()
)

data class GhosthandSelectorSupport(
    val aliases: List<String>,
    val strategies: List<String>,
    val primaryStrategies: List<String> = emptyList(),
    val boundedAids: List<String> = emptyList()
)

object GhosthandCommandCatalog {
    const val schemaVersion = "1.19"

    val selectorAliases: Map<String, String> = linkedMapOf(
        "text" to "text",
        "desc" to "contentDesc",
        "id" to "resourceId"
    )

    val selectorStrategies: List<String> = listOf(
        "text",
        "textContains",
        "resourceId",
        "contentDesc",
        "contentDescContains",
        "focused"
    )

    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "ping",
            category = "read",
            method = "GET",
            path = "/ping",
            description = "Health check with current Ghosthand version",
            responseFields = listOf("service", "version"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "service" to "ghosthand",
                    "version" to "1.0 (1)"
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "foreground",
            category = "read",
            method = "GET",
            path = "/foreground",
            description = "Current foreground app/activity summary; observer context only, not the sole visible-surface truth source",
            responseFields = listOf("packageName", "activity", "label", "timestamp"),
            stateTruth = "observer_context",
            operatorUses = listOf("observer_context")
        ),
        GhosthandCommandDescriptor(
            id = "screen",
            category = "read",
            method = "GET",
            path = "/screen",
            description = "Current actionable surface snapshot. `source=accessibility` keeps the default structured tree-first read, while explicit `ocr` or bounded `hybrid` modes expose OCR-derived elements with source provenance when accessibility output is operationally insufficient",
            responseFields = listOf("packageName", "activity", "snapshotToken", "capturedAt", "foregroundStableDuringCapture", "partialOutput", "candidateNodeCount", "returnedElementCount", "warnings", "omittedInvalidBoundsCount", "omittedLowSignalCount", "omittedNodeCount", "source", "accessibilityElementCount", "ocrElementCount", "usedOcrFallback", "elements", "disclosure"),
            stateTruth = "structured_actionable_surface_snapshot",
            operatorUses = listOf("structured_actionable_surface_snapshot", "selector_planning"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("source", "string", "query", false, "Read source mode", listOf("accessibility", "ocr", "hybrid")),
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
            description = "Current accessibility tree snapshot with explicit trust signaling for invalid bounds, low-signal nodes, and whether the current output is structurally full",
            responseFields = listOf("packageName", "activity", "snapshotToken", "capturedAt", "foregroundStableDuringCapture", "partialOutput", "returnedNodeCount", "warnings", "invalidBoundsCount", "lowSignalCount"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("mode", "string", "query", false, "Tree shape to return", listOf("raw", "flat"))
            )
        ),
        GhosthandCommandDescriptor(
            id = "info",
            category = "read",
            method = "GET",
            path = "/info",
            description = "Current foreground package, activity, and tree availability",
            responseFields = listOf("package", "activity", "label", "screen", "tree"),
            stateTruth = "mixed_state_summary"
        ),
        GhosthandCommandDescriptor(
            id = "focused",
            category = "read",
            method = "GET",
            path = "/focused",
            description = "Currently focused accessibility node",
            responseFields = listOf("available", "node", "reason"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "available" to true,
                    "node" to mapOf(
                        "resourceId" to "android:id/input",
                        "focused" to true
                    )
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "tap",
            category = "interaction",
            method = "POST",
            path = "/tap",
            description = "Tap exact screen coordinates",
            responseFields = listOf("performed", "backendUsed"),
            transportContract = "prompt_completion",
            params = listOf(
                GhosthandCommandParam("x", "int", "body", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", "body", true, "Screen Y coordinate")
            ),
            exampleRequest = mapOf("x" to 540, "y" to 1200)
        ),
        GhosthandCommandDescriptor(
            id = "click",
            category = "interaction",
            method = "POST",
            path = "/click",
            description = "Click by nodeId or first-class selector (text, contentDesc, resourceId); selector-based click resolves to an actionable clickable target by default, can cross between text and contentDesc through a bounded fallback chain, reports the requested-vs-matched selector truth on the dispatched target, returns bounded failure categories plus selector/actionability evidence on selector misses, and may include compact disclosure when selector/actionability assumptions are easy to misread",
            responseFields = listOf("performed", "stateChanged", "backendUsed", "attemptedPath", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "resolution", "failureCategory", "selectorMatchCount", "actionableMatchCount", "disclosure"),
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
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "performed" to true,
                    "attemptedPath" to "node_click",
                    "resolution" to mapOf(
                        "requestedStrategy" to "contentDesc",
                        "effectiveStrategy" to "contentDesc",
                        "requestedSurface" to "contentDesc",
                        "matchedSurface" to "contentDesc",
                        "requestedMatchSemantics" to "exact",
                        "matchedMatchSemantics" to "exact",
                        "usedSurfaceFallback" to false,
                        "usedContainsFallback" to false,
                        "matchedNodeClickable" to true,
                        "resolutionKind" to "matched_node"
                    )
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "find",
            category = "interaction",
            method = "POST",
            path = "/find",
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
                GhosthandCommandParam("strategy", "string", "body", false, "Explicit strategy name", selectorStrategies),
                GhosthandCommandParam("query", "string", "body", false, "Explicit strategy query"),
                GhosthandCommandParam("clickable", "boolean", "body", false, "Resolve up to a clickable target"),
                GhosthandCommandParam("index", "int", "body", false, "Bounded aid to select one match when a selector returns multiple results")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = selectorStrategies,
                primaryStrategies = listOf("text", "contentDesc", "resourceId"),
                boundedAids = listOf("index")
            ),
            exampleRequest = mapOf("desc" to "Settings", "clickable" to true),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "matchCount" to 1,
                    "centerX" to 540,
                    "centerY" to 640
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "input",
            category = "interaction",
            method = "POST",
            path = "/input",
            description = "Explicit focused-input interaction route: mutate text, dispatch Enter, or request both in sequence without implicitly clearing existing text",
            responseFields = listOf("performed", "textChanged", "keyDispatched", "textMutation", "keyDispatch"),
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
            id = "set_text",
            category = "interaction",
            method = "POST",
            path = "/setText",
            description = "Set text on a specific editable node; nodeId is snapshot-ephemeral and should only be used within the same trusted snapshot context",
            responseFields = listOf("performed", "backendUsed"),
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
            id = "scroll",
            category = "interaction",
            method = "POST",
            path = "/scroll",
            description = "Scroll a target node or matching container; use contentChanged as the primary same-activity effect signal, with before/after snapshot tokens for supporting detail",
            responseFields = listOf("performed", "count", "direction", "attemptedPath", "contentChanged", "surfaceChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "disclosure"),
            referenceStability = "snapshot_ephemeral",
            snapshotScope = "same_snapshot_only",
            recommendedInteractionModel = "selector_reresolution",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("target", "string", "body", false, "Text target used to locate a scroll container"),
                GhosthandCommandParam("direction", "string", "body", true, "Scroll direction", listOf("up", "down", "left", "right")),
                GhosthandCommandParam("count", "int", "body", false, "Repeat count")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text"),
                strategies = listOf("text"),
                primaryStrategies = listOf("text")
            ),
            delayedAcceptance = "recommended",
            exampleRequest = mapOf("direction" to "down", "count" to 1)
        ),
        GhosthandCommandDescriptor(
            id = "swipe",
            category = "interaction",
            method = "POST",
            path = "/swipe",
            description = "Swipe between two coordinates; canonical request uses from/to point objects, x1/y1/x2/y2 aliases are accepted for discoverability, and contentChanged is the primary same-activity effect signal",
            responseFields = listOf("performed", "backendUsed", "requestShape", "contentChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "disclosure"),
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
            exampleRequest = mapOf(
                "from" to mapOf("x" to 540, "y" to 1700),
                "to" to mapOf("x" to 540, "y" to 500),
                "durationMs" to 300
            )
        ),
        GhosthandCommandDescriptor(
            id = "longpress",
            category = "interaction",
            method = "POST",
            path = "/longpress",
            description = "Long press at coordinates",
            responseFields = listOf("performed"),
            params = listOf(
                GhosthandCommandParam("x", "int", "body", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", "body", true, "Screen Y coordinate"),
                GhosthandCommandParam("durationMs", "long", "body", false, "Press duration in milliseconds")
            )
        ),
        GhosthandCommandDescriptor(
            id = "gesture",
            category = "interaction",
            method = "POST",
            path = "/gesture",
            description = "Composite gesture or multi-stroke dispatch",
            responseFields = listOf("performed"),
            params = listOf(
                GhosthandCommandParam("type", "string", "body", false, "Named gesture type", listOf("pinch_in", "pinch_out")),
                GhosthandCommandParam("strokes", "stroke_array", "body", false, "Custom stroke descriptors")
            ),
            delayedAcceptance = "recommended"
        ),
        GhosthandCommandDescriptor(
            id = "launch",
            category = "interaction",
            method = "POST",
            path = "/launch",
            description = "Launch an installed app by package name through the standard Android package launch intent path",
            responseFields = listOf("launched", "packageName", "label", "strategy", "reason"),
            transportContract = "prompt_completion",
            params = listOf(
                GhosthandCommandParam("packageName", "string", "body", true, "Installed Android package name to launch")
            ),
            exampleRequest = mapOf("packageName" to "com.android.settings"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "launched" to true,
                    "packageName" to "com.android.settings",
                    "label" to "Settings",
                    "strategy" to "package_launch_intent",
                    "reason" to "launched"
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "back",
            category = "interaction",
            method = "POST",
            path = "/back",
            description = "Perform system back and report bounded observed effect fields alongside dispatch truth",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "disclosure"),
            transportContract = "prompt_completion"
        ),
        GhosthandCommandDescriptor(
            id = "home",
            category = "interaction",
            method = "POST",
            path = "/home",
            description = "Go to launcher home and report bounded observed effect fields alongside dispatch truth",
            responseFields = listOf("performed", "attemptedPath", "stateChanged", "beforeSnapshotToken", "afterSnapshotToken", "finalPackageName", "finalActivity", "disclosure"),
            transportContract = "prompt_completion"
        ),
        GhosthandCommandDescriptor(
            id = "recents",
            category = "interaction",
            method = "POST",
            path = "/recents",
            description = "Open system recents",
            responseFields = listOf("performed"),
            transportContract = "prompt_completion"
        ),
        GhosthandCommandDescriptor(
            id = "screenshot",
            category = "sensing",
            method = "GET",
            path = "/screenshot",
            description = "Return current screenshot as base64 PNG; primary visual truth for debugging and verification when structured surface output is stale, invalid, or ambiguous",
            responseFields = listOf("image", "width", "height"),
            stateTruth = "visual_truth",
            operatorUses = listOf("visual_truth", "debugging", "verification")
        ),
        GhosthandCommandDescriptor(
            id = "notify_read",
            category = "sensing",
            method = "GET",
            path = "/notify",
            description = "Read buffered notifications",
            responseFields = listOf("notifications"),
            params = listOf(
                GhosthandCommandParam("package", "string", "query", false, "Restrict results to one package"),
                GhosthandCommandParam("exclude", "csv", "query", false, "Comma-separated packages to exclude")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_post",
            category = "sensing",
            method = "POST",
            path = "/notify",
            description = "Post a local notification",
            responseFields = listOf("posted", "notificationId"),
            params = listOf(
                GhosthandCommandParam("title", "string", "body", false, "Notification title"),
                GhosthandCommandParam("text", "string", "body", true, "Notification body")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_cancel",
            category = "sensing",
            method = "DELETE",
            path = "/notify",
            description = "Cancel a posted local notification",
            responseFields = listOf("canceled"),
            params = listOf(
                GhosthandCommandParam("notificationId", "int", "body", true, "Notification identifier")
            )
        ),
        GhosthandCommandDescriptor(
            id = "wait_ui_change",
            category = "sensing",
            method = "GET",
            path = "/wait",
            description = "Wait for UI change; changed is kept for compatibility, while conditionMet, stateChanged, and timedOut separate wait outcome truth from the final observed settled state",
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
            id = "wait_condition",
            category = "sensing",
            method = "POST",
            path = "/wait",
            description = "Wait for a matching tree condition; satisfied is kept for compatibility, while conditionMet, stateChanged, and timedOut separate selector success from broader surface change",
            responseFields = listOf("satisfied", "conditionMet", "stateChanged", "timedOut", "elapsedMs", "node", "reason", "disclosure"),
            params = listOf(
                GhosthandCommandParam("condition", "selector", "body", true, "Selector object for the awaited condition"),
                GhosthandCommandParam("timeoutMs", "long", "body", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", "body", false, "Polling interval in milliseconds")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = selectorStrategies,
                primaryStrategies = listOf("text", "contentDesc", "resourceId")
            ),
            delayedAcceptance = "required",
            exampleRequest = mapOf(
                "condition" to mapOf("text" to "Settings"),
                "timeoutMs" to 3000
            )
        ),
        GhosthandCommandDescriptor(
            id = "clipboard_read",
            category = "sensing",
            method = "GET",
            path = "/clipboard",
            description = "Read current clipboard text, including a one-read fallback to the last successful Ghosthand write if Android reports the clipboard empty immediately afterward",
            responseFields = listOf("text", "reason"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "text" to "ghosthand clip path",
                    "reason" to "clipboard_cached_after_write"
                )
            )
        ),
        GhosthandCommandDescriptor(
            id = "clipboard_write",
            category = "sensing",
            method = "POST",
            path = "/clipboard",
            description = "Write clipboard text",
            responseFields = listOf("written"),
            params = listOf(
                GhosthandCommandParam("text", "string", "body", true, "Clipboard payload")
            )
        ),
        GhosthandCommandDescriptor(
            id = "commands",
            category = "introspection",
            method = "GET",
            path = "/commands",
            description = "Machine-readable Ghosthand capability catalog for local agents",
            responseFields = listOf("schemaVersion", "selectorAliases", "selectorStrategies", "commands"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf(
                    "schemaVersion" to schemaVersion,
                    "commands" to listOf(
                        mapOf(
                            "id" to "click",
                            "method" to "POST",
                            "path" to "/click"
                        )
                    )
                )
            )
        )
    )
}
