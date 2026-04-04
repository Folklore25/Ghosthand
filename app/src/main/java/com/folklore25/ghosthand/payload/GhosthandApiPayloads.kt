/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

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

import com.folklore25.ghosthand.*
import com.folklore25.ghosthand.screen.read.ScreenReadPayload
import com.folklore25.ghosthand.state.InputOperationResult

import org.json.JSONObject

object GhosthandApiPayloads {
    fun treePayload(snapshot: AccessibilityTreeSnapshot): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.treeFields(snapshot))

    fun rawTreePayload(snapshot: AccessibilityTreeSnapshot): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.rawTreeFields(snapshot))

    fun screenPayload(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): JSONObject = screenReadPayload(
        GhosthandScreenPayloads.accessibilityScreenRead(
            snapshot = snapshot,
            editableOnly = editableOnly,
            scrollableOnly = scrollableOnly,
            packageFilter = packageFilter,
            clickableOnly = clickableOnly
        )
    )

    fun screenReadPayload(payload: ScreenReadPayload): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.screenReadFields(payload))

    fun screenSummaryPayload(payload: ScreenReadPayload): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.summaryFields(payload))

    fun nodePayload(node: FlatAccessibilityNode): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.nodeFields(node))

    fun findPayload(result: FindNodeResult): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandScreenPayloads.findFields(result))

    fun clickPayload(result: ClickAttemptResult): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandInteractionPayloads.clickFields(result))

    fun clickFields(result: ClickAttemptResult): Map<String, Any?> =
        GhosthandInteractionPayloads.clickFields(result)

    fun globalActionFields(result: GlobalActionResult): Map<String, Any?> =
        GhosthandInteractionPayloads.globalActionFields(result)

    fun treeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> =
        GhosthandScreenPayloads.treeFields(snapshot)

    fun rawTreeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> =
        GhosthandScreenPayloads.rawTreeFields(snapshot)

    fun screenFields(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): Map<String, Any?> = GhosthandScreenPayloads.screenFields(
        snapshot = snapshot,
        editableOnly = editableOnly,
        scrollableOnly = scrollableOnly,
        packageFilter = packageFilter,
        clickableOnly = clickableOnly
    )

    fun accessibilityScreenRead(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload = GhosthandScreenPayloads.accessibilityScreenRead(
        snapshot = snapshot,
        editableOnly = editableOnly,
        scrollableOnly = scrollableOnly,
        packageFilter = packageFilter,
        clickableOnly = clickableOnly
    )

    fun screenReadFields(payload: ScreenReadPayload): Map<String, Any?> =
        GhosthandScreenPayloads.screenReadFields(payload)

    fun screenSummaryFields(payload: ScreenReadPayload): Map<String, Any?> =
        GhosthandScreenPayloads.summaryFields(payload)

    fun nodeFields(node: FlatAccessibilityNode): Map<String, Any?> =
        GhosthandScreenPayloads.nodeFields(node)

    fun findFields(result: FindNodeResult): Map<String, Any?> =
        GhosthandScreenPayloads.findFields(result)

    fun clickResolutionFields(resolution: ClickSelectorResolution): Map<String, Any?> =
        GhosthandInteractionPayloads.clickResolutionFields(resolution)

    fun actionEffectFields(effect: ActionEffectObservation): Map<String, Any?> =
        GhosthandInteractionPayloads.actionEffectFields(effect)

    fun postActionStateFields(state: PostActionState): Map<String, Any?> =
        GhosthandInteractionPayloads.postActionStateFields(state)

    fun clickFailureFields(hint: FindMissHint): Map<String, Any?> =
        GhosthandDisclosurePayloads.clickFailureFields(hint)

    fun disclosureFields(disclosure: GhosthandDisclosure): Map<String, Any?> =
        GhosthandDisclosurePayloads.disclosureFields(disclosure)

    fun disclosureJson(disclosure: GhosthandDisclosure): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandDisclosurePayloads.disclosureFields(disclosure))

    fun parseInputRequest(body: JSONObject): GhosthandInputRequestParseResult =
        GhosthandInputPayloads.parseRequest(body)

    fun parseInputRequest(body: Map<String, Any?>): GhosthandInputRequestParseResult =
        GhosthandInputPayloads.parseRequest(body)

    fun parseInputRequest(body: String): GhosthandInputRequestParseResult =
        GhosthandInputPayloads.parseRequest(body)

    fun inputResultFields(result: InputOperationResult): Map<String, Any?> =
        GhosthandInputPayloads.inputResultFields(result)

    fun inputResultJson(result: InputOperationResult): JSONObject =
        GhosthandPayloadJsonSupport.fieldsToJson(GhosthandInputPayloads.inputResultFields(result))
}
