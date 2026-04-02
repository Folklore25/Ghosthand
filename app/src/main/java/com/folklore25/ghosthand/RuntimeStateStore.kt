/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant

object RuntimeStateStore {
    private const val ACCESSIBILITY_LOG_TAG = "GhostAccessibilityState"
    private const val BOOTSTRAP_LOG_TAG = "GhostBootstrap"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runtimeState = MutableLiveData(RuntimeState())

    fun observe(): LiveData<RuntimeState> = runtimeState

    fun snapshot(): RuntimeState = runtimeState.value ?: RuntimeState()

    fun refreshRuntimeSnapshot(context: Context) {
        val appContext = context.applicationContext
        try {
            val diagnosticsSnapshot = HomeDiagnosticsProvider(appContext).snapshot()
            val permissionSnapshot = PermissionSnapshotProvider(appContext).snapshot()
            val foregroundSnapshot = ForegroundAppProvider(appContext).snapshot()
            val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
            val accessibilitySnapshot = AccessibilityStatusProvider(appContext)
                .snapshot(
                    isConnected = executionStatus.connected,
                    isDispatchCapable = executionStatus.dispatchCapable
                )
            val screenshotPermissionGranted =
                GhosthandServiceRegistry.getInstanceIfRunning()?.hasMediaProjection() == true
            val capabilityPolicy = CapabilityPolicyStore.getInstance(appContext).snapshot()
            val capabilityAccess = CapabilityAccessSnapshotFactory.create(
                accessibilityStatus = accessibilitySnapshot,
                mediaProjectionGranted = screenshotPermissionGranted,
                policy = capabilityPolicy
            )

            updateState { current ->
                val next = current.copy(
                    buildVersion = diagnosticsSnapshot.buildVersion,
                    installIdentity = diagnosticsSnapshot.installIdentity,
                    tapProbeUiBuildState = diagnosticsSnapshot.tapProbeUiBuildState,
                    writeSecureSettingsGranted = permissionSnapshot.writeSecureSettings,
                    foregroundPackage = foregroundSnapshot.packageName,
                    screenshotPermissionGranted = screenshotPermissionGranted,
                    accessibilityServiceConnected = accessibilitySnapshot.connected,
                    accessibilityDispatchCapable = accessibilitySnapshot.dispatchCapable,
                    accessibilityEnabled = accessibilitySnapshot.enabled,
                    accessibilityHealthy = accessibilitySnapshot.healthy,
                    accessibilityStatus = accessibilitySnapshot.status,
                    capabilityPolicy = capabilityPolicy,
                    capabilityAccess = capabilityAccess
                )
                logAccessibilityStateTransition(current, next, "runtime_snapshot")
                next
            }
        } catch (error: Exception) {
            recordRuntimeSnapshotFailure(
                source = "runtime_snapshot",
                preconditions = "serviceRunning=${snapshot().foregroundServiceRunning}",
                error = error
            )
        }
    }

    fun refreshLocalizedUiText(context: Context) {
        AppTextResolver.initialize(context)
        updateState { current ->
            current.copy(
                statusText = localizedStatusTextFor(current),
                lastServiceAction = localizedLastServiceActionFor(current)
            )
        }
    }

    fun markAppStarted() {
        updateState { current ->
            current.copy(
                appStarted = true,
                appStartedAtElapsedRealtimeMs = current.appStartedAtElapsedRealtimeMs ?: SystemClock.elapsedRealtime(),
                appStartedAtIso = current.appStartedAtIso ?: Instant.now().toString(),
                statusText = AppTextResolver.getString(R.string.status_app_started),
                lastAccessibilityHelperResult = if (current.lastAccessibilityHelperResult == "Not run") {
                    AppTextResolver.getString(R.string.accessibility_helper_result_default)
                } else {
                    current.lastAccessibilityHelperResult
                }
            )
        }
    }

    fun refreshHomeDiagnostics(context: Context) {
        refreshRuntimeSnapshot(context)
    }

