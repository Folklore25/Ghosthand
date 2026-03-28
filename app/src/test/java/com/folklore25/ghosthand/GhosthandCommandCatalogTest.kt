package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
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
    }

    @Test
    fun waitAndClickRoutesExposeStructuredParams() {
        val waitCondition = GhosthandCommandCatalog.commands.first { it.id == "wait_condition" }
        val selectorParam = waitCondition.params.first { it.name == "condition" }
        assertEquals("selector", selectorParam.type)
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
}
