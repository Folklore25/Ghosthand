/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

data class WaitOutcome(
    val conditionMet: Boolean?,
    val stateChanged: Boolean,
    val timedOut: Boolean
) {
    companion object {
        fun forUiChange(stateChanged: Boolean, timedOut: Boolean): WaitOutcome {
            return WaitOutcome(
                conditionMet = null,
                stateChanged = stateChanged,
                timedOut = timedOut
            )
        }

        fun forCondition(
            conditionMet: Boolean,
            initialState: UiStateSnapshot,
            finalState: UiStateSnapshot,
            timedOut: Boolean
        ): WaitOutcome {
            return WaitOutcome(
                conditionMet = conditionMet,
                stateChanged = GhosthandWaitLogic.hasUiChanged(initialState, finalState),
                timedOut = timedOut
            )
        }
    }
}