    fun markLocalApiServerStarted() {
        updateState { current ->
            current.copy(
                localApiServerRunning = true,
                recoverableFailureStatus = null,
                recoverableFailureAction = null,
                statusText = AppTextResolver.getString(R.string.status_api_listening)
            )
        }
    }

    fun markLocalApiServerStopped() {
        updateState { current ->
            current.copy(
                localApiServerRunning = false,
                statusText = if (current.foregroundServiceRunning) {
                    AppTextResolver.getString(R.string.status_service_without_api)
                } else {
                    AppTextResolver.getString(R.string.status_api_stopped)
                }
            )
        }
    }

    fun markLocalApiServerFailed(reason: String) {
        updateState { current ->
            current.copy(
                localApiServerRunning = false,
                statusText = AppTextResolver.getString(R.string.status_api_failed, reason)
            )
        }
    }

    fun markServiceStartRequested() {
        updateState { current ->
            current.copy(
                recoverableFailureStatus = null,
                recoverableFailureAction = null,
                lastServiceAction = AppTextResolver.getString(R.string.status_service_requested),
                statusText = AppTextResolver.getString(R.string.status_service_requested)
            )
        }
    }

    fun markServiceCreated() {
        updateState { current ->
            current.copy(
                recoverableFailureStatus = null,
                recoverableFailureAction = null,
                lastServiceAction = AppTextResolver.getString(R.string.status_service_created),
                statusText = AppTextResolver.getString(R.string.status_service_created)
            )
        }
    }

    fun markServiceRunning() {
        updateState { current ->
            current.copy(
                foregroundServiceRunning = true,
                recoverableFailureStatus = null,
                recoverableFailureAction = null,
                lastServiceAction = AppTextResolver.getString(R.string.status_service_running),
                statusText = AppTextResolver.getString(R.string.status_service_running)
            )
        }
    }

    fun markServiceStopped() {
        updateState { current ->
            current.copy(
                foregroundServiceRunning = false,
                screenshotPermissionGranted = false,
                lastServiceAction = if (current.recoverableFailureAction != null) {
                    current.lastServiceAction
                } else {
                    AppTextResolver.getString(R.string.status_service_stopped)
                },
                statusText = if (current.recoverableFailureStatus != null) {
                    current.statusText
                } else {
                    AppTextResolver.getString(R.string.status_service_stopped)
                }
            )
        }
    }

    fun markTapProbeTapped(source: String) {
        updateState { current ->
            val nextCount = current.tapProbeCount + 1
            current.copy(
                tapProbeCount = nextCount,
                tapProbeResultText = "Tap probe count: $nextCount",
                statusText = "Tap probe activated via $source."
            )
        }
    }

    fun markSwipeProbeState(scrollY: Int, topVisibleItem: Int) {
        updateState { current ->
            if (current.swipeProbeScrollY == scrollY &&
                current.swipeProbeTopVisibleItem == topVisibleItem
            ) {
                current
            } else {
                current.copy(
                    swipeProbeScrollY = scrollY,
                    swipeProbeTopVisibleItem = topVisibleItem,
                    swipeProbeSignalText = "Swipe probe scrollY: $scrollY | top item: ${topVisibleItem.toString().padStart(2, '0')}"
                )
            }
        }
    }

    fun markAccessibilityHelperResult(resultText: String) {
        updateState { current ->
            current.copy(
                lastAccessibilityHelperResult = resultText
            )
        }
    }

    fun markAccessibilityServiceConnected(isConnected: Boolean) {
        updateState { current ->
            val next = current.copy(
                accessibilityServiceConnected = isConnected,
                accessibilityDispatchCapable = if (isConnected) {
                    current.accessibilityDispatchCapable
                } else {
                    false
                }
            )
            logAccessibilityStateTransition(current, next, "service_connection")
            next
        }
    }

    fun refreshAccessibilityStatus(context: Context) {
        refreshRuntimeSnapshot(context)
    }

