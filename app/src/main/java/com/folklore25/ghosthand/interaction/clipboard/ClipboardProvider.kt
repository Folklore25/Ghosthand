/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.clipboard

import com.folklore25.ghosthand.R
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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Provides clipboard read/write via ClipboardManager in Ghosthand's own app process.
 * No special permission required — runs as the owning app.
 */
class ClipboardProvider(context: Context) {
    private val clipboardManager = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
        as ClipboardManager
    private val fallbackState = ClipboardReadFallbackState()

    /**
     * Reads the current primary clipboard text.
     * Returns null if clipboard is empty or contains non-text data.
     */
    fun readClipboard(): ClipboardReadResult {
        return try {
            val clip = clipboardManager.primaryClip
            val itemCount = clip?.itemCount ?: 0
            val text = if (itemCount > 0) {
                clip?.getItemAt(0)?.coerceToText(null)?.toString()
            } else {
                null
            }
            fallbackState.resolveRead(itemCount = itemCount, text = text)
        } catch (_: Exception) {
            ClipboardReadResult(
                available = false,
                text = null,
                attemptedPath = "clipboard_read_failed"
            )
        }
    }

    /**
     * Writes text to the primary clipboard.
     * Label is optional; a default label is used if null.
     */
    fun writeClipboard(text: String, label: String? = null): ClipboardWriteResult {
        return try {
            val clip = ClipData.newPlainText(label ?: CLIPBOARD_LABEL, text)
            clipboardManager.setPrimaryClip(clip)
            fallbackState.recordSuccessfulWrite(text)
            ClipboardWriteResult(
                performed = true,
                attemptedPath = "clipboard_write"
            )
        } catch (_: Exception) {
            ClipboardWriteResult(
                performed = false,
                attemptedPath = "clipboard_write_failed"
            )
        }
    }

    private companion object {
        const val CLIPBOARD_LABEL = "Ghosthand"
    }
}

data class ClipboardReadResult(
    val available: Boolean,
    val text: String?,
    val attemptedPath: String
)

data class ClipboardWriteResult(
    val performed: Boolean,
    val attemptedPath: String
)
