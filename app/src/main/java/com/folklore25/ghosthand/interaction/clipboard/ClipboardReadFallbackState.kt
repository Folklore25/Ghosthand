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

/**
 * Tracks the narrow fallback window after a successful in-process clipboard write.
 *
 * On some devices, Ghosthand can write clipboard text successfully and then observe an empty
 * clipboard immediately afterward once the app is backgrounded. In that specific case, allow
 * exactly one fallback read using the last successful write, then clear it so stale values are
 * not reused indefinitely.
 */
class ClipboardReadFallbackState {
    private var pendingWriteText: String? = null

    fun recordSuccessfulWrite(text: String) {
        pendingWriteText = text
    }

    fun resolveRead(itemCount: Int, text: String?): ClipboardReadResult {
        if (itemCount > 0) {
            pendingWriteText = null
            return ClipboardReadResult(
                available = true,
                text = text,
                attemptedPath = "clipboard_read"
            )
        }

        val fallback = pendingWriteText
        return if (fallback != null) {
            pendingWriteText = null
            ClipboardReadResult(
                available = true,
                text = fallback,
                attemptedPath = "clipboard_cached_after_write"
            )
        } else {
            ClipboardReadResult(
                available = false,
                text = null,
                attemptedPath = "clipboard_empty"
            )
        }
    }
}
