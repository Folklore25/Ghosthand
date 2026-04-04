/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service

import com.folklore25.ghosthand.TestFileSupport
import com.folklore25.ghosthand.service.runtime.GhosthandForegroundServiceContract
import org.junit.Assert.assertTrue
import org.junit.Test

class GhosthandForegroundServiceContractTest {
    @Test
    fun manifestAndRuntimeStartupUseTheBaseForegroundServiceType() {
        val manifest = TestFileSupport.readProjectFile(
            "app/src/main/AndroidManifest.xml",
            "src/main/AndroidManifest.xml"
        )
        val foregroundService = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/service/runtime/GhosthandForegroundService.kt",
            "src/main/java/com/folklore25/ghosthand/service/runtime/GhosthandForegroundService.kt"
        )

        assertTrue(
            manifest.contains(
                "android:foregroundServiceType=\"${GhosthandForegroundServiceContract.MANIFEST_FOREGROUND_SERVICE_TYPES}\""
            )
        )
        assertTrue(
            foregroundService.contains("GhosthandForegroundServiceContract.STARTUP_FOREGROUND_SERVICE_TYPES")
        )
    }
}
