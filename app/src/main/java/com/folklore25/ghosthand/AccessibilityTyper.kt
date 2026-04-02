/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import android.util.Log

class AccessibilityTyper {
    fun typeText(text: String): TypeAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return TypeAttemptResult.failure(
                reason = TypeFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val dispatchResult = service.performSetText(text)
        return when {
            dispatchResult.performed -> {
                Log.i(LOG_TAG, "event=type_path path=${dispatchResult.attemptedPath} success=true")
                TypeAttemptResult.success(attemptedPath = dispatchResult.attemptedPath)
            }
            !dispatchResult.targetFound -> {
                Log.i(LOG_TAG, "event=type_path path=${dispatchResult.attemptedPath} success=false")
                TypeAttemptResult.failure(
                    reason = TypeFailureReason.NO_EDITABLE_TARGET,
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
            else -> {
                Log.i(LOG_TAG, "event=type_path path=${dispatchResult.attemptedPath} success=false")
                TypeAttemptResult.failure(
                    reason = TypeFailureReason.ACTION_FAILED,
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
        }
    }

    fun setTextOnNode(nodeId: String, text: String): SetTextAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return SetTextAttemptResult.failure(
                reason = SetTextFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val dispatchResult = service.setTextOnNode(nodeId, text)
        return when {
            !dispatchResult.nodeFound -> {
                Log.i(LOG_TAG, "event=settext_node nodeId=$nodeId path=${dispatchResult.attemptedPath} success=false")
                SetTextAttemptResult.failure(
                    reason = when (dispatchResult.attemptedPath) {
                        "root_unavailable" -> SetTextFailureReason.ACCESSIBILITY_UNAVAILABLE
                        else -> SetTextFailureReason.NODE_NOT_FOUND
                    },
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
            !dispatchResult.performed -> {
                Log.i(LOG_TAG, "event=settext_node nodeId=$nodeId path=${dispatchResult.attemptedPath} success=false")
                SetTextAttemptResult.failure(
                    reason = when (dispatchResult.attemptedPath) {
                        "node_not_editable" -> SetTextFailureReason.NODE_NOT_EDITABLE
                        else -> SetTextFailureReason.ACTION_FAILED
                    },
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
            else -> {
                Log.i(LOG_TAG, "event=settext_node nodeId=$nodeId path=${dispatchResult.attemptedPath} success=true")
                SetTextAttemptResult.success(attemptedPath = dispatchResult.attemptedPath)
            }
        }
    }

    fun dispatchKey(key: InputKey): InputKeyAttemptResult {
        val service = GhostAccessibilityExecutionCoreRegistry.currentInstance()
            ?: return InputKeyAttemptResult.failure(
                reason = InputKeyFailureReason.ACCESSIBILITY_UNAVAILABLE,
                attemptedPath = "service_missing"
            )

        val dispatchResult = when (key) {
            InputKey.ENTER -> service.performImeEnterAction()
        }
        return when {
            dispatchResult.performed -> {
                Log.i(LOG_TAG, "event=input_key key=${key.wireValue} path=${dispatchResult.attemptedPath} success=true")
                InputKeyAttemptResult.success(attemptedPath = dispatchResult.attemptedPath)
            }
            !dispatchResult.targetFound -> {
                Log.i(LOG_TAG, "event=input_key key=${key.wireValue} path=${dispatchResult.attemptedPath} success=false")
                InputKeyAttemptResult.failure(
                    reason = InputKeyFailureReason.NO_EDITABLE_TARGET,
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
            else -> {
                Log.i(LOG_TAG, "event=input_key key=${key.wireValue} path=${dispatchResult.attemptedPath} success=false")
                InputKeyAttemptResult.failure(
                    reason = InputKeyFailureReason.ACTION_FAILED,
                    attemptedPath = dispatchResult.attemptedPath
                )
            }
        }
    }

    private companion object {
        const val LOG_TAG = "GhostType"
    }
}

data class TypeAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: TypeFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): TypeAttemptResult = TypeAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: TypeFailureReason, attemptedPath: String): TypeAttemptResult =
            TypeAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class TypeFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NO_EDITABLE_TARGET,
    ACTION_FAILED
}

data class SetTextAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: SetTextFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): SetTextAttemptResult = SetTextAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: SetTextFailureReason, attemptedPath: String): SetTextAttemptResult =
            SetTextAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class SetTextFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NODE_NOT_FOUND,
    NODE_NOT_EDITABLE,
    ACTION_FAILED
}

data class InputKeyAttemptResult(
    val performed: Boolean,
    val backendUsed: String?,
    val failureReason: InputKeyFailureReason?,
    val attemptedPath: String
) {
    companion object {
        fun success(attemptedPath: String): InputKeyAttemptResult = InputKeyAttemptResult(
            performed = true,
            backendUsed = "accessibility",
            failureReason = null,
            attemptedPath = attemptedPath
        )

        fun failure(reason: InputKeyFailureReason, attemptedPath: String): InputKeyAttemptResult =
            InputKeyAttemptResult(
                performed = false,
                backendUsed = null,
                failureReason = reason,
                attemptedPath = attemptedPath
            )
    }
}

enum class InputKeyFailureReason {
    ACCESSIBILITY_UNAVAILABLE,
    NO_EDITABLE_TARGET,
    ACTION_FAILED
}