    fun recordRuntimeStartFailure(preconditions: String, error: Throwable) {
        recordRecoverableFailure(
            action = "start_runtime",
            preconditions = preconditions,
            fallback = "runtime_remains_stopped",
            failureClass = error.javaClass.simpleName,
            statusMessage = AppTextResolver.getString(R.string.status_runtime_start_failed),
            actionMessage = AppTextResolver.getString(
                R.string.status_runtime_start_failed_action,
                error.javaClass.simpleName
            ),
            error = error
        )
    }

    fun recordAccessibilitySettingsFailure(
        preconditions: String,
        failureClass: String,
        error: Throwable? = null
    ) {
        recordRecoverableFailure(
            action = "open_accessibility_settings",
            preconditions = preconditions,
            fallback = "stay_on_permissions_screen",
            failureClass = failureClass,
            statusMessage = AppTextResolver.getString(R.string.status_accessibility_settings_failed),
            actionMessage = AppTextResolver.getString(
                R.string.status_accessibility_settings_failed_action,
                failureClass
            ),
            error = error
        )
    }

    fun recordRuntimeSnapshotFailure(
        source: String,
        preconditions: String,
        error: Throwable
    ) {
        recordRecoverableFailure(
            action = source,
            preconditions = preconditions,
            fallback = "keep_previous_runtime_state",
            failureClass = error.javaClass.simpleName,
            statusMessage = AppTextResolver.getString(R.string.status_runtime_snapshot_failed),
            actionMessage = AppTextResolver.getString(
                R.string.status_runtime_snapshot_failed_action,
                error.javaClass.simpleName
            ),
            error = error
        )
    }

    fun recordServiceBootstrapFailure(
        stage: String,
        preconditions: String,
        error: Throwable
    ) {
        recordRecoverableFailure(
            action = stage,
            preconditions = preconditions,
            fallback = "stop_service_and_keep_app_alive",
            failureClass = error.javaClass.simpleName,
            statusMessage = AppTextResolver.getString(R.string.status_service_bootstrap_failed),
            actionMessage = AppTextResolver.getString(
                R.string.status_service_bootstrap_failed_action,
                error.javaClass.simpleName
            ),
            error = error
        )
    }

    private fun logAccessibilityStateTransition(
        previous: RuntimeState,
        next: RuntimeState,
        source: String
    ) {
        if (previous.accessibilityEnabled == next.accessibilityEnabled &&
            previous.accessibilityServiceConnected == next.accessibilityServiceConnected &&
            previous.accessibilityDispatchCapable == next.accessibilityDispatchCapable &&
            previous.accessibilityStatus == next.accessibilityStatus
        ) {
            return
        }

        Log.i(
            ACCESSIBILITY_LOG_TAG,
            "event=accessibility_transition source=$source enabled=${next.accessibilityEnabled} connected=${next.accessibilityServiceConnected} dispatchCapable=${next.accessibilityDispatchCapable} status=${next.accessibilityStatus}"
        )
    }

    private fun updateState(transform: (RuntimeState) -> RuntimeState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val current = runtimeState.value ?: RuntimeState()
            runtimeState.value = transform(current)
        } else {
            mainHandler.post {
                val current = runtimeState.value ?: RuntimeState()
                runtimeState.value = transform(current)
            }
        }
    }

    private fun recordRecoverableFailure(
        action: String,
        preconditions: String,
        fallback: String,
        failureClass: String,
        statusMessage: String,
        actionMessage: String,
        error: Throwable?
    ) {
        Log.e(
            BOOTSTRAP_LOG_TAG,
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

    private fun localizedStatusTextFor(state: RuntimeState): String {
        return when {
            state.recoverableFailureStatus != null -> state.recoverableFailureStatus
            state.localApiServerRunning -> AppTextResolver.getString(R.string.status_api_listening)
            state.foregroundServiceRunning -> AppTextResolver.getString(R.string.status_service_without_api)
            state.appStarted -> AppTextResolver.getString(R.string.status_app_started)
            else -> state.statusText
        }
    }

    private fun localizedLastServiceActionFor(state: RuntimeState): String {
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
