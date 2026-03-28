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
    val strategies: List<String>
)

object GhosthandCommandCatalog {
    const val schemaVersion = "1.2"

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
            id = "screen",
            category = "read",
            method = "GET",
            path = "/screen",
            description = "Current UI elements with bounds and action-ready center coordinates",
            responseFields = listOf("packageName", "activity", "snapshotToken", "capturedAt", "elements"),
            params = listOf(
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
            description = "Current accessibility tree snapshot",
            responseFields = listOf("packageName", "activity", "snapshotToken", "capturedAt", "root"),
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
            responseFields = listOf("package", "activity", "label", "screen", "tree")
        ),
        GhosthandCommandDescriptor(
            id = "focused",
            category = "read",
            method = "GET",
            path = "/focused",
            description = "Currently focused accessibility node",
            responseFields = listOf("available", "node", "reason"),
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
            description = "Click by nodeId or selector",
            responseFields = listOf("performed", "backendUsed"),
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("text", "string", "body", false, "Exact text selector"),
                GhosthandCommandParam("desc", "string", "body", false, "Exact content description selector"),
                GhosthandCommandParam("id", "string", "body", false, "Exact resource id selector"),
                GhosthandCommandParam("clickable", "boolean", "body", false, "Require a clickable resolved target")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = listOf("text", "resourceId", "contentDesc")
            ),
            exampleRequest = mapOf("text" to "Settings"),
            exampleResponse = mapOf(
                "ok" to true,
                "data" to mapOf("performed" to true)
            )
        ),
        GhosthandCommandDescriptor(
            id = "find",
            category = "interaction",
            method = "POST",
            path = "/find",
            description = "Find a node and return action-ready geometry",
            responseFields = listOf("found", "matchCount", "index", "node", "text", "desc", "id", "bounds", "centerX", "centerY", "clickable", "editable", "scrollable"),
            params = listOf(
                GhosthandCommandParam("text", "string", "body", false, "Exact text selector"),
                GhosthandCommandParam("desc", "string", "body", false, "Exact content description selector"),
                GhosthandCommandParam("id", "string", "body", false, "Exact resource id selector"),
                GhosthandCommandParam("strategy", "string", "body", false, "Explicit strategy name"),
                GhosthandCommandParam("query", "string", "body", false, "Explicit strategy query"),
                GhosthandCommandParam("clickable", "boolean", "body", false, "Resolve up to a clickable target"),
                GhosthandCommandParam("index", "int", "body", false, "Match index to return")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = selectorStrategies
            ),
            exampleRequest = mapOf("text" to "Settings", "clickable" to true),
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
            description = "Set, append, or clear text in the current focused field",
            responseFields = listOf("performed", "backendUsed", "text", "previousText", "action"),
            params = listOf(
                GhosthandCommandParam("text", "string", "body", false, "Text payload"),
                GhosthandCommandParam("append", "boolean", "body", false, "Append to existing field content"),
                GhosthandCommandParam("clear", "boolean", "body", false, "Clear the field before applying text")
            ),
            focusRequirement = "focused_editable",
            exampleRequest = mapOf("text" to "wifi", "clear" to true)
        ),
        GhosthandCommandDescriptor(
            id = "set_text",
            category = "interaction",
            method = "POST",
            path = "/setText",
            description = "Set text on a specific editable node",
            responseFields = listOf("performed", "backendUsed"),
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
            description = "Scroll a target node or matching container",
            responseFields = listOf("performed", "count", "direction", "attemptedPath"),
            params = listOf(
                GhosthandCommandParam("nodeId", "string", "body", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("target", "string", "body", false, "Text target used to locate a scroll container"),
                GhosthandCommandParam("direction", "string", "body", true, "Scroll direction", listOf("up", "down", "left", "right")),
                GhosthandCommandParam("count", "int", "body", false, "Repeat count")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text"),
                strategies = listOf("text")
            ),
            delayedAcceptance = "recommended",
            exampleRequest = mapOf("direction" to "down", "count" to 1)
        ),
        GhosthandCommandDescriptor(
            id = "swipe",
            category = "interaction",
            method = "POST",
            path = "/swipe",
            description = "Swipe between two coordinates",
            responseFields = listOf("performed", "backendUsed"),
            params = listOf(
                GhosthandCommandParam("from", "point", "body", true, "Start coordinate object"),
                GhosthandCommandParam("to", "point", "body", true, "End coordinate object"),
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
            id = "back",
            category = "interaction",
            method = "POST",
            path = "/back",
            description = "Perform system back",
            responseFields = listOf("performed")
        ),
        GhosthandCommandDescriptor(
            id = "home",
            category = "interaction",
            method = "POST",
            path = "/home",
            description = "Go to launcher home",
            responseFields = listOf("performed")
        ),
        GhosthandCommandDescriptor(
            id = "recents",
            category = "interaction",
            method = "POST",
            path = "/recents",
            description = "Open system recents",
            responseFields = listOf("performed")
        ),
        GhosthandCommandDescriptor(
            id = "screenshot",
            category = "sensing",
            method = "GET",
            path = "/screenshot",
            description = "Return current screenshot as base64 PNG",
            responseFields = listOf("image", "width", "height")
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
            description = "Wait for UI change",
            responseFields = listOf("changed", "elapsedMs", "snapshotToken", "packageName", "activity"),
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
            description = "Wait for a matching tree condition",
            responseFields = listOf("satisfied", "elapsedMs", "node", "reason"),
            params = listOf(
                GhosthandCommandParam("condition", "selector", "body", true, "Selector object for the awaited condition"),
                GhosthandCommandParam("timeoutMs", "long", "body", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", "body", false, "Polling interval in milliseconds")
            ),
            selectorSupport = GhosthandSelectorSupport(
                aliases = listOf("text", "desc", "id"),
                strategies = selectorStrategies
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
            description = "Read current clipboard text",
            responseFields = listOf("text", "reason")
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
