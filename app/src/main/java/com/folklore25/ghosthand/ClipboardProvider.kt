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

    /**
     * Reads the current primary clipboard text.
     * Returns null if clipboard is empty or contains non-text data.
     */
    fun readClipboard(): ClipboardReadResult {
        return try {
            val clip = clipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) {
                return ClipboardReadResult(
                    available = false,
                    text = null,
                    attemptedPath = "clipboard_empty"
                )
            }
            val text = clip.getItemAt(0).coerceToText(null)?.toString()
            ClipboardReadResult(
                available = true,
                text = text,
                attemptedPath = "clipboard_read"
            )
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
