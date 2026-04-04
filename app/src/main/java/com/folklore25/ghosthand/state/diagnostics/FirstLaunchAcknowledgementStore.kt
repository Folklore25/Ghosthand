/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.diagnostics

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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.firstLaunchAcknowledgementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = FirstLaunchAcknowledgementStore.DATASTORE_NAME
)

class FirstLaunchAcknowledgementStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    constructor(context: Context) : this(context.applicationContext.firstLaunchAcknowledgementDataStore)

    fun loadAcknowledged(onLoaded: (Boolean) -> Unit) {
        scope.launch {
            val acknowledged = readAcknowledged()
            mainHandler.post { onLoaded(acknowledged) }
        }
    }

    fun markAcknowledged() {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[ACKNOWLEDGED_KEY] = true
                }
            } catch (error: Exception) {
                Log.w(
                    LOG_TAG,
                    "component=FirstLaunchAcknowledgementStore operation=markAcknowledged failure=${error.javaClass.simpleName}",
                    error
                )
            }
        }
    }

    private suspend fun readAcknowledged(): Boolean {
        return dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences[ACKNOWLEDGED_KEY] ?: false }
            .first()
    }

    companion object {
        internal const val DATASTORE_NAME = "ghosthand_first_launch_acknowledgement"
        private const val LOG_TAG = "FirstLaunchAck"
        private val ACKNOWLEDGED_KEY = booleanPreferencesKey("first_launch_acknowledged")

        @Volatile
        private var instance: FirstLaunchAcknowledgementStore? = null

        fun getInstance(context: Context): FirstLaunchAcknowledgementStore {
            return instance ?: synchronized(this) {
                instance ?: FirstLaunchAcknowledgementStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
