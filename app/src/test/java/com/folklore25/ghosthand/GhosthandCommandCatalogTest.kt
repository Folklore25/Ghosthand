/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandCommandCatalogTest {
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
        assertEquals("1.18", GhosthandCommandCatalog.schemaVersion)
        assertNotNull(commandsRoute.exampleResponse)
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
        assertTrue(clickRoute.responseFields.contains("backendUsed"))
        assertTrue(clickRoute.responseFields.contains("attemptedPath"))
        assertTrue(clickRoute.responseFields.contains("resolution"))
        assertTrue(clickRoute.responseFields.contains("disclosure"))
        assertTrue(clickRoute.description.contains("actionable clickable target by default"))
        assertTrue(clickRoute.description.contains("requested-vs-matched selector truth"))
        assertTrue(clickRoute.description.contains("contentDesc"))
        assertNotNull(clickRoute.exampleRequest)
        assertNotNull(clickRoute.exampleResponse)

        val inputRoute = GhosthandCommandCatalog.commands.first { it.id == "input" }
        assertEquals("focused_editable", inputRoute.focusRequirement)
        assertEquals("stable", inputRoute.stability)
        assertTrue(inputRoute.responseFields.contains("previousText"))
        assertTrue(inputRoute.responseFields.contains("action"))
        assertNotNull(inputRoute.exampleRequest)
        assertFalse(inputRoute.params.isEmpty())

        val swipeRoute = GhosthandCommandCatalog.commands.first { it.id == "swipe" }
        assertEquals("recommended", swipeRoute.delayedAcceptance)
        assertTrue(swipeRoute.responseFields.contains("performed"))
        assertTrue(swipeRoute.responseFields.contains("requestShape"))
        assertTrue(swipeRoute.responseFields.contains("contentChanged"))
        assertTrue(swipeRoute.responseFields.contains("beforeSnapshotToken"))
        assertTrue(swipeRoute.responseFields.contains("afterSnapshotToken"))
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
        assertTrue(waitConditionRoute.responseFields.contains("satisfied"))
        assertTrue(waitConditionRoute.responseFields.contains("conditionMet"))
        assertTrue(waitConditionRoute.responseFields.contains("stateChanged"))
        assertTrue(waitConditionRoute.responseFields.contains("timedOut"))
        assertTrue(waitConditionRoute.responseFields.contains("reason"))
        assertTrue(waitConditionRoute.responseFields.contains("disclosure"))

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
    }

    @Test
    fun paramsExposeMachineReadableLocationsAndCommandsExposeResponseFields() {
        val screenRoute = GhosthandCommandCatalog.commands.first { it.id == "screen" }
        assertTrue(screenRoute.params.all { it.location == "query" })
        assertTrue(screenRoute.params.any { it.name == "source" })
        assertTrue(screenRoute.responseFields.contains("elements"))
        assertTrue(screenRoute.responseFields.contains("source"))
        assertTrue(screenRoute.responseFields.contains("accessibilityElementCount"))
        assertTrue(screenRoute.responseFields.contains("ocrElementCount"))
        assertTrue(screenRoute.responseFields.contains("usedOcrFallback"))
        assertTrue(screenRoute.responseFields.contains("foregroundStableDuringCapture"))
        assertTrue(screenRoute.responseFields.contains("partialOutput"))
        assertTrue(screenRoute.responseFields.contains("candidateNodeCount"))
        assertTrue(screenRoute.responseFields.contains("returnedElementCount"))
        assertTrue(screenRoute.responseFields.contains("warnings"))
        assertTrue(screenRoute.responseFields.contains("omittedInvalidBoundsCount"))
        assertTrue(screenRoute.responseFields.contains("omittedLowSignalCount"))
        assertTrue(screenRoute.responseFields.contains("omittedNodeCount"))
        assertTrue(screenRoute.responseFields.contains("disclosure"))
        assertEquals("structured_actionable_surface_snapshot", screenRoute.stateTruth)
        assertTrue(screenRoute.operatorUses.contains("structured_actionable_surface_snapshot"))
        assertTrue(screenRoute.operatorUses.contains("selector_planning"))
        assertEquals("snapshot_ephemeral", screenRoute.referenceStability)
        assertEquals("same_snapshot_only", screenRoute.snapshotScope)
        assertEquals("selector_reresolution", screenRoute.recommendedInteractionModel)
        assertTrue(screenRoute.description.contains("source=accessibility"))
        assertTrue(screenRoute.description.contains("hybrid"))

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

        val launchRoute = GhosthandCommandCatalog.commands.first { it.id == "launch" }
        assertEquals("prompt_completion", launchRoute.transportContract)
        assertEquals("/launch", launchRoute.path)
        assertEquals(
            listOf("launched", "packageName", "label", "strategy", "reason"),
            launchRoute.responseFields
        )
        assertEquals("packageName", launchRoute.params.single().name)
        assertEquals("body", launchRoute.params.single().location)

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
