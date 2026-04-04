/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

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
            "app/src/test/java/com/folklore25/ghosthand/server/GhosthandHttpTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/routes/GhosthandRoutePoliciesTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/routes/RouteDisclosureBuildersTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/payload/GhosthandApiPayloadsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/state/StateCoordinatorStatePayloadTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/AccessibilityTreeSnapshotProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/AccessibilityNodeFinderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/AccessibilityNodeLocatorTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/GhosthandSelectorsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/ScreenOcrProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/wait/GhosthandWaitLogicTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/wait/WaitOutcomeTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/capability/CapabilityAccessSnapshotFactoryTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/capability/CapabilityPolicyStoreTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/interaction/ClipboardReadFallbackStateTest.kt"
        )
        val retiredFlatPaths = listOf(
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerCapabilityPolicyTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerRequestParsingTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandHttpTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandRoutePoliciesTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/LocalApiServerDisclosureTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandApiPayloadsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/AccessibilityNodeFinderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/AccessibilityNodeLocatorTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandSelectorsTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/AccessibilityTreeSnapshotProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/ScreenOcrProviderTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandWaitLogicTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/WaitOutcomeTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/GhosthandCommandCatalogTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/CapabilityAccessSnapshotFactoryTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/CapabilityPolicyStoreTest.kt",
            "app/src/test/java/com/folklore25/ghosthand/ClipboardReadFallbackStateTest.kt"
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
            "app/src/main/java/com/folklore25/ghosthand/screen/read/AccessibilityTreeSnapshotProvider.kt",
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
        val rootDirectory = listOf(
            File("app/src/main/java/com/folklore25/ghosthand"),
            File("src/main/java/com/folklore25/ghosthand")
        ).firstOrNull(File::exists)
            ?: error("Root source directory not found")
        val rootFiles = rootDirectory
            .walkTopDown()
            .maxDepth(1)
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .toList()

        assertTrue(
            "Expected the root directory to be materially reduced, found ${rootFiles.size} files",
            rootFiles.size <= 1 && rootFiles.contains("GhosthandApp.kt")
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
