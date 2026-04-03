/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.routes.GhosthandRoutePolicies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandRoutePoliciesTest {
    @Test
    fun policiesCoverCommandsAndWaitRoutes() {
        val commands = GhosthandRoutePolicies.policyFor("/commands")
        assertNotNull(commands)
        assertEquals(setOf("GET"), commands!!.allowedMethods)

        val wait = GhosthandRoutePolicies.policyFor("/wait")
        assertNotNull(wait)
        assertEquals(setOf("GET", "POST"), wait!!.allowedMethods)
    }

    @Test
    fun notifySupportsReadPostAndDelete() {
        val notify = GhosthandRoutePolicies.policyFor("/notify")
        assertNotNull(notify)
        assertTrue(notify!!.allowedMethods.contains("GET"))
        assertTrue(notify.allowedMethods.contains("POST"))
        assertTrue(notify.allowedMethods.contains("DELETE"))
    }

    @Test
    fun unknownPathsReturnNullPolicy() {
        assertNull(GhosthandRoutePolicies.policyFor("/unknown"))
    }
}
