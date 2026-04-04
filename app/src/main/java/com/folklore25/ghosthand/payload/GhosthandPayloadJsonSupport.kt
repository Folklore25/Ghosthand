/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.R

import org.json.JSONArray
import org.json.JSONObject

internal object GhosthandPayloadJsonSupport {
    fun fieldsToJson(fields: Map<String, Any?>): JSONObject {
        return JSONObject().apply {
            fields.forEach { (key, value) -> put(key, toJsonValue(value)) }
        }
    }

    fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (key, nestedValue) ->
                    put(key as String, toJsonValue(nestedValue))
                }
            }
            is List<*> -> JSONArray().apply {
                value.forEach { item -> put(toJsonValue(item)) }
            }
            else -> value
        }
    }
}
