/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

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

import com.folklore25.ghosthand.server.LocalApiServerRequestException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
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

    @Test
    fun readUtf8BodyRejectsInvalidContentLength() {
        val error = expectBodyReadFailure {
            GhosthandHttp.readUtf8Body(ByteArrayInputStream(ByteArray(0)), "nope", maxBodyBytes = 1024)
        }

        assertEquals(400, error.statusCode)
        assertEquals("BAD_REQUEST", error.errorCode)
    }

    @Test
    fun readUtf8BodyRejectsTruncatedBodies() {
        val error = expectBodyReadFailure {
            GhosthandHttp.readUtf8Body(
                ByteArrayInputStream("abc".toByteArray(StandardCharsets.UTF_8)),
                "5",
                maxBodyBytes = 1024
            )
        }

        assertEquals(400, error.statusCode)
        assertEquals("BAD_REQUEST", error.errorCode)
    }

    private fun expectBodyReadFailure(block: () -> Unit): LocalApiServerRequestException {
        try {
            block()
        } catch (error: LocalApiServerRequestException) {
            return error
        }

        fail("Expected LocalApiServerRequestException")
        throw AssertionError("unreachable")
    }
}
