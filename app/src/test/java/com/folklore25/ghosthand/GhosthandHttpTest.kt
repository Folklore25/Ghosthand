/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

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

    @Test
    fun readHttpLineConsumesCrLfAndReturnsNullAtEof() {
        val input = ByteArrayInputStream("POST /click HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray(StandardCharsets.UTF_8))

        assertEquals("POST /click HTTP/1.1", GhosthandHttp.readHttpLine(input))
        assertEquals("Host: 127.0.0.1", GhosthandHttp.readHttpLine(input))
        assertEquals("", GhosthandHttp.readHttpLine(input))
        assertNull(GhosthandHttp.readHttpLine(input))
    }

    @Test
    fun readUtf8BodyUsesByteContentLengthInsteadOfCharacterCount() {
        val body = "{\"text\":\"设置\"}"
        val input = ByteArrayInputStream(body.toByteArray(StandardCharsets.UTF_8))

        assertEquals(
            body,
            GhosthandHttp.readUtf8Body(input, body.toByteArray(StandardCharsets.UTF_8).size.toString())
        )
    }
}
