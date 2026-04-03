/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object GhosthandInputPayloads {
    fun parseRequest(body: JSONObject): GhosthandInputRequestParseResult {
        return parseRequest(
            linkedMapOf<String, Any?>().apply {
                body.keys().forEach { key ->
                    put(key, if (body.isNull(key)) null else body.opt(key))
                }
            }
        )
    }

    fun parseRequest(body: String): GhosthandInputRequestParseResult {
        return try {
            parseRequest(JSONObject(body))
        } catch (_: JSONException) {
            GhosthandInputRequestParseResult(errorMessage = "Request body must be valid JSON.")
        }
    }

    fun parseRequest(body: Map<String, Any?>): GhosthandInputRequestParseResult {
        val text = when {
            body.containsKey("text") && body["text"] != null -> body["text"] as? String
                ?: return GhosthandInputRequestParseResult(errorMessage = "text must be a string.")
            else -> null
        }
        val explicitTextAction = when {
            body.containsKey("textAction") && body["textAction"] != null -> {
                val raw = body["textAction"] as? String
                    ?: return GhosthandInputRequestParseResult(errorMessage = "textAction must be a string.")
                InputTextAction.fromWireValue(raw)
                    ?: return GhosthandInputRequestParseResult(errorMessage = "textAction must be one of: set, append, clear.")
            }
            else -> null
        }
        val key = when {
            body.containsKey("key") && body["key"] != null -> {
                val raw = body["key"] as? String
                    ?: return GhosthandInputRequestParseResult(errorMessage = "key must be a string.")
                InputKey.fromWireValue(raw)
                    ?: return GhosthandInputRequestParseResult(errorMessage = "key must be one of: enter.")
            }
            else -> null
        }

        val append = body["append"] as? Boolean ?: false
        val clear = body["clear"] as? Boolean ?: false
        if (explicitTextAction == null && append && clear) {
            return GhosthandInputRequestParseResult(errorMessage = "append and clear cannot both be true.")
        }

        val textAction = explicitTextAction ?: when {
            append -> InputTextAction.APPEND
            text != null -> InputTextAction.SET
            clear -> InputTextAction.CLEAR
            else -> null
        }

        if (textAction == null && key == null) {
            return GhosthandInputRequestParseResult(errorMessage = "At least one explicit /input operation is required.")
        }
        if (textAction == InputTextAction.CLEAR && text != null) {
            return GhosthandInputRequestParseResult(errorMessage = "text must be omitted when textAction=clear.")
        }
        if (textAction != null && textAction != InputTextAction.CLEAR && text == null) {
            return GhosthandInputRequestParseResult(
                errorMessage = "text is required when textAction=${textAction.wireValue}."
            )
        }

        return GhosthandInputRequestParseResult(
            request = GhosthandInputRequest(
                textAction = textAction,
                text = text,
                key = key
            )
        )
    }
}

