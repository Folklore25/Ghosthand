/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

data class RoutePolicy(
    val allowedMethods: Set<String>,
    val methodNotAllowedMessage: String
)

object GhosthandRoutePolicies {
    private val policies = linkedMapOf(
        "/ping" to RoutePolicy(setOf("GET"), "Only GET is supported for /ping."),
        "/health" to RoutePolicy(setOf("GET"), "Only GET is supported for /health."),
        "/commands" to RoutePolicy(setOf("GET"), "Only GET is supported for /commands."),
        "/state" to RoutePolicy(setOf("GET"), "Only GET is supported for /state."),
        "/device" to RoutePolicy(setOf("GET"), "Only GET is supported for /device."),
        "/foreground" to RoutePolicy(setOf("GET"), "Only GET is supported for /foreground."),
        "/tree" to RoutePolicy(setOf("GET"), "Only GET is supported for /tree."),
        "/find" to RoutePolicy(setOf("POST"), "Only POST is supported for /find."),
        "/tap" to RoutePolicy(setOf("POST"), "Only POST is supported for /tap."),
        "/swipe" to RoutePolicy(setOf("POST"), "Only POST is supported for /swipe."),
        "/type" to RoutePolicy(setOf("POST"), "Only POST is supported for /type."),
        "/screen" to RoutePolicy(setOf("GET"), "Only GET is supported for /screen."),
        "/screenshot" to RoutePolicy(setOf("GET", "POST"), "Only GET and POST are supported for /screenshot."),
        "/info" to RoutePolicy(setOf("GET"), "Only GET is supported for /info."),
        "/focused" to RoutePolicy(setOf("GET"), "Only GET is supported for /focused."),
        "/click" to RoutePolicy(setOf("POST"), "Only POST is supported for /click."),
        "/input" to RoutePolicy(setOf("POST"), "Only POST is supported for /input."),
        "/setText" to RoutePolicy(setOf("POST"), "Only POST is supported for /setText."),
        "/scroll" to RoutePolicy(setOf("POST"), "Only POST is supported for /scroll."),
        "/longpress" to RoutePolicy(setOf("POST"), "Only POST is supported for /longpress."),
        "/gesture" to RoutePolicy(setOf("POST"), "Only POST is supported for /gesture."),
        "/back" to RoutePolicy(setOf("POST"), "Only POST is supported for /back."),
        "/home" to RoutePolicy(setOf("POST"), "Only POST is supported for /home."),
        "/recents" to RoutePolicy(setOf("POST"), "Only POST is supported for /recents."),
        "/clipboard" to RoutePolicy(setOf("GET", "POST"), "Only GET (read) and POST (write) are supported for /clipboard."),
        "/wait" to RoutePolicy(setOf("GET", "POST"), "Only GET and POST are supported for /wait."),
        "/notify" to RoutePolicy(setOf("GET", "POST", "DELETE"), "Only GET (read), POST (post), and DELETE (cancel) are supported for /notify.")
    )

    fun policyFor(path: String): RoutePolicy? = policies[path]
}
