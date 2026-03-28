/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class RuntimeStateViewModel : ViewModel() {
    val runtimeState: LiveData<RuntimeState> = RuntimeStateStore.observe()

    fun requestForegroundServiceStart() {
        RuntimeStateStore.markServiceStartRequested()
    }

    fun recordTapProbeTap(source: String) {
        RuntimeStateStore.markTapProbeTapped(source)
    }
}
