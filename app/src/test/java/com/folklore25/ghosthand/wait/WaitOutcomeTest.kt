/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.wait

import com.folklore25.ghosthand.wait.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitOutcomeTest {
    @Test
    fun conditionOutcomeSeparatesSuccessFromStateChange() {
        val outcome = WaitOutcome.forCondition(
            conditionMet = true,
            initialState = UiStateSnapshot("snap1", "pkg", "Activity"),
            finalState = UiStateSnapshot("snap1", "pkg", "Activity"),
            timedOut = false
        )

        assertEquals(true, outcome.conditionMet)
        assertFalse(outcome.stateChanged)
        assertFalse(outcome.timedOut)
    }

    @Test
    fun uiChangeOutcomeLeavesConditionMetUnset() {
        val outcome = WaitOutcome.forUiChange(
            stateChanged = false,
            timedOut = true
        )

        assertNull(outcome.conditionMet)
        assertFalse(outcome.stateChanged)
        assertTrue(outcome.timedOut)
    }
}
