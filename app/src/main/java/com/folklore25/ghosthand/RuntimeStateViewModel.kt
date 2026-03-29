/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData

class RuntimeStateViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val capabilityPolicyStore = CapabilityPolicyStore(application)

    val runtimeState: LiveData<RuntimeState> = RuntimeStateStore.observe()
    internal val homeScreenState = MediatorLiveData<HomeScreenUiState>().apply {
        addSource(runtimeState) { state ->
            value = HomeScreenUiStateFactory.create(state)
        }
    }
    internal val permissionsScreenState = MediatorLiveData<PermissionsScreenUiState>().apply {
        addSource(runtimeState) { state ->
            value = PermissionsScreenUiStateFactory.create(state)
        }
    }

    fun requestForegroundServiceStart() {
        RuntimeStateStore.markServiceStartRequested()
    }

    fun setCapabilityPolicy(capability: GhosthandCapability, allowed: Boolean) {
        capabilityPolicyStore.setAllowed(capability, allowed)
        RuntimeStateStore.refreshRuntimeSnapshot(getApplication())
    }

    fun recordTapProbeTap(source: String) {
        RuntimeStateStore.markTapProbeTapped(source)
    }
}
