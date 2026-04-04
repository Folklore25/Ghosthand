/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

import com.folklore25.ghosthand.integration.projection.MediaProjectionProvider
import com.folklore25.ghosthand.interaction.execution.GhostAccessibilityExecutionCoreRegistry
import com.folklore25.ghosthand.state.read.AccessibilityStatusProvider
import com.folklore25.ghosthand.state.read.AccessibilityStatusSnapshot


class CapabilityAccessResolver(
    private val accessibilityStatusProvider: AccessibilityStatusProvider,
    private val mediaProjectionProvider: MediaProjectionProvider,
    private val capabilityPolicyStore: CapabilityPolicyStore
) {
    fun accessibilityStatusSnapshot(): AccessibilityStatusSnapshot {
        val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
        return accessibilityStatusProvider.snapshot(
            isConnected = executionStatus.connected,
            isDispatchCapable = executionStatus.dispatchCapable
        )
    }

    fun capabilityAccessSnapshot(
        accessibilitySnapshot: AccessibilityStatusSnapshot = accessibilityStatusSnapshot()
    ): CapabilityAccessSnapshot {
        return CapabilityAccessSnapshotFactory.create(
            accessibilityStatus = accessibilitySnapshot,
            mediaProjectionGranted = mediaProjectionProvider.hasProjection(),
            policy = capabilityPolicyStore.snapshot()
        )
    }
}
