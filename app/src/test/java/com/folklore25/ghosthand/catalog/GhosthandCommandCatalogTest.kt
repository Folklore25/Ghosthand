/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

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

import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.routes.GhosthandRoutePolicies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandCommandCatalogTest {
    @Test
    fun routeGroupsAndSharedMetadataComposeThePublishedCatalog() {
        assertEquals(GhosthandSelectorCatalog.aliases, GhosthandCommandCatalog.selectorAliases)
        assertEquals(GhosthandSelectorCatalog.strategies, GhosthandCommandCatalog.selectorStrategies)
        assertEquals(
            GhosthandReadCommandCatalog.commands +
                GhosthandInteractionCommandCatalog.commands +
                GhosthandSensingCommandCatalog.commands +
                GhosthandIntrospectionCommandCatalog.commands,
            GhosthandCommandCatalog.commands
        )
    }

    @Test
    fun commandsHaveUniqueMethodPathPairs() {
        val pairs = GhosthandCommandCatalog.commands.map { it.method to it.path }
        assertEquals(pairs.size, pairs.toSet().size)
    }

    @Test
    fun commandsExposeMachineReadableMetadata() {
        val commandsRoute = GhosthandCommandCatalog.commands.first { it.path == "/commands" && it.method == "GET" }
        assertEquals("commands", commandsRoute.id)
        assertEquals("introspection", commandsRoute.category)
        assertTrue(commandsRoute.description.isNotBlank())
        assertEquals("1.25", GhosthandCommandCatalog.schemaVersion)
        assertNotNull(commandsRoute.exampleResponse)
    }

    @Test
    fun commandPayloadsPreserveFinalSurfaceWithoutMeaninglessDefaultMetadata() {
        val screenPayload = GhosthandCommandCatalog.commandPayloads().first { it["id"] == "screen" }
        assertEquals("/screen", screenPayload["path"])
        assertEquals(
            GhosthandSelectorCatalog.screenResponseFields,
            screenPayload["responseFields"]
        )
        assertEquals("structured_actionable_surface_snapshot", screenPayload["stateTruth"])
        assertEquals("observation", screenPayload["plane"])
        assertEquals("source_dependent_runtime_gated", screenPayload["availabilityModel"])
        assertEquals("structured_surface_observation", screenPayload["truthType"])
        assertEquals("mixed", screenPayload["directness"])
        assertTrue((screenPayload["preconditions"] as List<*>).contains("accessibility_when_source=accessibility"))
        assertTrue((screenPayload["failureModes"] as List<*>).contains("ocr_capture_unavailable"))
        assertEquals("selector_reresolution", screenPayload["recommendedInteractionModel"])
        assertEquals("snapshot_ephemeral", screenPayload["referenceStability"])
        assertEquals("same_snapshot_only", screenPayload["snapshotScope"])
        assertNull(screenPayload["selectorSupport"])
        assertNull(screenPayload["exampleRequest"])
        assertNull(screenPayload["exampleResponse"])
        assertNull(screenPayload["focusRequirement"])
        assertNull(screenPayload["delayedAcceptance"])
        assertNull(screenPayload["changeSignal"])
        val screenParams = screenPayload["params"] as List<*>
        val sourceParam = screenParams.first { (it as Map<*, *>)["name"] == "source" } as Map<*, *>
        assertEquals(listOf("accessibility", "ocr", "hybrid"), sourceParam["allowedValues"])
        val packageParam = screenParams.first { (it as Map<*, *>)["name"] == "package" } as Map<*, *>
        assertFalse(packageParam.containsKey("allowedValues"))

        val clickPayload = GhosthandCommandCatalog.commandPayloads().first { it["id"] == "click" }
        assertEquals("control", clickPayload["plane"])
        assertEquals("accessibility_runtime_gated", clickPayload["availabilityModel"])
        assertEquals("action_truth", clickPayload["truthType"])
        assertEquals("direct", clickPayload["directness"])
        assertTrue((clickPayload["failureModes"] as List<*>).contains("stale_node_reference"))
        assertEquals(listOf("accessibility_control"), clickPayload["capabilityIds"])
        val clickCapabilities = clickPayload["capabilities"] as List<*>
        val clickCapability = clickCapabilities.single() as Map<*, *>
        assertEquals("accessibility_control", clickCapability["capabilityId"])
        assertEquals("control", clickCapability["domain"])
        assertEquals("primitive", clickCapability["kind"])
        assertEquals("direct", clickCapability["directness"])
        assertTrue(clickPayload.containsKey("selectorSupport"))
        assertTrue(clickPayload.containsKey("exampleRequest"))
        assertTrue(clickPayload.containsKey("exampleResponse"))
        assertEquals("none", clickPayload["focusRequirement"])

        val commandsPayload = GhosthandCommandCatalog.commandPayloads().first { it["id"] == "commands" }
        assertEquals("/commands", commandsPayload["path"])
        assertEquals("capability", commandsPayload["plane"])
        assertEquals("capability_truth", commandsPayload["truthType"])
        assertEquals(listOf("route_contract_catalog"), commandsPayload["capabilityIds"])
        val commandsCapabilities = commandsPayload["capabilities"] as List<*>
        assertEquals("route_contract_catalog", (commandsCapabilities.single() as Map<*, *>)["capabilityId"])
        assertNull(commandsPayload["selectorSupport"])
        assertNull(commandsPayload["exampleRequest"])
        assertNotNull(commandsPayload["exampleResponse"])
        assertNull(commandsPayload["operatorUses"])
    }

    @Test
    fun waitAndClickRoutesExposeStructuredParams() {
        val waitCondition = GhosthandCommandCatalog.commands.first { it.id == "wait_condition" }
        val selectorParam = waitCondition.params.first { it.name == "condition" }
        assertEquals("selector", selectorParam.type)
        assertEquals("body", selectorParam.location)
        assertTrue(selectorParam.required)

        val clickRoute = GhosthandCommandCatalog.commands.first { it.id == "click" }
        val paramNames = clickRoute.params.map { it.name }.toSet()
        assertTrue(paramNames.contains("nodeId"))
        assertTrue(paramNames.contains("text"))
        assertTrue(paramNames.contains("desc"))
        assertTrue(paramNames.contains("id"))
    }

    @Test
    fun everyCommandHasCompatibleRoutePolicy() {
        GhosthandCommandCatalog.commands.forEach { command ->
            val policy = GhosthandRoutePolicies.policyFor(command.path)
            assertTrue(
                "Missing route policy for ${command.path}",
                policy != null
            )
            assertTrue(
                "Policy for ${command.path} does not allow ${command.method}",
                policy!!.allowedMethods.contains(command.method)
            )
        }
    }

    @Test
    fun selectorMetadataIsExplicitForAgents() {
        assertEquals("text", GhosthandCommandCatalog.selectorAliases["text"])
        assertEquals("contentDesc", GhosthandCommandCatalog.selectorAliases["desc"])
        assertEquals("resourceId", GhosthandCommandCatalog.selectorAliases["id"])
        assertTrue(GhosthandCommandCatalog.selectorStrategies.contains("focused"))
        assertTrue(GhosthandCommandCatalog.selectorStrategies.contains("textContains"))

        val findRoute = GhosthandCommandCatalog.commands.first { it.id == "find" }
        assertEquals(listOf("text", "contentDesc", "resourceId"), findRoute.selectorSupport!!.primaryStrategies)
        assertTrue(findRoute.selectorSupport!!.boundedAids.contains("index"))
    }

    @Test
    fun interactiveCommandsExposeFocusSelectorAndAcceptanceHints() {
        val clickRoute = GhosthandCommandCatalog.commands.first { it.id == "click" }
        assertNotNull(clickRoute.selectorSupport)
        assertEquals(listOf("text", "desc", "id"), clickRoute.selectorSupport!!.aliases)
        assertEquals(listOf("text", "contentDesc", "resourceId"), clickRoute.selectorSupport!!.primaryStrategies)
        assertEquals("none", clickRoute.focusRequirement)
        assertEquals("none", clickRoute.delayedAcceptance)
        assertEquals("prompt_completion", clickRoute.transportContract)
        assertTrue(clickRoute.operatorUses.contains("content_desc_selector"))
        assertTrue(clickRoute.responseFields.contains("performed"))
        assertTrue(clickRoute.responseFields.contains("stateChanged"))
        assertTrue(clickRoute.responseFields.contains("backendUsed"))
        assertTrue(clickRoute.responseFields.contains("attemptedPath"))
        assertTrue(clickRoute.responseFields.contains("resolution"))
        assertTrue(clickRoute.responseFields.contains("failureCategory"))
        assertTrue(clickRoute.responseFields.contains("selectorMatchCount"))
        assertTrue(clickRoute.responseFields.contains("actionableMatchCount"))
        assertTrue(clickRoute.responseFields.contains("postActionState"))
        assertTrue(clickRoute.responseFields.contains("disclosure"))
        assertTrue(clickRoute.description.contains("actionable clickable target by default"))
        assertTrue(clickRoute.description.contains("requested-vs-matched selector truth"))
        assertTrue(clickRoute.description.contains("failure categories"))
        assertTrue(clickRoute.description.contains("stale nodeId reference"))
        assertTrue(clickRoute.description.contains("modal transitions"))
        assertTrue(clickRoute.description.contains("contentDesc"))
        assertTrue(clickRoute.description.contains("compact post-action state summary"))
        assertNotNull(clickRoute.exampleRequest)
        assertNotNull(clickRoute.exampleResponse)

        val inputRoute = GhosthandCommandCatalog.commands.first { it.id == "input" }
        assertEquals("focused_editable", inputRoute.focusRequirement)
        assertEquals("stable", inputRoute.stability)
        assertTrue(inputRoute.responseFields.contains("textChanged"))
        assertTrue(inputRoute.responseFields.contains("keyDispatched"))
        assertTrue(inputRoute.responseFields.contains("textMutation"))
        assertTrue(inputRoute.responseFields.contains("keyDispatch"))
        assertTrue(inputRoute.responseFields.contains("postActionState"))
        val inputParamNames = inputRoute.params.map { it.name }
        assertTrue(inputParamNames.contains("textAction"))
        assertTrue(inputParamNames.contains("key"))
        assertNotNull(inputRoute.exampleRequest)
        assertFalse(inputRoute.params.isEmpty())

        val swipeRoute = GhosthandCommandCatalog.commands.first { it.id == "swipe" }
        assertEquals("recommended", swipeRoute.delayedAcceptance)
        assertTrue(swipeRoute.responseFields.contains("performed"))
        assertTrue(swipeRoute.responseFields.contains("requestShape"))
        assertTrue(swipeRoute.responseFields.contains("contentChanged"))
        assertTrue(swipeRoute.responseFields.contains("beforeSnapshotToken"))
        assertTrue(swipeRoute.responseFields.contains("afterSnapshotToken"))
        assertTrue(swipeRoute.responseFields.contains("postActionState"))
        assertTrue(swipeRoute.responseFields.contains("disclosure"))
        val swipeParamNames = swipeRoute.params.map { it.name }
        assertTrue(swipeParamNames.contains("from"))
        assertTrue(swipeParamNames.contains("to"))
        assertTrue(swipeParamNames.contains("x1"))
        assertTrue(swipeParamNames.contains("y1"))
        assertTrue(swipeParamNames.contains("x2"))
        assertTrue(swipeParamNames.contains("y2"))

        val waitConditionRoute = GhosthandCommandCatalog.commands.first { it.id == "wait_condition" }
        assertEquals("required", waitConditionRoute.delayedAcceptance)
        assertNotNull(waitConditionRoute.selectorSupport)
        assertTrue(waitConditionRoute.selectorSupport!!.strategies.contains("focused"))
        assertTrue(waitConditionRoute.description.contains("same selector aliases"))
        assertTrue(waitConditionRoute.responseFields.contains("satisfied"))
        assertTrue(waitConditionRoute.responseFields.contains("conditionMet"))
        assertTrue(waitConditionRoute.responseFields.contains("stateChanged"))
        assertTrue(waitConditionRoute.responseFields.contains("timedOut"))
        assertTrue(waitConditionRoute.responseFields.contains("reason"))
        assertTrue(waitConditionRoute.responseFields.contains("disclosure"))
        assertTrue(waitConditionRoute.params.first { it.name == "condition" }.description.contains("text/desc/id aliases"))
        assertTrue(waitConditionRoute.description.contains("modal transitions"))
        assertTrue(waitConditionRoute.description.contains("retry-oriented wait"))

        val waitUiRoute = GhosthandCommandCatalog.commands.first { it.id == "wait_ui_change" }
        assertTrue(waitUiRoute.description.contains("conditionMet"))
        assertTrue(waitUiRoute.responseFields.contains("packageName"))
        assertTrue(waitUiRoute.responseFields.contains("activity"))
        assertTrue(waitUiRoute.responseFields.contains("conditionMet"))
        assertTrue(waitUiRoute.responseFields.contains("stateChanged"))
        assertTrue(waitUiRoute.responseFields.contains("timedOut"))
        assertTrue(waitUiRoute.responseFields.contains("disclosure"))
        assertEquals("final_settled_state", waitUiRoute.stateTruth)
        assertEquals("transition_observed_during_window", waitUiRoute.changeSignal)
        assertTrue(waitUiRoute.description.contains("retry-oriented settle path"))

        val backRoute = GhosthandCommandCatalog.commands.first { it.id == "back" }
        assertTrue(backRoute.responseFields.contains("stateChanged"))
        assertTrue(backRoute.responseFields.contains("beforeSnapshotToken"))
        assertTrue(backRoute.responseFields.contains("afterSnapshotToken"))
        assertTrue(backRoute.responseFields.contains("finalPackageName"))
        assertTrue(backRoute.responseFields.contains("finalActivity"))
        assertTrue(backRoute.responseFields.contains("postActionState"))
        assertTrue(backRoute.description.contains("observed effect"))

        val homeRoute = GhosthandCommandCatalog.commands.first { it.id == "home" }
        assertTrue(homeRoute.responseFields.contains("stateChanged"))
        assertTrue(homeRoute.responseFields.contains("beforeSnapshotToken"))
        assertTrue(homeRoute.responseFields.contains("afterSnapshotToken"))
        assertTrue(homeRoute.responseFields.contains("finalPackageName"))
        assertTrue(homeRoute.responseFields.contains("finalActivity"))
        assertTrue(homeRoute.responseFields.contains("postActionState"))
        assertTrue(homeRoute.description.contains("observed effect"))

        val setTextRoute = GhosthandCommandCatalog.commands.first { it.id == "set_text" }
        assertTrue(setTextRoute.responseFields.contains("postActionState"))
        assertTrue(setTextRoute.description.contains("compact post-action state summary"))

        val scrollRoute = GhosthandCommandCatalog.commands.first { it.id == "scroll" }
        assertTrue(scrollRoute.responseFields.contains("postActionState"))
        assertTrue(scrollRoute.description.contains("compact post-action state summary"))

        val tapRoute = GhosthandCommandCatalog.commands.first { it.id == "tap" }
        assertTrue(tapRoute.responseFields.contains("postActionState"))

        val longpressRoute = GhosthandCommandCatalog.commands.first { it.id == "longpress" }
        assertTrue(longpressRoute.responseFields.contains("postActionState"))

        val gestureRoute = GhosthandCommandCatalog.commands.first { it.id == "gesture" }
        assertTrue(gestureRoute.responseFields.contains("postActionState"))

        val recentsRoute = GhosthandCommandCatalog.commands.first { it.id == "recents" }
        assertTrue(recentsRoute.responseFields.contains("postActionState"))
    }

    @Test
    fun paramsExposeMachineReadableLocationsAndCommandsExposeResponseFields() {
        val screenRoute = GhosthandCommandCatalog.commands.first { it.id == "screen" }
        assertTrue(screenRoute.params.all { it.location == "query" })
        assertTrue(screenRoute.params.any { it.name == "source" })
        assertTrue(screenRoute.params.any { it.name == "summaryOnly" })
        assertFalse(screenRoute.params.any { it.name == "includePreview" })
        assertTrue(screenRoute.responseFields.contains("elements"))
        assertTrue(screenRoute.responseFields.contains("source"))
        assertTrue(screenRoute.responseFields.contains("renderMode"))
        assertTrue(screenRoute.responseFields.contains("surfaceReadability"))
        assertTrue(screenRoute.responseFields.contains("visualAvailable"))
        assertTrue(screenRoute.responseFields.contains("previewAvailable"))
        assertTrue(screenRoute.responseFields.contains("previewPath"))
        assertTrue(screenRoute.responseFields.contains("previewWidth"))
        assertTrue(screenRoute.responseFields.contains("previewHeight"))
        assertTrue(screenRoute.responseFields.contains("accessibilityElementCount"))
        assertTrue(screenRoute.responseFields.contains("ocrElementCount"))
        assertTrue(screenRoute.responseFields.contains("usedOcrFallback"))
        assertTrue(screenRoute.responseFields.contains("focusedEditablePresent"))
        assertTrue(screenRoute.responseFields.contains("suggestedSource"))
        assertTrue(screenRoute.responseFields.contains("fallbackReason"))
        assertFalse(screenRoute.responseFields.contains("suggestedFallback"))
        assertFalse(screenRoute.responseFields.contains("retryHint"))
        assertFalse(screenRoute.responseFields.contains("previewImage"))
        assertTrue(screenRoute.responseFields.contains("foregroundStableDuringCapture"))
        assertTrue(screenRoute.responseFields.contains("partialOutput"))
        assertTrue(screenRoute.responseFields.contains("candidateNodeCount"))
        assertTrue(screenRoute.responseFields.contains("returnedElementCount"))
        assertTrue(screenRoute.responseFields.contains("warnings"))
        assertTrue(screenRoute.responseFields.contains("omittedInvalidBoundsCount"))
        assertTrue(screenRoute.responseFields.contains("omittedLowSignalCount"))
        assertTrue(screenRoute.responseFields.contains("omittedNodeCount"))
        assertTrue(screenRoute.responseFields.contains("omittedCategories"))
        assertTrue(screenRoute.responseFields.contains("omittedSummary"))
        assertTrue(screenRoute.responseFields.contains("invalidBoundsPresent"))
        assertTrue(screenRoute.responseFields.contains("lowSignalPresent"))
        assertTrue(screenRoute.responseFields.contains("disclosure"))
        assertEquals("structured_actionable_surface_snapshot", screenRoute.stateTruth)
        assertTrue(screenRoute.operatorUses.contains("structured_actionable_surface_snapshot"))
        assertTrue(screenRoute.operatorUses.contains("selector_planning"))
        assertEquals("snapshot_ephemeral", screenRoute.referenceStability)
        assertEquals("same_snapshot_only", screenRoute.snapshotScope)
        assertEquals("selector_reresolution", screenRoute.recommendedInteractionModel)
        assertTrue(screenRoute.description.contains("source=accessibility"))
        assertTrue(screenRoute.description.contains("summaryOnly=true"))
        assertTrue(screenRoute.description.contains("/screenshot?width={previewWidth}&height={previewHeight}"))
        assertTrue(screenRoute.description.contains("hybrid"))
        assertTrue(screenRoute.description.contains("render mode"))
        assertTrue(screenRoute.description.contains("surface readability"))
        assertTrue(screenRoute.description.contains("preview availability"))
        assertTrue(screenRoute.description.contains("previewPath"))
        assertTrue(screenRoute.description.contains("fallback recommendation"))
        assertTrue(screenRoute.description.contains("modal transitions"))

        val foregroundRoute = GhosthandCommandCatalog.commands.first { it.id == "foreground" }
        assertEquals("observer_context", foregroundRoute.stateTruth)
        assertTrue(foregroundRoute.operatorUses.contains("observer_context"))
        assertTrue(foregroundRoute.description.contains("observer context"))

        val screenshotRoute = GhosthandCommandCatalog.commands.first { it.id == "screenshot" }
        assertEquals("visual_truth", screenshotRoute.stateTruth)
        assertTrue(screenshotRoute.operatorUses.contains("visual_truth"))
        assertTrue(screenshotRoute.operatorUses.contains("debugging"))
        assertTrue(screenshotRoute.operatorUses.contains("verification"))
        assertTrue(screenshotRoute.description.contains("visual truth"))

        val clipboardReadRoute = GhosthandCommandCatalog.commands.first { it.id == "clipboard_read" }
        assertTrue(clipboardReadRoute.description.contains("one-read fallback"))
        assertNotNull(clipboardReadRoute.exampleResponse)
        assertTrue(clipboardReadRoute.responseFields.contains("text"))
        assertTrue(clipboardReadRoute.responseFields.contains("reason"))

        val clipboardWriteRoute = GhosthandCommandCatalog.commands.first { it.id == "clipboard_write" }
        assertEquals("body", clipboardWriteRoute.params.single().location)
        assertTrue(clipboardWriteRoute.responseFields.contains("written"))

        val commandsRoute = GhosthandCommandCatalog.commands.first { it.id == "commands" }
        assertTrue(commandsRoute.responseFields.contains("schemaVersion"))
        assertTrue(commandsRoute.responseFields.contains("commands"))

        val eventsRoute = GhosthandCommandCatalog.commands.first { it.id == "events" }
        assertEquals("/events", eventsRoute.path)
        assertTrue(eventsRoute.responseFields.contains("events"))
        assertTrue(eventsRoute.responseFields.contains("nextCursor"))
        assertTrue(eventsRoute.params.any { it.name == "since" })
        assertTrue(eventsRoute.params.any { it.name == "limit" })
        val eventsPayload = GhosthandCommandCatalog.commandPayloads().first { it["id"] == "events" }
        assertEquals("observation", eventsPayload["plane"])
        assertEquals("always_available", eventsPayload["availabilityModel"])
        assertEquals("observation_truth", eventsPayload["truthType"])
        assertEquals("derived", eventsPayload["directness"])
        assertEquals(listOf("event_observation"), eventsPayload["capabilityIds"])
        val eventsCapabilities = eventsPayload["capabilities"] as List<*>
        assertEquals("event_observation", (eventsCapabilities.single() as Map<*, *>)["capabilityId"])
        assertTrue((eventsPayload["failureModes"] as List<*>).contains("stale_cursor_window"))

        val capabilitiesPayload = GhosthandCommandCatalog.commandPayloads().first { it["id"] == "capabilities" }
        val referencedCapabilities = capabilitiesPayload["capabilities"] as List<*>
        val referencedCapabilityIds = referencedCapabilities.map { (it as Map<*, *>)["capabilityId"] }
        assertTrue(referencedCapabilityIds.contains("accessibility_control"))
        assertTrue(referencedCapabilityIds.contains("route_contract_catalog"))
    }

    @Test
    fun transportAndStateTruthMetadataAreExplicitForAgents() {
        val findRoute = GhosthandCommandCatalog.commands.first { it.id == "find" }
        assertEquals("prompt_completion", findRoute.transportContract)
        assertTrue(findRoute.operatorUses.contains("content_desc_selector"))
        assertTrue(findRoute.operatorUses.contains("index_disambiguation"))
        assertTrue(findRoute.responseFields.contains("disclosure"))
        assertTrue(findRoute.responseFields.contains("searchedSurface"))
        assertTrue(findRoute.responseFields.contains("matchSemantics"))
        assertTrue(findRoute.responseFields.contains("requestedSurface"))
        assertTrue(findRoute.responseFields.contains("requestedMatchSemantics"))
        assertTrue(findRoute.responseFields.contains("matchedSurface"))
        assertTrue(findRoute.responseFields.contains("matchedMatchSemantics"))
        assertTrue(findRoute.responseFields.contains("usedSurfaceFallback"))
        assertTrue(findRoute.responseFields.contains("usedContainsFallback"))
        assertTrue(findRoute.responseFields.contains("suggestedAlternateSurfaces"))
        assertTrue(findRoute.responseFields.contains("suggestedAlternateStrategies"))
        assertEquals("snapshot_ephemeral", findRoute.referenceStability)
        assertEquals("same_snapshot_only", findRoute.snapshotScope)
        assertEquals("selector_reresolution", findRoute.recommendedInteractionModel)
        assertTrue(findRoute.description.contains("exact strategies stay exact"))
        assertTrue(findRoute.description.contains("requested-vs-matched selector truth"))

        val homeRoute = GhosthandCommandCatalog.commands.first { it.id == "home" }
        assertEquals("prompt_completion", homeRoute.transportContract)

        val infoRoute = GhosthandCommandCatalog.commands.first { it.id == "info" }
        assertEquals("mixed_state_summary", infoRoute.stateTruth)
    }

    @Test
    fun nodeIdPolicyIsExplicitForSnapshotScopedRoutes() {
        val clickRoute = GhosthandCommandCatalog.commands.first { it.id == "click" }
        assertEquals("snapshot_ephemeral", clickRoute.referenceStability)
        assertEquals("same_snapshot_only", clickRoute.snapshotScope)
        assertEquals("selector_reresolution", clickRoute.recommendedInteractionModel)
        assertTrue(clickRoute.description.contains("bounded fallback chain"))

        val setTextRoute = GhosthandCommandCatalog.commands.first { it.id == "set_text" }
        assertEquals("snapshot_ephemeral", setTextRoute.referenceStability)
        assertEquals("same_snapshot_only", setTextRoute.snapshotScope)
        assertEquals("selector_reresolution", setTextRoute.recommendedInteractionModel)

        val treeRoute = GhosthandCommandCatalog.commands.first { it.id == "tree" }
        assertTrue(treeRoute.responseFields.contains("foregroundStableDuringCapture"))
        assertTrue(treeRoute.responseFields.contains("partialOutput"))
        assertTrue(treeRoute.responseFields.contains("returnedNodeCount"))
        assertTrue(treeRoute.responseFields.contains("warnings"))
        assertTrue(treeRoute.responseFields.contains("invalidBoundsCount"))
        assertTrue(treeRoute.responseFields.contains("lowSignalCount"))
        assertEquals("snapshot_ephemeral", treeRoute.referenceStability)
        assertEquals("same_snapshot_only", treeRoute.snapshotScope)
        assertEquals("selector_reresolution", treeRoute.recommendedInteractionModel)

        val scrollRoute = GhosthandCommandCatalog.commands.first { it.id == "scroll" }
        assertTrue(scrollRoute.responseFields.contains("contentChanged"))
        assertTrue(scrollRoute.responseFields.contains("surfaceChanged"))
        assertTrue(scrollRoute.responseFields.contains("beforeSnapshotToken"))
        assertTrue(scrollRoute.responseFields.contains("afterSnapshotToken"))
        assertTrue(scrollRoute.responseFields.contains("disclosure"))
    }
}
