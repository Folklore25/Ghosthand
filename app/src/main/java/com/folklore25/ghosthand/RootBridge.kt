/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class RootBridge {
    fun availability(): RootAvailabilitySnapshot {
        val result = checkAvailability()
        val available = result.succeeded && result.stdout.contains("uid=0")
        val status = when {
            available -> "available"
            result.suPath != null -> "authorization_required"
            else -> "unavailable"
        }
        if (!available) {
            logDiscovery()
        }
        return RootAvailabilitySnapshot(
            implemented = true,
            available = available,
            healthy = available,
            status = status
        )
    }

    fun checkAvailability(): RootCommandResult {
        return execute(RootAction.CheckAvailability)
    }

    fun launchApp(packageName: String, activity: String?): RootCommandResult {
        return when {
            !isValidPackageName(packageName) -> RootCommandResult(
                succeeded = false,
                suPath = null,
                exitCode = null,
                stdout = "",
                stderr = "invalid_package"
            )
            activity != null && !isValidActivityName(activity) -> RootCommandResult(
                succeeded = false,
                suPath = null,
                exitCode = null,
                stdout = "",
                stderr = "invalid_activity"
            )
            else -> execute(RootAction.LaunchApp(packageName, activity))
        }
    }

    fun stopApp(packageName: String): RootCommandResult {
        if (!isValidPackageName(packageName)) {
            return RootCommandResult(
                succeeded = false,
                suPath = null,
                exitCode = null,
                stdout = "",
                stderr = "invalid_package"
            )
        }
        return execute(RootAction.StopApp(packageName))
    }

    fun getSecureSetting(name: RootSecureSettingKey): RootCommandResult {
        return execute(RootAction.GetSecureSetting(name))
    }

    fun putSecureSetting(name: RootSecureSettingKey, value: String): RootCommandResult {
        return execute(RootAction.PutSecureSetting(name, value))
    }

    private fun execute(action: RootAction): RootCommandResult {
        var lastError = "su_not_found"

        for (suPath in SU_CANDIDATES) {
            try {
                val process = ProcessBuilder(suPath, "-c", action.command)
                    .redirectErrorStream(false)
                    .start()
                val stdout = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                val stderr = process.errorStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                val exitCode = process.waitFor()
                val result = RootCommandResult(
                    succeeded = exitCode == 0,
                    suPath = suPath,
                    exitCode = exitCode,
                    stdout = stdout.trim(),
                    stderr = stderr.trim()
                )
                Log.i(
                    LOG_TAG,
                    "event=root_command action=${action.javaClass.simpleName} suPath=$suPath exitCode=$exitCode success=${result.succeeded} stdout=${result.stdout.asLogValue()} stderr=${result.stderr.asLogValue()}"
                )
                return result
            } catch (error: IOException) {
                lastError = error.message ?: "io_error"
                Log.i(
                    LOG_TAG,
                    "event=root_command_spawn_failed action=${action.javaClass.simpleName} suPath=$suPath message=${lastError.asLogValue()}"
                )
                continue
            }
        }

        val result = RootCommandResult(
            succeeded = false,
            suPath = null,
            exitCode = null,
            stdout = "",
            stderr = lastError
        )
        Log.i(
            LOG_TAG,
            "event=root_command_unavailable action=${action.javaClass.simpleName} success=false stdout=${result.stdout.asLogValue()} stderr=${result.stderr.asLogValue()}"
        )
        return result
    }

    private fun logDiscovery() {
        Log.i(
            LOG_TAG,
            "event=root_discovery_env appPath=${(System.getenv("PATH") ?: "").asLogValue()}"
        )

        val shFile = File(SH_PATH)
        Log.i(
            LOG_TAG,
            "event=root_discovery_shell_path path=$SH_PATH exists=${shFile.exists()} executable=${shFile.canExecute()}"
        )

        DISCOVERY_PATHS.forEach { path ->
            val file = File(path)
            Log.i(
                LOG_TAG,
                "event=root_discovery_candidate path=$path exists=${file.exists()} executable=${file.canExecute()}"
            )
        }

        MAGISK_DISCOVERY_PATHS.forEach { path ->
            val file = File(path)
            Log.i(
                LOG_TAG,
                "event=root_discovery_magisk_candidate path=$path exists=${file.exists()} executable=${file.canExecute()}"
            )
            if (file.exists() && file.canExecute()) {
                runDirect(
                    executable = path,
                    arguments = listOf("su", "-c", "id"),
                    logEvent = "root_discovery_magisk_su"
                )
            }
        }

        if (!shFile.exists()) {
            return
        }

        val shellDiscovery = runDirect(
            executable = SH_PATH,
            arguments = listOf(
                "-c",
                "echo APP_PATH=${'$'}PATH; " +
                    "echo COMMAND_V_SU=${'$'}(command -v su 2>/dev/null); " +
                    "echo WHICH_SU=${'$'}(which su 2>/dev/null); " +
                    "for p in ${DISCOVERY_PATHS.joinToString(" ")}; do " +
                    "[ -e \"${'$'}p\" ] && echo EXISTS=${'$'}p; done"
            ),
            logEvent = "root_discovery_shell"
        )

        runDirect(
            executable = SH_PATH,
            arguments = listOf(
                "-c",
                "pwd; " +
                    "id; " +
                    "echo APP_PATH=${'$'}PATH; " +
                    "echo __LS_DIRS__; " +
                    "ls -ld /apex /apex/com.android.runtime /apex/com.android.runtime/bin 2>&1; " +
                    "echo __LS_BIN__; " +
                    "ls -l /apex/com.android.runtime/bin 2>&1; " +
                    "echo __LS_TARGETS__; " +
                    "ls -l /apex/com.android.runtime/bin/su /apex/com.android.runtime/bin/magisk 2>&1; " +
                    "echo __READLINK_SU__; " +
                    "readlink /apex/com.android.runtime/bin/su 2>&1; " +
                    "echo __READLINK_MAGISK__; " +
                    "readlink /apex/com.android.runtime/bin/magisk 2>&1; " +
                    "echo __MOUNTINFO_APEX__; " +
                    "cat /proc/self/mountinfo | grep apex 2>&1 || true; " +
                    "echo __MOUNT_APEX__; " +
                    "mount | grep apex 2>&1 || true"
            ),
            logEvent = "root_discovery_namespace"
        )

        val visibleSuPath = shellDiscovery.stdout
            .lineSequence()
            .firstOrNull { it.startsWith("COMMAND_V_SU=") }
            ?.removePrefix("COMMAND_V_SU=")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: shellDiscovery.stdout
                .lineSequence()
                .firstOrNull { it.startsWith("WHICH_SU=") }
                ?.removePrefix("WHICH_SU=")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        if (!visibleSuPath.isNullOrBlank()) {
            runDirect(
                executable = visibleSuPath,
                arguments = listOf("-c", "id"),
                logEvent = "root_discovery_visible_su"
            )
        }
    }

    private fun runDirect(
        executable: String,
        arguments: List<String>,
        logEvent: String
    ): RootCommandResult {
        return try {
            val process = ProcessBuilder(buildList {
                add(executable)
                addAll(arguments)
            }).redirectErrorStream(false).start()
            val stdout = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
            val stderr = process.errorStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
            val exitCode = process.waitFor()
            val result = RootCommandResult(
                succeeded = exitCode == 0,
                suPath = executable,
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim()
            )
            Log.i(
                LOG_TAG,
                "event=$logEvent executable=$executable exitCode=$exitCode success=${result.succeeded} stdout=${result.stdout.asLogValue()} stderr=${result.stderr.asLogValue()}"
            )
            result
        } catch (error: IOException) {
            val result = RootCommandResult(
                succeeded = false,
                suPath = executable,
                exitCode = null,
                stdout = "",
                stderr = error.message ?: "io_error"
            )
            Log.i(
                LOG_TAG,
                "event=${logEvent}_spawn_failed executable=$executable stdout=${result.stdout.asLogValue()} stderr=${result.stderr.asLogValue()}"
            )
            result
        }
    }

    private fun isValidPackageName(packageName: String): Boolean {
        return PACKAGE_NAME_REGEX.matches(packageName)
    }

    private fun isValidActivityName(activity: String): Boolean {
        return ACTIVITY_NAME_REGEX.matches(activity)
    }

    private companion object {
        const val LOG_TAG = "RootBridge"
        const val SH_PATH = "/system/bin/sh"
        val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")
        val ACTIVITY_NAME_REGEX = Regex("^[A-Za-z0-9_.$]+$")
        val SU_CANDIDATES = listOf(
            "/apex/com.android.runtime/bin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "su"
        )
        val DISCOVERY_PATHS = listOf(
            "/apex/com.android.runtime/bin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/debug_ramdisk/su"
        )
        val MAGISK_DISCOVERY_PATHS = listOf(
            "/apex/com.android.runtime/bin/magisk",
            "/system/bin/magisk"
        )
    }
}

