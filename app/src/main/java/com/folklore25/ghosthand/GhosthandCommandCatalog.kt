/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

object GhosthandCommandCatalog {
    const val schemaVersion = "1.24"

    val selectorAliases: Map<String, String> = GhosthandSelectorCatalog.aliases

    val selectorStrategies: List<String> = GhosthandSelectorCatalog.strategies

    val commands: List<GhosthandCommandDescriptor> = buildList {
        addAll(GhosthandReadCommandCatalog.commands)
        addAll(GhosthandInteractionCommandCatalog.commands)
        addAll(GhosthandSensingCommandCatalog.commands)
        addAll(GhosthandIntrospectionCommandCatalog.commands)
    }
}
