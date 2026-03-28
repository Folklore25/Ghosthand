package com.folklore25.ghosthand

class RootControlProvider(
    private val rootBridge: RootBridge = RootBridge()
) {
    fun availability(): RootAvailabilitySnapshot = rootBridge.availability()

    fun launchApp(packageName: String, activity: String?): RootControlResult {
        val availabilityResult = rootBridge.checkAvailability()
        val availability = rootBridge.availability()
        if (availability.available != true) {
            return RootControlResult(
                performed = false,
                failureReason = when {
                    availability.status == "authorization_required" ||
                        (availabilityResult.suPath != null && availabilityResult.exitCode != 0) ->
                        RootControlFailureReason.ROOT_AUTHORIZATION_REQUIRED
                    else -> RootControlFailureReason.ROOT_UNAVAILABLE
                },
                packageName = packageName
            )
        }

        val result = rootBridge.launchApp(packageName, activity)
        return if (result.succeeded) {
            RootControlResult(
                performed = true,
                failureReason = null,
                packageName = packageName
            )
        } else {
            RootControlResult(
                performed = false,
                failureReason = RootControlFailureReason.ACTION_FAILED,
                packageName = packageName
            )
        }
    }

    fun stopApp(packageName: String): RootControlResult {
        val availabilityResult = rootBridge.checkAvailability()
        val availability = rootBridge.availability()
        if (availability.available != true) {
            return RootControlResult(
                performed = false,
                failureReason = when {
                    availability.status == "authorization_required" ||
                        (availabilityResult.suPath != null && availabilityResult.exitCode != 0) ->
                        RootControlFailureReason.ROOT_AUTHORIZATION_REQUIRED
                    else -> RootControlFailureReason.ROOT_UNAVAILABLE
                },
                packageName = packageName
            )
        }

        val result = rootBridge.stopApp(packageName)
        return if (result.succeeded) {
            RootControlResult(
                performed = true,
                failureReason = null,
                packageName = packageName
            )
        } else {
            RootControlResult(
                performed = false,
                failureReason = RootControlFailureReason.ACTION_FAILED,
                packageName = packageName
            )
        }
    }
}

data class RootControlResult(
    val performed: Boolean,
    val failureReason: RootControlFailureReason?,
    val packageName: String
)

enum class RootControlFailureReason {
    ROOT_UNAVAILABLE,
    ROOT_AUTHORIZATION_REQUIRED,
    ACTION_FAILED
}
