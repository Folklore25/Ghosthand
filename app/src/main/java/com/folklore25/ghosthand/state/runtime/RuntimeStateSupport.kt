/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.state.runtime

import android.util.Log
import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.ui.common.model.AppTextResolver

internal object RuntimeStateFailureSupport {
    fun recordRecoverableFailure(
        logTag: String,
        updateState: ((RuntimeState) -> RuntimeState) -> Unit,
        action: String,
        preconditions: String,
        fallback: String,
        failureClass: String,
        statusMessage: String,
        actionMessage: String,
        error: Throwable?
    ) {
        Log.e(
            logTag,
            "event=bootstrap_failure action=$action preconditions=$preconditions failureClass=$failureClass fallback=$fallback",
            error
        )
        updateState { current ->
            current.copy(
                recoverableFailureAction = actionMessage,
                recoverableFailureStatus = statusMessage,
                lastServiceAction = actionMessage,
                statusText = statusMessage
            )
        }
    }

    fun localizedStatusTextFor(state: RuntimeState): String {
        return when {
            state.recoverableFailureStatus != null -> state.recoverableFailureStatus
            state.localApiServerRunning -> AppTextResolver.getString(R.string.status_api_listening)
            state.foregroundServiceRunning -> AppTextResolver.getString(R.string.status_service_without_api)
            state.appStarted -> AppTextResolver.getString(R.string.status_app_started)
            else -> state.statusText
        }
    }

    fun localizedLastServiceActionFor(state: RuntimeState): String {
        if (state.lastServiceAction.isBlank()) {
            return state.lastServiceAction
        }

        return when {
            state.foregroundServiceRunning && state.localApiServerRunning ->
                AppTextResolver.getString(R.string.status_service_running)
            state.foregroundServiceRunning ->
                AppTextResolver.getString(R.string.status_service_created)
            !state.foregroundServiceRunning && state.appStarted ->
                AppTextResolver.getString(R.string.status_service_stopped)
            else ->
                state.lastServiceAction
        }
    }
}
