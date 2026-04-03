/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class GhosthandCommandDescriptor(
    val id: String,
    val category: String,
    val method: String,
    val path: String,
    val description: String,
    val params: List<GhosthandCommandParam> = emptyList(),
    val responseFields: List<String> = emptyList(),
    val selectorSupport: GhosthandSelectorSupport? = null,
    val focusRequirement: String = "none",
    val delayedAcceptance: String = "none",
    val transportContract: String = "default",
    val stateTruth: String = "none",
    val changeSignal: String = "none",
    val operatorUses: List<String> = emptyList(),
    val referenceStability: String = "not_applicable",
    val snapshotScope: String = "not_applicable",
    val recommendedInteractionModel: String = "none",
    val stability: String = "stable",
    val exampleRequest: Map<String, Any?>? = null,
    val exampleResponse: Map<String, Any?>? = null
)

data class GhosthandCommandParam(
    val name: String,
    val type: String,
    val location: String,
    val required: Boolean,
    val description: String,
    val allowedValues: List<String> = emptyList()
)

data class GhosthandSelectorSupport(
    val aliases: List<String>,
    val strategies: List<String>,
    val primaryStrategies: List<String> = emptyList(),
    val boundedAids: List<String> = emptyList()
)
