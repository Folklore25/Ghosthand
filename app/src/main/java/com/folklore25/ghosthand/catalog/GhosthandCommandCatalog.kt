/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.catalog

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

import com.folklore25.ghosthand.capability.GhosthandCapabilityPresentation

object GhosthandCommandCatalog {
    const val schemaVersion = "1.25"

    val selectorAliases: Map<String, String> = GhosthandSelectorCatalog.aliases

    val selectorStrategies: List<String> = GhosthandSelectorCatalog.strategies

    val commands: List<GhosthandCommandDescriptor> = buildList {
        addAll(GhosthandReadCommandCatalog.commands)
        addAll(GhosthandInteractionCommandCatalog.commands)
        addAll(GhosthandSensingCommandCatalog.commands)
        addAll(GhosthandIntrospectionCommandCatalog.commands)
    }

    fun commandPayloads(): List<Map<String, Any?>> = commands.map { command ->
        val capabilityMetadata = GhosthandCapabilityPlaneCatalog.metadataFor(command)
        linkedMapOf<String, Any?>(
            "id" to command.id,
            "category" to command.category,
            "method" to command.method,
            "path" to command.path,
            "description" to command.description,
            "capabilityIds" to command.capabilityIds.takeIf { it.isNotEmpty() },
            "capabilities" to GhosthandCapabilityPresentation.commandCapabilityFields(command.capabilityIds).takeIf { it.isNotEmpty() },
            "params" to command.params.map(::paramPayload),
            "responseFields" to command.responseFields,
            "plane" to capabilityMetadata.plane,
            "availabilityModel" to capabilityMetadata.availabilityModel,
            "truthType" to capabilityMetadata.truthType,
            "directness" to capabilityMetadata.directness
        ).apply {
            if (capabilityMetadata.preconditions.isNotEmpty()) put("preconditions", capabilityMetadata.preconditions)
            if (capabilityMetadata.failureModes.isNotEmpty()) put("failureModes", capabilityMetadata.failureModes)
            command.selectorSupport?.let { selectorSupport ->
                put(
                    "selectorSupport",
                    linkedMapOf(
                        "aliases" to selectorSupport.aliases,
                        "strategies" to selectorSupport.strategies,
                        "primaryStrategies" to selectorSupport.primaryStrategies.takeIf { it.isNotEmpty() },
                        "boundedAids" to selectorSupport.boundedAids.takeIf { it.isNotEmpty() }
                    ).filterValues { it != null }
                )
            }
            if (command.category == "interaction") {
                put("focusRequirement", command.focusRequirement)
                put("delayedAcceptance", command.delayedAcceptance)
            } else if (command.delayedAcceptance != "none") {
                put("delayedAcceptance", command.delayedAcceptance)
            }
            if (command.transportContract != "default") put("transportContract", command.transportContract)
            if (command.stateTruth != "none") put("stateTruth", command.stateTruth)
            if (command.changeSignal != "none") put("changeSignal", command.changeSignal)
            if (command.operatorUses.isNotEmpty()) put("operatorUses", command.operatorUses)
            if (command.referenceStability != "not_applicable") put("referenceStability", command.referenceStability)
            if (command.snapshotScope != "not_applicable") put("snapshotScope", command.snapshotScope)
            if (command.recommendedInteractionModel != "none") {
                put("recommendedInteractionModel", command.recommendedInteractionModel)
            }
            if (command.stability != "stable") put("stability", command.stability)
            command.exampleRequest?.let { put("exampleRequest", it) }
            command.exampleResponse?.let { put("exampleResponse", it) }
        }
    }

    private fun paramPayload(param: GhosthandCommandParam): Map<String, Any?> =
        linkedMapOf<String, Any?>(
            "name" to param.name,
            "type" to param.type,
            "location" to param.location,
            "required" to param.required,
            "description" to param.description
        ).apply {
            if (param.allowedValues.isNotEmpty()) put("allowedValues", param.allowedValues)
        }
}
