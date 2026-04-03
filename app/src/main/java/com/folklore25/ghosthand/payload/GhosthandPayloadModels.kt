/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

data class GhosthandInputRequest(
    val textAction: InputTextAction? = null,
    val text: String? = null,
    val key: InputKey? = null
)

data class GhosthandInputRequestParseResult(
    val request: GhosthandInputRequest? = null,
    val errorMessage: String? = null
)

enum class InputTextAction(val wireValue: String) {
    SET("set"),
    APPEND("append"),
    CLEAR("clear");

    companion object {
        fun fromWireValue(value: String?): InputTextAction? {
            return entries.firstOrNull { it.wireValue == value }
        }
    }
}

enum class InputKey(val wireValue: String) {
    ENTER("enter");

    companion object {
        fun fromWireValue(value: String?): InputKey? {
            return entries.firstOrNull { it.wireValue == value }
        }
    }
}

data class GhosthandDisclosure(
    val kind: String,
    val summary: String,
    val assumptionToCorrect: String? = null,
    val nextBestActions: List<String> = emptyList()
)

data class PostActionState(
    val packageName: String? = null,
    val activity: String? = null,
    val snapshotToken: String? = null,
    val focusedEditablePresent: Boolean? = null,
    val renderMode: String? = null,
    val surfaceReadability: String? = null,
    val visualAvailable: Boolean? = null
)
