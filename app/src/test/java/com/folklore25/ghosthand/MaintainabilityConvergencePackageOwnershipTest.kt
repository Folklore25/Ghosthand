/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import java.io.File
import org.junit.Assert.assertFalse
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
        val waitCoordinator = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/wait/GhosthandWaitCoordinator.kt",
            "src/main/java/com/folklore25/ghosthand/wait/GhosthandWaitCoordinator.kt"
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
        assertTrue(stateCoordinator.contains("import com.folklore25.ghosthand.wait.GhosthandWaitCoordinator"))

        assertTrue(payloads.contains("package com.folklore25.ghosthand.payload"))
        assertTrue(routePolicies.contains("package com.folklore25.ghosthand.routes"))
        assertTrue(commandCatalog.contains("package com.folklore25.ghosthand.catalog"))
        assertTrue(waitLogic.contains("package com.folklore25.ghosthand.wait"))
        assertTrue(waitCoordinator.contains("package com.folklore25.ghosthand.wait"))
        assertTrue(interactionPlane.contains("package com.folklore25.ghosthand.interaction.execution"))
    }

    @Test
    fun runtimeDomainTestsLiveUnderMatchingBehaviorPackages() {
        val expectedPaths = listOf(
            "app/src/test/java/com/folklore25/ghosthand/server/LocalApiServerCapabilityPolicyTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/server/LocalApiServerRequestParsingTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/routes/GhosthandRoutePoliciesTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/routes/RouteDisclosureBuildersTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/payload/GhosthandApiPayloadsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/state/StateCoordinatorStatePayloadTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/AccessibilityTreeSnapshotProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/ScreenOcrProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/wait/GhosthandWaitLogicTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/wait/WaitOutcomeTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogTest.kt"
        )
        val retiredFlatPaths = listOf(
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerCapabilityPolicyTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerRequestParsingTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandRoutePoliciesTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerDisclosureTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandApiPayloadsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/AccessibilityTreeSnapshotProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/ScreenOcrProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandWaitLogicTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/WaitOutcomeTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandCommandCatalogTest.kt"
        )

        expectedPaths.forEach { path ->
            assertTrue(
                "Expected runtime-domain test at $path",
                listOf(path, path.removePrefix("app/")).any { candidate -> File(candidate).exists() }
            )
        }
        retiredFlatPaths.forEach { path ->
            assertFalse(
                "Flat runtime-domain test should be retired at $path",
                listOf(path, path.removePrefix("app/")).any { candidate -> File(candidate).exists() }
            )
        }
    }

    @Test
    fun runtimeOwnersDoNotDependOnTheTransitionalPayloadFacade() {
        val runtimeOwnerPaths = listOf(
            "app/src/main/java/com/folklore25/ghosthand/AccessibilityTreeSnapshotProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionNavigationRouteHandlers.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionRouteSupport.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/action/ActionTapClickRouteHandlers.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/input/InputRouteHandlers.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadFindRouteHandlers.kt",
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenRouteHandlers.kt",
            "app/src/main/java/com/folklore25/ghosthand/screen/find/ScreenFindPayloads.kt",
            "app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadComposer.kt",
            "app/src/main/java/com/folklore25/ghosthand/server/LocalApiServerCore.kt"
        )

        runtimeOwnerPaths.forEach { path ->
            val source = TestFileSupport.readProjectFile(path, path.removePrefix("app/"))
            assertFalse(
                "Runtime owner should not depend on GhosthandApiPayloads: $path",
                source.contains("GhosthandApiPayloads")
            )
        }

        val screenReadPayloadFields = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadFields.kt",
            "src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadFields.kt"
        )
        assertFalse(
            "Screen fallback fields should not carry a dead includeRetryHint parameter",
            screenReadPayloadFields.contains("includeRetryHint")
        )
    }

    @Test
    fun rootDirectoryIsMateriallyReducedAndProviderOwnershipUsesDomainFolders() {
        val rootFiles = File("app/src/main/java/com/folklore25/ghosthand")
            .walkTopDown()
            .maxDepth(1)
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .toList()

        assertTrue(
            "Expected the root directory to be materially reduced, found ${rootFiles.size} files",
            rootFiles.size <= 38
        )

        val movedPaths = listOf(
            "app/src/main/java/com/folklore25/ghosthand/state/device/DeviceSnapshotProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/state/device/ForegroundAppProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/state/device/PermissionSnapshotProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/state/diagnostics/HomeDiagnosticsProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/state/diagnostics/FirstLaunchAcknowledgementStore.kt",
            "app/src/main/java/com/folklore25/ghosthand/state/read/AccessibilityStatusProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/interaction/clipboard/ClipboardProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/interaction/clipboard/ClipboardReadFallbackState.kt",
            "app/src/main/java/com/folklore25/ghosthand/integration/github/GitHubReleaseInfo.kt",
            "app/src/main/java/com/folklore25/ghosthand/integration/github/GitHubReleaseRepository.kt",
            "app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt",
            "app/src/main/java/com/folklore25/ghosthand/notification/NotificationBuffer.kt",
            "app/src/main/java/com/folklore25/ghosthand/notification/NotificationDispatcher.kt",
            "app/src/main/java/com/folklore25/ghosthand/screen/ocr/ScreenOcrProvider.kt"
        )

        movedPaths.forEach { path ->
            assertTrue(
                "Expected moved ownership path at $path",
                listOf(path, path.removePrefix("app/")).any { candidate -> File(candidate).exists() }
            )
        }
    }
}
