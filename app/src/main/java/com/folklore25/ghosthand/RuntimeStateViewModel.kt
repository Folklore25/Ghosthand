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
