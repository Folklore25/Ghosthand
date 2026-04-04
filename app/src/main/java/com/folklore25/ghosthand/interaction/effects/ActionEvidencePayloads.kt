/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.effects

import com.folklore25.ghosthand.interaction.execution.ActionEffectObservation

import com.folklore25.ghosthand.R

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
