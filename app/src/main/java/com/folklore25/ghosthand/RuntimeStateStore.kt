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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runtimeState = MutableLiveData(RuntimeState())

    fun observe(): LiveData<RuntimeState> = runtimeState

    fun snapshot(): RuntimeState = runtimeState.value ?: RuntimeState()

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
        val snapshot = HomeDiagnosticsProvider(context.applicationContext).snapshot()
        val permissionSnapshot = PermissionSnapshotProvider(context.applicationContext).snapshot()
        val foregroundSnapshot = ForegroundAppProvider(context.applicationContext).snapshot()
        val rootSnapshot = RootControlProvider().availability()
        updateState { current ->
            current.copy(
                buildVersion = snapshot.buildVersion,
                installIdentity = snapshot.installIdentity,
                tapProbeUiBuildState = snapshot.tapProbeUiBuildState,
                writeSecureSettingsGranted = permissionSnapshot.writeSecureSettings,
                foregroundPackage = foregroundSnapshot.packageName,
                rootAvailable = rootSnapshot.available,
                rootHealthy = rootSnapshot.healthy,
                rootStatus = rootSnapshot.status
            )
        }
    }

    fun markLocalApiServerStarted() {
        updateState { current ->
            current.copy(
                localApiServerRunning = true,
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
                lastServiceAction = AppTextResolver.getString(R.string.status_service_requested),
                statusText = AppTextResolver.getString(R.string.status_service_requested)
            )
        }
    }

    fun markServiceCreated() {
        updateState { current ->
            current.copy(
                lastServiceAction = AppTextResolver.getString(R.string.status_service_created),
                statusText = AppTextResolver.getString(R.string.status_service_created)
            )
        }
    }

    fun markServiceRunning() {
        updateState { current ->
            current.copy(
                foregroundServiceRunning = true,
                lastServiceAction = AppTextResolver.getString(R.string.status_service_running),
                statusText = AppTextResolver.getString(R.string.status_service_running)
            )
        }
    }

    fun markServiceStopped() {
        updateState { current ->
            current.copy(
                foregroundServiceRunning = false,
                lastServiceAction = AppTextResolver.getString(R.string.status_service_stopped),
                statusText = AppTextResolver.getString(R.string.status_service_stopped)
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
        val executionStatus = GhostAccessibilityExecutionCoreRegistry.currentStatus()
        val snapshot = AccessibilityStatusProvider(context.applicationContext)
            .snapshot(
                isConnected = executionStatus.connected,
                isDispatchCapable = executionStatus.dispatchCapable
            )
        updateState { current ->
            val next = current.copy(
                accessibilityServiceConnected = snapshot.connected,
                accessibilityDispatchCapable = snapshot.dispatchCapable,
                accessibilityEnabled = snapshot.enabled,
                accessibilityHealthy = snapshot.healthy,
                accessibilityStatus = snapshot.status
            )
            logAccessibilityStateTransition(current, next, "status_refresh")
            next
        }
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
}
