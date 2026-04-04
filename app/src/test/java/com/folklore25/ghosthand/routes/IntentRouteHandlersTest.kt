/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.routes

import com.folklore25.ghosthand.catalog.GhosthandCommandCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentRouteHandlersTest {
    @Test
    fun thinIntentLayerIsExplicitlyDeferredFromTheCurrentCatalog() {
        assertTrue(
            GhosthandCommandCatalog.commandPayloads().none { payload ->
                payload["plane"] == "intent"
            }
        )
        assertTrue(
            GhosthandCommandCatalog.commands.none { command ->
                command.id in setOf(
                    "recover_to_stable_surface",
                    "dismiss_blocking_dialog_if_present",
                    "tap_and_observe",
                    "ensure_foreground_surface_readable",
                    "ensure_text_submission_attempted"
                )
            }
        )
    }
}
