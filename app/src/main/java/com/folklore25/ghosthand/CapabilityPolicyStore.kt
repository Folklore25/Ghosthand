/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

enum class GhosthandCapability(
    val prefKey: String
) {
    Accessibility("accessibility"),
    Screenshot("screenshot"),
    Root("root")
}

data class CapabilityPolicySnapshot(
    val accessibilityAllowed: Boolean = false,
    val screenshotAllowed: Boolean = false,
    val rootAllowed: Boolean = false
) {
    fun allowed(capability: GhosthandCapability): Boolean {
        return when (capability) {
            GhosthandCapability.Accessibility -> accessibilityAllowed
            GhosthandCapability.Screenshot -> screenshotAllowed
            GhosthandCapability.Root -> rootAllowed
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
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.capabilityPolicyDataStore)

    fun observe(): Flow<CapabilityPolicySnapshot> {
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

    fun snapshot(): CapabilityPolicySnapshot = runBlocking { observe().first() }

    fun isAllowed(capability: GhosthandCapability): Boolean = snapshot().allowed(capability)

    fun setAllowed(capability: GhosthandCapability, allowed: Boolean) {
        runBlocking {
            dataStore.edit { prefs ->
                prefs[preferenceKey(capability)] = allowed
            }
        }
    }

    private fun toSnapshot(preferences: Preferences): CapabilityPolicySnapshot {
        return CapabilityPolicySnapshot(
            accessibilityAllowed = preferences[preferenceKey(GhosthandCapability.Accessibility)] ?: false,
            screenshotAllowed = preferences[preferenceKey(GhosthandCapability.Screenshot)] ?: false,
            rootAllowed = preferences[preferenceKey(GhosthandCapability.Root)] ?: false
        )
    }

    companion object {
        internal const val DATASTORE_NAME = "ghosthand_capability_policy"
        internal const val LEGACY_SHARED_PREFERENCES_NAME = "ghosthand_capability_policy"
        private const val PREFS_PREFIX = "capability"

        internal fun preferenceKey(capability: GhosthandCapability) =
            booleanPreferencesKey("${PREFS_PREFIX}.${capability.prefKey}")

        internal fun migratedSnapshot(legacyPreferences: Map<String, *>): CapabilityPolicySnapshot {
            return CapabilityPolicySnapshot(
                accessibilityAllowed = legacyPreferences[preferenceKey(GhosthandCapability.Accessibility).name] as? Boolean ?: false,
                screenshotAllowed = legacyPreferences[preferenceKey(GhosthandCapability.Screenshot).name] as? Boolean ?: false,
                rootAllowed = legacyPreferences[preferenceKey(GhosthandCapability.Root).name] as? Boolean ?: false
            )
        }
    }
}