internal object GhosthandInteractionPayloads {
    fun clickFields(result: ClickAttemptResult): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "backendUsed" to result.backendUsed,
            "attemptedPath" to result.attemptedPath
        )
        result.effect?.let { effect -> payload.putAll(actionEffectFields(effect)) }
        result.selectorResolution?.let { resolution ->
            payload["resolution"] = clickResolutionFields(resolution)
        }
        return payload
    }

    fun globalActionFields(result: GlobalActionResult): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "attemptedPath" to result.attemptedPath
        ).apply {
            result.effect?.let { effect -> putAll(actionEffectFields(effect)) }
        }
    }

    fun clickResolutionFields(resolution: ClickSelectorResolution): Map<String, Any?> {
        return linkedMapOf(
            "requestedStrategy" to resolution.requestedStrategy,
            "effectiveStrategy" to resolution.effectiveStrategy,
            "requestedSurface" to resolution.requestedSurface,
            "matchedSurface" to resolution.matchedSurface,
            "requestedMatchSemantics" to resolution.requestedMatchSemantics,
            "matchedMatchSemantics" to resolution.matchedMatchSemantics,
            "usedSurfaceFallback" to resolution.usedSurfaceFallback,
            "usedContainsFallback" to resolution.usedContainsFallback,
            "matchedNodeId" to resolution.matchedNodeId,
            "matchedNodeClickable" to resolution.matchedNodeClickable,
            "resolvedNodeId" to resolution.resolvedNodeId,
            "resolutionKind" to resolution.resolutionKind,
            "ancestorDepth" to resolution.ancestorDepth
        )
    }

    fun actionEffectFields(effect: ActionEffectObservation): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "stateChanged" to effect.stateChanged,
            "beforeSnapshotToken" to effect.beforeSnapshotToken,
            "afterSnapshotToken" to effect.afterSnapshotToken,
            "finalPackageName" to effect.finalPackageName,
            "finalActivity" to effect.finalActivity
        ).apply {
            postActionStateFields(
                PostActionState(
                    packageName = effect.finalPackageName,
                    activity = effect.finalActivity,
                    snapshotToken = effect.afterSnapshotToken
                )
            ).takeIf { it.isNotEmpty() }?.let { put("postActionState", it) }
        }
    }

    fun postActionStateFields(state: PostActionState): Map<String, Any?> {
        return linkedMapOf<String, Any?>().apply {
            state.packageName?.let { put("packageName", it) }
            state.activity?.let { put("activity", it) }
            state.snapshotToken?.let { put("snapshotToken", it) }
            state.focusedEditablePresent?.let { put("focusedEditablePresent", it) }
            state.renderMode?.let { put("renderMode", it) }
            state.surfaceReadability?.let { put("surfaceReadability", it) }
            state.visualAvailable?.let { put("visualAvailable", it) }
        }
    }

    fun clickFailureFields(hint: FindMissHint): Map<String, Any?> {
        return linkedMapOf(
            "failureCategory" to hint.failureCategory,
            "selectorMatchCount" to hint.selectorMatchCount,
            "actionableMatchCount" to hint.actionableMatchCount,
            "searchedSurface" to hint.searchedSurface,
            "matchSemantics" to hint.matchSemantics,
            "matchedSurface" to hint.matchedSurface,
            "matchedMatchSemantics" to hint.matchedMatchSemantics
        )
    }

    fun disclosureFields(disclosure: GhosthandDisclosure): Map<String, Any?> {
        return linkedMapOf(
            "kind" to disclosure.kind,
            "summary" to disclosure.summary,
            "assumptionToCorrect" to disclosure.assumptionToCorrect,
            "nextBestActions" to disclosure.nextBestActions
        )
    }

    fun inputResultFields(result: InputOperationResult): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "performed" to result.performed,
            "textChanged" to (result.textMutation?.performed ?: false),
            "keyDispatched" to (result.keyDispatch?.performed ?: false),
            "textMutation" to result.textMutation?.let { mutation ->
                linkedMapOf(
                    "requested" to mutation.requested,
                    "performed" to mutation.performed,
                    "action" to mutation.action,
                    "previousText" to mutation.previousText,
                    "text" to mutation.finalText,
                    "backendUsed" to mutation.backendUsed,
                    "failureReason" to mutation.failureReason?.name,
                    "attemptedPath" to mutation.attemptedPath
                )
            },
            "keyDispatch" to result.keyDispatch?.let { dispatch ->
                linkedMapOf(
                    "requested" to dispatch.requested,
                    "performed" to dispatch.performed,
                    "key" to dispatch.key,
                    "backendUsed" to dispatch.backendUsed,
                    "failureReason" to dispatch.failureReason?.name,
                    "attemptedPath" to dispatch.attemptedPath
                )
            }
        ).apply {
            result.postActionState
                ?.let(::postActionStateFields)
                ?.takeIf { it.isNotEmpty() }
                ?.let { put("postActionState", it) }
        }
    }
}

