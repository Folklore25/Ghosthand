/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.summary

import com.folklore25.ghosthand.ScreenReadPayload
import com.folklore25.ghosthand.screen.read.ScreenReadPayloadFields

object ScreenSummaryPayloadComposer {
    fun summaryFields(payload: ScreenReadPayload): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            putAll(ScreenReadPayloadFields.surfaceContextFields(payload))
            putAll(ScreenReadPayloadFields.surfaceObservationFields(payload))
            putAll(ScreenReadPayloadFields.surfaceFallbackFields(payload, includeRetryHint = false))
            putAll(ScreenReadPayloadFields.surfacePreviewFields(payload, includeImage = false))
            put("omittedSummary", payload.omittedSummary)
            put("focusedEditablePresent", payload.elements.any { it.editable && it.focused })
        }
    }
}
