/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class SelectorQuery(
    val strategy: String,
    val query: String?
)

object GhosthandSelectors {
    fun normalize(
        text: String?,
        desc: String?,
        id: String?,
        strategy: String?,
        query: String?
    ): SelectorQuery? {
        text?.trim()?.ifEmpty { null }?.let {
            return SelectorQuery("text", it)
        }
        desc?.trim()?.ifEmpty { null }?.let {
            return SelectorQuery("contentDesc", it)
        }
        id?.trim()?.ifEmpty { null }?.let {
            return SelectorQuery("resourceId", it)
        }

        val normalizedStrategy = strategy?.trim()?.ifEmpty { null } ?: return null
        val normalizedQuery = query?.takeIf { it.isNotBlank() }
        return SelectorQuery(normalizedStrategy, normalizedQuery)
    }
}
