/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand

internal enum class ModuleExplanation(
    val titleRes: Int,
    val bodyRes: Int
) {
    Runtime(R.string.explanation_runtime_title, R.string.explanation_runtime_body),
    Permissions(R.string.explanation_permissions_title, R.string.explanation_permissions_body),
    Accessibility(R.string.explanation_accessibility_title, R.string.explanation_accessibility_body),
    Screenshot(R.string.explanation_screenshot_title, R.string.explanation_screenshot_body),
    Diagnostics(R.string.explanation_diagnostics_title, R.string.explanation_diagnostics_body)
}
