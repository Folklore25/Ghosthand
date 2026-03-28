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
        assertEquals("1.2", GhosthandCommandCatalog.schemaVersion)
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
    }

    @Test
    fun interactiveCommandsExposeFocusSelectorAndAcceptanceHints() {
        val clickRoute = GhosthandCommandCatalog.commands.first { it.id == "click" }
        assertNotNull(clickRoute.selectorSupport)
        assertEquals(listOf("text", "desc", "id"), clickRoute.selectorSupport!!.aliases)
        assertEquals("none", clickRoute.focusRequirement)
        assertEquals("none", clickRoute.delayedAcceptance)
        assertTrue(clickRoute.responseFields.contains("performed"))
        assertTrue(clickRoute.responseFields.contains("backendUsed"))
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

        val waitConditionRoute = GhosthandCommandCatalog.commands.first { it.id == "wait_condition" }
        assertEquals("required", waitConditionRoute.delayedAcceptance)
        assertNotNull(waitConditionRoute.selectorSupport)
        assertTrue(waitConditionRoute.selectorSupport!!.strategies.contains("focused"))
        assertTrue(waitConditionRoute.responseFields.contains("satisfied"))
        assertTrue(waitConditionRoute.responseFields.contains("reason"))
    }

    @Test
    fun paramsExposeMachineReadableLocationsAndCommandsExposeResponseFields() {
        val screenRoute = GhosthandCommandCatalog.commands.first { it.id == "screen" }
        assertTrue(screenRoute.params.all { it.location == "query" })
        assertTrue(screenRoute.responseFields.contains("elements"))

        val clipboardWriteRoute = GhosthandCommandCatalog.commands.first { it.id == "clipboard_write" }
        assertEquals("body", clipboardWriteRoute.params.single().location)
        assertTrue(clipboardWriteRoute.responseFields.contains("written"))

        val commandsRoute = GhosthandCommandCatalog.commands.first { it.id == "commands" }
        assertTrue(commandsRoute.responseFields.contains("schemaVersion"))
        assertTrue(commandsRoute.responseFields.contains("commands"))
    }
}
