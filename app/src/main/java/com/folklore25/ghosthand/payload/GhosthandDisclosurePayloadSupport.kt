/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.payload

import com.folklore25.ghosthand.screen.find.FindMissHint

import com.folklore25.ghosthand.R

internal object GhosthandDisclosurePayloads {
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
}