sealed interface RootAction {
    val command: String

    data object CheckAvailability : RootAction {
        override val command: String = "id"
    }

    data class LaunchApp(
        private val packageName: String,
        private val activity: String?
    ) : RootAction {
        override val command: String = if (activity.isNullOrBlank()) {
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        } else {
            val component = if (activity.contains('/')) {
                activity
            } else {
                "$packageName/$activity"
            }
            "am start -n $component"
        }
    }

    data class StopApp(private val packageName: String) : RootAction {
        override val command: String = "am force-stop $packageName"
    }

    data class GetSecureSetting(private val key: RootSecureSettingKey) : RootAction {
        override val command: String = "settings get secure ${key.rawName}"
    }

    data class PutSecureSetting(
        private val key: RootSecureSettingKey,
        private val value: String
    ) : RootAction {
        override val command: String = "settings put secure ${key.rawName} ${shellEscape(value)}"
    }
}

enum class RootSecureSettingKey(val rawName: String) {
    ACCESSIBILITY_ENABLED("accessibility_enabled"),
    ENABLED_ACCESSIBILITY_SERVICES("enabled_accessibility_services")
}

data class RootCommandResult(
    val succeeded: Boolean,
    val suPath: String?,
    val exitCode: Int?,
    val stdout: String,
    val stderr: String
)

data class RootAvailabilitySnapshot(
    val implemented: Boolean,
    val available: Boolean?,
    val healthy: Boolean?,
    val status: String
)

private fun shellEscape(value: String): String {
    return "'" + value.replace("'", "'\"'\"'") + "'"
}

private fun String.asLogValue(): String {
    return replace('\n', ' ').ifBlank { "\"\"" }
}
