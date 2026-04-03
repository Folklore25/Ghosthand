/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.screen.summary

import com.folklore25.ghosthand.screen.read.ScreenReadPayloadFields
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.screen.read.ScreenStateLegibilityProjector

object ScreenSummaryPayloadComposer {
    fun summaryFields(payload: ScreenReadPayload): Map<String, Any?> {
        val legibility = ScreenStateLegibilityProjector.fromPayload(payload)
        return linkedMapOf<String, Any?>().apply {
            putAll(ScreenReadPayloadFields.surfaceContextFields(payload))
            putAll(ScreenReadPayloadFields.surfaceObservationFields(payload, legibility))
            putAll(ScreenReadPayloadFields.surfaceFallbackFields(payload, includeRetryHint = false))
            putAll(ScreenReadPayloadFields.surfacePreviewFields(payload, legibility, includeImage = false))
            put("omittedSummary", payload.omittedSummary)
        }
    }
}
