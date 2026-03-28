package com.folklore25.ghosthand

data class GhosthandCommandDescriptor(
    val id: String,
    val category: String,
    val method: String,
    val path: String,
    val description: String,
    val params: List<GhosthandCommandParam> = emptyList(),
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
            params = listOf(
                GhosthandCommandParam("editable", "boolean", false, "Filter to editable elements only"),
                GhosthandCommandParam("scrollable", "boolean", false, "Filter to scrollable elements only"),
                GhosthandCommandParam("clickable", "boolean", false, "Filter to clickable elements only"),
                GhosthandCommandParam("package", "string", false, "Restrict results to a package name")
            )
        ),
        GhosthandCommandDescriptor(
            id = "tree",
            category = "read",
            method = "GET",
            path = "/tree",
            description = "Current accessibility tree snapshot",
            params = listOf(
                GhosthandCommandParam("mode", "string", false, "Tree shape to return", listOf("raw", "flat"))
            )
        ),
        GhosthandCommandDescriptor(
            id = "info",
            category = "read",
            method = "GET",
            path = "/info",
            description = "Current foreground package, activity, and tree availability"
        ),
        GhosthandCommandDescriptor(
            id = "focused",
            category = "read",
            method = "GET",
            path = "/focused",
            description = "Currently focused accessibility node",
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
            params = listOf(
                GhosthandCommandParam("x", "int", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", true, "Screen Y coordinate")
            ),
            exampleRequest = mapOf("x" to 540, "y" to 1200)
        ),
        GhosthandCommandDescriptor(
            id = "click",
            category = "interaction",
            method = "POST",
            path = "/click",
            description = "Click by nodeId or selector",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("text", "string", false, "Exact text selector"),
                GhosthandCommandParam("desc", "string", false, "Exact content description selector"),
                GhosthandCommandParam("id", "string", false, "Exact resource id selector"),
                GhosthandCommandParam("clickable", "boolean", false, "Require a clickable resolved target")
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
            params = listOf(
                GhosthandCommandParam("text", "string", false, "Exact text selector"),
                GhosthandCommandParam("desc", "string", false, "Exact content description selector"),
                GhosthandCommandParam("id", "string", false, "Exact resource id selector"),
                GhosthandCommandParam("strategy", "string", false, "Explicit strategy name"),
                GhosthandCommandParam("query", "string", false, "Explicit strategy query"),
                GhosthandCommandParam("clickable", "boolean", false, "Resolve up to a clickable target"),
                GhosthandCommandParam("index", "int", false, "Match index to return")
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
            params = listOf(
                GhosthandCommandParam("text", "string", false, "Text payload"),
                GhosthandCommandParam("append", "boolean", false, "Append to existing field content"),
                GhosthandCommandParam("clear", "boolean", false, "Clear the field before applying text")
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
            params = listOf(
                GhosthandCommandParam("nodeId", "string", true, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("text", "string", true, "Replacement text")
            ),
            exampleRequest = mapOf("nodeId" to "snap:abc123:path:0.1", "text" to "wifi")
        ),
        GhosthandCommandDescriptor(
            id = "scroll",
            category = "interaction",
            method = "POST",
            path = "/scroll",
            description = "Scroll a target node or matching container",
            params = listOf(
                GhosthandCommandParam("nodeId", "string", false, "Snapshot-scoped node identifier"),
                GhosthandCommandParam("target", "string", false, "Text target used to locate a scroll container"),
                GhosthandCommandParam("direction", "string", true, "Scroll direction", listOf("up", "down", "left", "right")),
                GhosthandCommandParam("count", "int", false, "Repeat count")
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
            params = listOf(
                GhosthandCommandParam("from", "point", true, "Start coordinate object"),
                GhosthandCommandParam("to", "point", true, "End coordinate object"),
                GhosthandCommandParam("durationMs", "long", true, "Swipe duration in milliseconds")
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
            params = listOf(
                GhosthandCommandParam("x", "int", true, "Screen X coordinate"),
                GhosthandCommandParam("y", "int", true, "Screen Y coordinate"),
                GhosthandCommandParam("durationMs", "long", false, "Press duration in milliseconds")
            )
        ),
        GhosthandCommandDescriptor(
            id = "gesture",
            category = "interaction",
            method = "POST",
            path = "/gesture",
            description = "Composite gesture or multi-stroke dispatch",
            params = listOf(
                GhosthandCommandParam("type", "string", false, "Named gesture type", listOf("pinch_in", "pinch_out")),
                GhosthandCommandParam("strokes", "stroke_array", false, "Custom stroke descriptors")
            ),
            delayedAcceptance = "recommended"
        ),
        GhosthandCommandDescriptor(
            id = "back",
            category = "interaction",
            method = "POST",
            path = "/back",
            description = "Perform system back"
        ),
        GhosthandCommandDescriptor(
            id = "home",
            category = "interaction",
            method = "POST",
            path = "/home",
            description = "Go to launcher home"
        ),
        GhosthandCommandDescriptor(
            id = "recents",
            category = "interaction",
            method = "POST",
            path = "/recents",
            description = "Open system recents"
        ),
        GhosthandCommandDescriptor(
            id = "screenshot",
            category = "sensing",
            method = "GET",
            path = "/screenshot",
            description = "Return current screenshot as base64 PNG"
        ),
        GhosthandCommandDescriptor(
            id = "notify_read",
            category = "sensing",
            method = "GET",
            path = "/notify",
            description = "Read buffered notifications",
            params = listOf(
                GhosthandCommandParam("package", "string", false, "Restrict results to one package"),
                GhosthandCommandParam("exclude", "csv", false, "Comma-separated packages to exclude")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_post",
            category = "sensing",
            method = "POST",
            path = "/notify",
            description = "Post a local notification",
            params = listOf(
                GhosthandCommandParam("title", "string", false, "Notification title"),
                GhosthandCommandParam("text", "string", true, "Notification body")
            )
        ),
        GhosthandCommandDescriptor(
            id = "notify_cancel",
            category = "sensing",
            method = "DELETE",
            path = "/notify",
            description = "Cancel a posted local notification",
            params = listOf(
                GhosthandCommandParam("notificationId", "int", true, "Notification identifier")
            )
        ),
        GhosthandCommandDescriptor(
            id = "wait_ui_change",
            category = "sensing",
            method = "GET",
            path = "/wait",
            description = "Wait for UI change",
            params = listOf(
                GhosthandCommandParam("timeout", "long", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", false, "Polling interval in milliseconds")
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
            params = listOf(
                GhosthandCommandParam("condition", "selector", true, "Selector object for the awaited condition"),
                GhosthandCommandParam("timeoutMs", "long", false, "Maximum wait duration in milliseconds"),
                GhosthandCommandParam("intervalMs", "long", false, "Polling interval in milliseconds")
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
            description = "Read current clipboard text"
        ),
        GhosthandCommandDescriptor(
            id = "clipboard_write",
            category = "sensing",
            method = "POST",
            path = "/clipboard",
            description = "Write clipboard text",
            params = listOf(
                GhosthandCommandParam("text", "string", true, "Clipboard payload")
            )
        ),
        GhosthandCommandDescriptor(
            id = "commands",
            category = "introspection",
            method = "GET",
            path = "/commands",
            description = "Machine-readable Ghosthand capability catalog for local agents",
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
