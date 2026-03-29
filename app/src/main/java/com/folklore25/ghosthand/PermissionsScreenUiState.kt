/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

internal data class PermissionsScreenUiState(
    val accessibility: CapabilityPermissionUiState,
    val screenshot: CapabilityPermissionUiState
)

internal object PermissionsScreenUiStateFactory {
    fun create(
        runtimeState: RuntimeState,
        textLookup: UiTextLookup = AppUiTextLookup
    ): PermissionsScreenUiState {
        return PermissionsScreenUiState(
            accessibility = CapabilityUiStateFactory.forCapability(
                capability = GhosthandCapability.Accessibility,
                runtimeState = runtimeState,
                textLookup = textLookup
            ),
            screenshot = CapabilityUiStateFactory.forCapability(
                capability = GhosthandCapability.Screenshot,
                runtimeState = runtimeState,
                textLookup = textLookup
            )
        )
    }
}
