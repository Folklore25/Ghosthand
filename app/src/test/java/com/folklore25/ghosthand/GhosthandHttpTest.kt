package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Test

class GhosthandHttpTest {
    @Test
    fun parseRequestTargetSplitsPathAndQuery() {
        val target = GhosthandHttp.parseRequestTarget("/screen?clickable=true&package=com.example")
        assertEquals("/screen", target.path)
        assertEquals("true", target.queryParameters["clickable"])
        assertEquals("com.example", target.queryParameters["package"])
    }

    @Test
    fun parseQueryParametersDecodesValuesAndSkipsEmptyKeys() {
        val params = GhosthandHttp.parseQueryParameters("/wait?timeout=5000&package=com.android.settings&desc=%E6%90%9C%E7%B4%A2&=ignored")
        assertEquals("5000", params["timeout"])
        assertEquals("com.android.settings", params["package"])
        assertEquals("搜索", params["desc"])
        assertEquals(3, params.size)
    }

    @Test
    fun parseQueryParametersLastValueWinsForDuplicateKeys() {
        val params = GhosthandHttp.parseQueryParameters("/tree?mode=flat&mode=raw")
        assertEquals("raw", params["mode"])
    }

    @Test
    fun statusTextMapsKnownAndUnknownCodes() {
        assertEquals("OK", GhosthandHttp.statusText(200))
        assertEquals("Method Not Allowed", GhosthandHttp.statusText(405))
        assertEquals("Internal Server Error", GhosthandHttp.statusText(999))
    }
}
