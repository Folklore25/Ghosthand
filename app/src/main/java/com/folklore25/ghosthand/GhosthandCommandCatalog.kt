package com.folklore25.ghosthand

data class GhosthandCommandDescriptor(
    val id: String,
    val category: String,
    val method: String,
    val path: String,
    val description: String,
    val params: List<GhosthandCommandParam> = emptyList()
)

data class GhosthandCommandParam(
    val name: String,
    val type: String,
    val required: Boolean,
    val description: String,
    val allowedValues: List<String> = emptyList()
)

object GhosthandCommandCatalog {
    val commands: List<GhosthandCommandDescriptor> = listOf(
        GhosthandCommandDescriptor(
            id = "ping",
            category = "read",
            method = "GET",
            path = "/ping",
            description = "Health check with current Ghosthand version"
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
            description = "Currently focused accessibility node"
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
            )
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
            )
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
            )
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
            )
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
            )
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
            )
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
            description = "Machine-readable Ghosthand capability catalog for local agents"
        )
    )
}
