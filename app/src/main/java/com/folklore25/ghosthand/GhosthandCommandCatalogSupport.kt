/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

internal object GhosthandSelectorCatalog {
    val aliases: Map<String, String> = linkedMapOf(
        "text" to "text",
        "desc" to "contentDesc",
        "id" to "resourceId"
    )

    val strategies: List<String> = listOf(
        "text",
        "textContains",
        "resourceId",
        "contentDesc",
        "contentDescContains",
        "focused"
    )

    val screenResponseFields: List<String> = listOf(
        "packageName", "activity", "snapshotToken", "capturedAt", "foregroundStableDuringCapture",
        "partialOutput", "candidateNodeCount", "returnedElementCount", "warnings",
        "omittedInvalidBoundsCount", "omittedLowSignalCount", "omittedNodeCount",
        "omittedCategories", "omittedSummary", "invalidBoundsPresent", "lowSignalPresent",
        "source", "renderMode", "surfaceReadability", "visualAvailable", "previewAvailable",
        "previewToken", "previewWidth", "previewHeight", "accessibilityElementCount",
        "ocrElementCount", "usedOcrFallback", "focusedEditablePresent", "suggestedFallback",
        "suggestedSource", "fallbackReason", "retryHint", "previewImage", "elements", "disclosure"
    )
}
