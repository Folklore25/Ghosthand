/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.effects

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

import com.folklore25.ghosthand.payload.PostActionState
import com.folklore25.ghosthand.state.summary.PostActionStateComposer

object ActionEvidencePayloads {
    fun commonFields(
        performed: Boolean,
        attemptedPath: String? = null,
        backendUsed: String? = null,
        requestShape: String? = null,
        actionEffect: ActionEffectObservation? = null,
        postActionState: PostActionState? = null,
        extras: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "performed" to performed,
            "attemptedPath" to attemptedPath,
            "backendUsed" to backendUsed,
            "requestShape" to requestShape
        ).apply {
            actionEffect?.let { putAll(ActionEffectPayloads.fields(it)) }
            postActionState
                ?.let(PostActionStateComposer::fields)
                ?.takeIf { it.isNotEmpty() }
                ?.let { put("postActionState", it) }
            postActionState?.suggestedSource?.let { put("suggestedSource", it) }
            postActionState?.fallbackReason?.let { put("fallbackReason", it) }
            extras.forEach { (key, value) ->
                if (value != null) put(key, value)
            }
        }.filterValues { it != null }
    }
}
