/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

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