internal object GhosthandScreenPayloads {
    fun treeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        val invalidBoundsCount = snapshot.nodes.count { !it.bounds.isValidGeometry() }
        val lowSignalCount = snapshot.nodes.count { it.isLowSignalNode() }
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "foregroundStableDuringCapture" to snapshot.foregroundStableDuringCapture,
            "partialOutput" to false,
            "returnedNodeCount" to snapshot.nodes.size,
            "warnings" to combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(invalidBoundsCount, "tree"),
                readabilityWarnings = warningsForLowSignal(lowSignalCount, "tree")
            ),
            "invalidBoundsCount" to invalidBoundsCount,
            "lowSignalCount" to lowSignalCount,
            "nodes" to snapshot.nodes.map(::nodeFields)
        )
    }

    fun rawTreeFields(snapshot: AccessibilityTreeSnapshot): Map<String, Any?> {
        val invalidBoundsCount = snapshot.nodes.count { !it.bounds.isValidGeometry() }
        val lowSignalCount = snapshot.nodes.count { it.isLowSignalNode() }
        return linkedMapOf(
            "packageName" to snapshot.packageName,
            "activity" to snapshot.activity,
            "snapshotToken" to snapshot.snapshotToken,
            "capturedAt" to snapshot.capturedAt,
            "foregroundStableDuringCapture" to snapshot.foregroundStableDuringCapture,
            "partialOutput" to false,
            "returnedNodeCount" to snapshot.nodes.size,
            "warnings" to combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(invalidBoundsCount, "tree"),
                readabilityWarnings = warningsForLowSignal(lowSignalCount, "tree")
            ),
            "invalidBoundsCount" to invalidBoundsCount,
            "lowSignalCount" to lowSignalCount,
            "root" to buildRawTreeFields(snapshot, listOf(0))
        )
    }

    fun screenFields(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): Map<String, Any?> {
        return screenReadFields(
            accessibilityScreenRead(
                snapshot = snapshot,
                editableOnly = editableOnly,
                scrollableOnly = scrollableOnly,
                packageFilter = packageFilter,
                clickableOnly = clickableOnly
            )
        )
    }

    fun accessibilityScreenRead(
        snapshot: AccessibilityTreeSnapshot,
        editableOnly: Boolean,
        scrollableOnly: Boolean,
        packageFilter: String?,
        clickableOnly: Boolean
    ): ScreenReadPayload {
        val filteredNodes = snapshot.nodes
            .asSequence()
            .filter { !editableOnly || it.editable }
            .filter { !scrollableOnly || it.scrollable }
            .filter { !clickableOnly || it.clickable }
            .filter { packageFilter.isNullOrBlank() || snapshot.packageName == packageFilter }
            .toList()
        val actionableNodes = filteredNodes.filter { it.hasActionableBounds() }
        val omittedInvalidBoundsCount = filteredNodes.size - actionableNodes.size
        val readableNodes = actionableNodes.filterNot { it.isLowSignalNode() }
        val omittedLowSignalCount = actionableNodes.size - readableNodes.size
        val omittedNodeCount = omittedInvalidBoundsCount + omittedLowSignalCount
        val partialOutput = omittedNodeCount > 0
        val elements = readableNodes.map { node ->
            ScreenReadElement(
                nodeId = node.nodeId,
                text = node.text ?: "",
                desc = node.contentDesc ?: "",
                id = node.resourceId ?: "",
                clickable = node.clickable,
                editable = node.editable,
                scrollable = node.scrollable,
                bounds = "[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]",
                centerX = node.centerX,
                centerY = node.centerY,
                source = ScreenReadMode.ACCESSIBILITY.wireValue
            )
        }
        val payload = ScreenReadPayload(
            packageName = snapshot.packageName,
            activity = snapshot.activity,
            snapshotToken = snapshot.snapshotToken,
            capturedAt = snapshot.capturedAt,
            foregroundStableDuringCapture = snapshot.foregroundStableDuringCapture,
            partialOutput = partialOutput,
            candidateNodeCount = filteredNodes.size,
            returnedElementCount = elements.size,
            warnings = combinedWarnings(
                freshnessWarnings = snapshot.freshnessWarnings,
                geometryWarnings = warningsForInvalidBounds(omittedInvalidBoundsCount, "screen"),
                readabilityWarnings = warningsForLowSignal(omittedLowSignalCount, "screen"),
                partialWarnings = warningsForPartialOutput(partialOutput)
            ),
            omittedInvalidBoundsCount = omittedInvalidBoundsCount,
            omittedLowSignalCount = omittedLowSignalCount,
            omittedNodeCount = omittedNodeCount,
            omittedCategories = buildOmittedCategories(omittedInvalidBoundsCount, omittedLowSignalCount),
            omittedSummary = buildOmittedSummary(omittedInvalidBoundsCount, omittedLowSignalCount),
            invalidBoundsPresent = omittedInvalidBoundsCount > 0,
            lowSignalPresent = omittedLowSignalCount > 0,
            elements = elements,
            source = ScreenReadMode.ACCESSIBILITY.wireValue,
            accessibilityElementCount = elements.size,
            ocrElementCount = 0,
            usedOcrFallback = false
        )
        return payload.copy(retryHint = accessibilityRetryHint(payload))
    }

    fun screenReadFields(payload: ScreenReadPayload): Map<String, Any?> {
        return linkedMapOf(
            "packageName" to payload.packageName,
            "activity" to payload.activity,
            "snapshotToken" to payload.snapshotToken,
            "capturedAt" to payload.capturedAt,
            "foregroundStableDuringCapture" to payload.foregroundStableDuringCapture,
            "partialOutput" to payload.partialOutput,
            "candidateNodeCount" to payload.candidateNodeCount,
            "returnedElementCount" to payload.returnedElementCount,
            "warnings" to payload.warnings,
            "omittedInvalidBoundsCount" to payload.omittedInvalidBoundsCount,
            "omittedLowSignalCount" to payload.omittedLowSignalCount,
            "omittedNodeCount" to payload.omittedNodeCount,
            "omittedCategories" to payload.omittedCategories,
            "omittedSummary" to payload.omittedSummary,
            "invalidBoundsPresent" to payload.invalidBoundsPresent,
            "lowSignalPresent" to payload.lowSignalPresent,
            "source" to payload.source,
            "renderMode" to payload.renderMode(),
            "surfaceReadability" to payload.surfaceReadability(),
            "visualAvailable" to payload.visualAvailable,
            "previewAvailable" to payload.previewAvailable,
            "previewToken" to payload.previewToken,
            "previewWidth" to payload.previewWidth,
            "previewHeight" to payload.previewHeight,
            "accessibilityElementCount" to payload.accessibilityElementCount,
            "ocrElementCount" to payload.ocrElementCount,
            "usedOcrFallback" to payload.usedOcrFallback,
            "suggestedFallback" to payload.retryHint?.source,
            "suggestedSource" to payload.retryHint?.source,
            "fallbackReason" to payload.retryHint?.reason,
            "retryHint" to payload.retryHint?.let { hint ->
                linkedMapOf("source" to hint.source, "reason" to hint.reason)
            },
            "previewImage" to payload.previewImage,
            "elements" to payload.elements.map { element ->
                linkedMapOf(
                    "nodeId" to element.nodeId,
                    "text" to element.text,
                    "desc" to element.desc,
                    "id" to element.id,
                    "clickable" to element.clickable,
                    "editable" to element.editable,
                    "scrollable" to element.scrollable,
                    "bounds" to element.bounds,
                    "centerX" to element.centerX,
                    "centerY" to element.centerY,
                    "source" to element.source
                )
            }
        )
    }

    fun summaryFields(payload: ScreenReadPayload): Map<String, Any?> {
        return linkedMapOf(
            "packageName" to payload.packageName,
            "activity" to payload.activity,
            "snapshotToken" to payload.snapshotToken,
            "capturedAt" to payload.capturedAt,
            "foregroundStableDuringCapture" to payload.foregroundStableDuringCapture,
            "partialOutput" to payload.partialOutput,
            "candidateNodeCount" to payload.candidateNodeCount,
            "returnedElementCount" to payload.returnedElementCount,
            "warnings" to payload.warnings,
            "omittedSummary" to payload.omittedSummary,
            "source" to payload.source,
            "renderMode" to payload.renderMode(),
            "surfaceReadability" to payload.surfaceReadability(),
            "visualAvailable" to payload.visualAvailable,
            "previewAvailable" to payload.previewAvailable,
            "previewToken" to payload.previewToken,
            "previewWidth" to payload.previewWidth,
            "previewHeight" to payload.previewHeight,
            "accessibilityElementCount" to payload.accessibilityElementCount,
            "ocrElementCount" to payload.ocrElementCount,
            "usedOcrFallback" to payload.usedOcrFallback,
            "focusedEditablePresent" to payload.elements.any { it.editable },
            "suggestedFallback" to payload.retryHint?.source,
            "suggestedSource" to payload.retryHint?.source,
            "fallbackReason" to payload.retryHint?.reason
        )
    }

    fun nodeFields(node: FlatAccessibilityNode): Map<String, Any?> {
        val boundsValid = node.bounds.isValidGeometry()
        val actionableBounds = node.hasActionableBounds()
        val lowSignal = node.isLowSignalNode()
        return linkedMapOf(
            "nodeId" to node.nodeId,
            "text" to node.text,
            "contentDesc" to node.contentDesc,
            "resourceId" to node.resourceId,
            "className" to node.className,
            "clickable" to node.clickable,
            "editable" to node.editable,
            "enabled" to node.enabled,
            "scrollable" to node.scrollable,
            "boundsValid" to boundsValid,
            "actionableBounds" to actionableBounds,
            "lowSignal" to lowSignal,
            "centerX" to node.centerX,
            "centerY" to node.centerY,
            "bounds" to linkedMapOf(
                "left" to node.bounds.left,
                "top" to node.bounds.top,
                "right" to node.bounds.right,
                "bottom" to node.bounds.bottom
            )
        )
    }

    fun findFields(result: FindNodeResult): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "found" to result.found,
            "matchCount" to result.matches.size,
            "index" to result.selectedIndex
        )
        result.missHint?.let { hint ->
            payload["searchedSurface"] = hint.searchedSurface
            payload["matchSemantics"] = hint.matchSemantics
            payload["requestedSurface"] = hint.requestedSurface
            payload["requestedMatchSemantics"] = hint.requestedMatchSemantics
            payload["matchedSurface"] = hint.matchedSurface
            payload["matchedMatchSemantics"] = hint.matchedMatchSemantics
            payload["usedSurfaceFallback"] = hint.usedSurfaceFallback
            payload["usedContainsFallback"] = hint.usedContainsFallback
            if (hint.suggestedAlternateSurfaces.isNotEmpty()) {
                payload["suggestedAlternateSurfaces"] = hint.suggestedAlternateSurfaces
            }
            if (hint.suggestedAlternateStrategies.isNotEmpty()) {
                payload["suggestedAlternateStrategies"] = hint.suggestedAlternateStrategies
            }
        }
        val node = result.node ?: run {
            payload["node"] = null
            return payload
        }
        payload["node"] = nodeFields(node)
        payload["text"] = node.text ?: ""
        payload["desc"] = node.contentDesc ?: ""
        payload["id"] = node.resourceId ?: ""
        payload["bounds"] = "[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]"
        payload["centerX"] = node.centerX
        payload["centerY"] = node.centerY
        payload["clickable"] = node.clickable
        payload["editable"] = node.editable
        payload["scrollable"] = node.scrollable
        return payload
    }

    private fun accessibilityRetryHint(payload: ScreenReadPayload): ScreenReadRetryHint? {
        return when {
            payload.returnedElementCount == 0 -> ScreenReadRetryHint(
                source = ScreenReadMode.OCR.wireValue,
                reason = "accessibility_empty"
            )
            payload.accessibilityTreeIsOperationallyInsufficient() -> ScreenReadRetryHint(
                source = ScreenReadMode.HYBRID.wireValue,
                reason = "accessibility_operationally_insufficient"
            )
            else -> null
        }
    }

    private fun buildOmittedCategories(
        omittedInvalidBoundsCount: Int,
        omittedLowSignalCount: Int
    ): List<String> = buildList {
        if (omittedInvalidBoundsCount > 0) add("invalid_bounds")
        if (omittedLowSignalCount > 0) add("low_signal")
    }

    private fun buildOmittedSummary(
        omittedInvalidBoundsCount: Int,
        omittedLowSignalCount: Int
    ): String? {
        val parts = buildList {
            if (omittedInvalidBoundsCount > 0) add("$omittedInvalidBoundsCount invalid-bounds")
            if (omittedLowSignalCount > 0) add("$omittedLowSignalCount low-signal")
        }
        return if (parts.isEmpty()) null else "Omitted ${parts.joinToString(" and ")} nodes."
    }

    private fun buildRawTreeFields(
        snapshot: AccessibilityTreeSnapshot,
        path: List<Int>
    ): Map<String, Any?>? {
        val targetNode = snapshot.nodes.firstOrNull { node ->
            AccessibilityNodeLocator.pathSegments(node.nodeId) == path
        } ?: return null

        val children = snapshot.nodes
            .asSequence()
            .filter { candidate ->
                val candidatePath = AccessibilityNodeLocator.pathSegments(candidate.nodeId)
                candidatePath.size == path.size + 1 && candidatePath.dropLast(1) == path
            }
            .sortedBy { AccessibilityNodeLocator.pathSegments(it.nodeId).lastOrNull() ?: 0 }
            .mapNotNull { child -> buildRawTreeFields(snapshot, AccessibilityNodeLocator.pathSegments(child.nodeId)) }
            .toList()

        return nodeFields(targetNode) + ("children" to children)
    }

    private fun warningsForInvalidBounds(count: Int, route: String): List<String> {
        if (count <= 0) return emptyList()
        return when (route) {
            "screen" -> listOf("omitted_nodes_with_invalid_bounds")
            else -> listOf("invalid_bounds_present")
        }
    }

    private fun combinedWarnings(
        freshnessWarnings: List<String>,
        geometryWarnings: List<String>,
        readabilityWarnings: List<String>,
        partialWarnings: List<String> = emptyList()
    ): List<String> {
        return (freshnessWarnings + geometryWarnings + readabilityWarnings + partialWarnings).distinct()
    }

    private fun warningsForLowSignal(count: Int, route: String): List<String> {
        if (count <= 0) return emptyList()
        return when (route) {
            "screen" -> listOf("omitted_low_signal_nodes")
            else -> listOf("low_signal_nodes_present")
        }
    }

    private fun warningsForPartialOutput(partialOutput: Boolean): List<String> {
        return if (partialOutput) listOf("partial_output") else emptyList()
    }
}

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

private fun NodeBounds.isValidGeometry(): Boolean {
    return right > left && bottom > top
}

private fun FlatAccessibilityNode.hasActionableBounds(): Boolean {
    if (!bounds.isValidGeometry()) return false
    if (bounds.left < 0 || bounds.top < 0 || bounds.right < 0 || bounds.bottom < 0) return false
    return centerX in bounds.left..bounds.right && centerY in bounds.top..bounds.bottom
}

private fun FlatAccessibilityNode.isLowSignalNode(): Boolean {
    val hasMeaningfulLabel = !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || !resourceId.isNullOrBlank()
    if (hasMeaningfulLabel) return false
    if (clickable || editable || scrollable || focused) return false
    return true
}
