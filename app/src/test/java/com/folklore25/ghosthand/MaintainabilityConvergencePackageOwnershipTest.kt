/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import org.junit.Assert.assertTrue
import org.junit.Test

class MaintainabilityConvergencePackageOwnershipTest {
    @Test
    fun runtimeOwnershipLivesInDomainPackages() {
        val server = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/server/LocalApiServer.kt",
            "src/main/java/com/folklore25/ghosthand/server/LocalApiServer.kt"
        )
        val stateCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt"
        )
        val payloads = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/payload/GhosthandApiPayloads.kt",
            "src/main/java/com/folklore25/ghosthand/payload/GhosthandApiPayloads.kt"
        )
        val routePolicies = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/GhosthandRoutePolicies.kt",
            "src/main/java/com/folklore25/ghosthand/routes/GhosthandRoutePolicies.kt"
        )
        val commandCatalog = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalog.kt",
            "src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalog.kt"
        )
        val waitLogic = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/wait/GhosthandWaitLogic.kt",
            "src/main/java/com/folklore25/ghosthand/wait/GhosthandWaitLogic.kt"
        )
        val interactionPlane = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandInteractionPlane.kt",
            "src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandInteractionPlane.kt"
        )

        assertTrue(server.contains("package com.folklore25.ghosthand.server"))
        assertTrue(server.contains("import com.folklore25.ghosthand.state.StateCoordinator"))
        assertTrue(server.contains("import com.folklore25.ghosthand.routes.GhosthandRoutePolicies"))

        assertTrue(stateCoordinator.contains("package com.folklore25.ghosthand.state"))
        assertTrue(stateCoordinator.contains("import com.folklore25.ghosthand.interaction.execution.AccessibilityInteractionPlane"))
        assertTrue(stateCoordinator.contains("import com.folklore25.ghosthand.interaction.execution.GhosthandInteractionPlane"))
        assertTrue(stateCoordinator.contains("import com.folklore25.ghosthand.payload.GhosthandApiPayloads"))
        assertTrue(stateCoordinator.contains("import com.folklore25.ghosthand.wait.GhosthandWaitLogic"))

        assertTrue(payloads.contains("package com.folklore25.ghosthand.payload"))
        assertTrue(routePolicies.contains("package com.folklore25.ghosthand.routes"))
        assertTrue(commandCatalog.contains("package com.folklore25.ghosthand.catalog"))
        assertTrue(waitLogic.contains("package com.folklore25.ghosthand.wait"))
        assertTrue(interactionPlane.contains("package com.folklore25.ghosthand.interaction.execution"))
    }
}
