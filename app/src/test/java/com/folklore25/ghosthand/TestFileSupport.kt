/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

import java.io.File

internal object TestFileSupport {
    fun readProjectFile(vararg candidates: String): String {
        val file = candidates
            .map(::File)
            .firstOrNull(File::exists)
            ?: error("Unable to locate any of: ${candidates.joinToString()}")
        return file.readText()
    }
}
