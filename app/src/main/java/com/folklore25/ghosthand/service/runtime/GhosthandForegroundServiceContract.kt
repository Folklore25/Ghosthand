/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.runtime

import android.content.pm.ServiceInfo

object GhosthandForegroundServiceContract {
    const val MANIFEST_FOREGROUND_SERVICE_TYPES = "dataSync"

    val STARTUP_FOREGROUND_SERVICE_TYPES: Int =
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC

    val MEDIA_PROJECTION_FOREGROUND_SERVICE_TYPES: Int =
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
}
