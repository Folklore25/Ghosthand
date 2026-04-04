/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.capability

import com.folklore25.ghosthand.R
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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.util.Log
import java.io.IOException

enum class GhosthandCapability(
    val prefKey: String
) {
    Accessibility("accessibility"),
    Screenshot("screenshot")
}

data class CapabilityPolicySnapshot(
    val accessibilityAllowed: Boolean = false,
    val screenshotAllowed: Boolean = false
) {
    fun allowed(capability: GhosthandCapability): Boolean {
        return when (capability) {
            GhosthandCapability.Accessibility -> accessibilityAllowed
            GhosthandCapability.Screenshot -> screenshotAllowed
        }
    }
}

private val Context.capabilityPolicyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = CapabilityPolicyStore.DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = CapabilityPolicyStore.LEGACY_SHARED_PREFERENCES_NAME
            )
        )
    }
)

class CapabilityPolicyStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    constructor(context: Context) : this(context.applicationContext.capabilityPolicyDataStore)

    private val snapshotState = MutableStateFlow(CapabilityPolicySnapshot())

    init {
        scope.launch {
            observeDataStore().collect { snapshot ->
                snapshotState.value = snapshot
            }
        }
    }

    fun observe(): Flow<CapabilityPolicySnapshot> {
        return snapshotState
    }

    fun snapshot(): CapabilityPolicySnapshot = snapshotState.value

    fun isAllowed(capability: GhosthandCapability): Boolean = snapshot().allowed(capability)

    fun setAllowed(capability: GhosthandCapability, allowed: Boolean) {
        snapshotState.value = when (capability) {
            GhosthandCapability.Accessibility -> snapshotState.value.copy(accessibilityAllowed = allowed)
            GhosthandCapability.Screenshot -> snapshotState.value.copy(screenshotAllowed = allowed)
        }
        scope.launch {
            try {
                dataStore.edit { prefs ->
                    prefs[preferenceKey(capability)] = allowed
                }
            } catch (error: Exception) {
                Log.w(
                    LOG_TAG,
                    "component=CapabilityPolicyStore operation=setAllowed capability=${capability.name} failure=${error.javaClass.simpleName}",
                    error
                )
            }
        }
    }

    private fun observeDataStore(): Flow<CapabilityPolicySnapshot> {
        return dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map(::toSnapshot)
    }

    private fun toSnapshot(preferences: Preferences): CapabilityPolicySnapshot {
        return CapabilityPolicySnapshot(
            accessibilityAllowed = preferences[preferenceKey(GhosthandCapability.Accessibility)] ?: false,
            screenshotAllowed = preferences[preferenceKey(GhosthandCapability.Screenshot)] ?: false
        )
    }

    companion object {
        internal const val DATASTORE_NAME = "ghosthand_capability_policy"
        internal const val LEGACY_SHARED_PREFERENCES_NAME = "ghosthand_capability_policy"
        private const val LOG_TAG = "CapabilityPolicy"
        private const val PREFS_PREFIX = "capability"
        @Volatile
        private var instance: CapabilityPolicyStore? = null

        fun getInstance(context: Context): CapabilityPolicyStore {
            return instance ?: synchronized(this) {
                instance ?: CapabilityPolicyStore(context.applicationContext).also { instance = it }
            }
        }

        internal fun preferenceKey(capability: GhosthandCapability) =
            booleanPreferencesKey("${PREFS_PREFIX}.${capability.prefKey}")

        internal fun migratedSnapshot(legacyPreferences: Map<String, *>): CapabilityPolicySnapshot {
            return CapabilityPolicySnapshot(
                accessibilityAllowed = legacyPreferences[preferenceKey(GhosthandCapability.Accessibility).name] as? Boolean ?: false,
                screenshotAllowed = legacyPreferences[preferenceKey(GhosthandCapability.Screenshot).name] as? Boolean ?: false
            )
        }
    }
}
