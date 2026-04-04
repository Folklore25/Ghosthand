/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

internal fun findEditableInputFocusNode(
    service: AccessibilityService,
    activeRootProvider: () -> AccessibilityNodeInfo?
): AccessibilityNodeInfo? {
    service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        ?.takeIf(::isEditableTarget)
        ?.let { return it }

    service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        ?.takeIf(::isEditableTarget)
        ?.let { return it }

    return activeRootProvider()?.let(::findFocusedEditableNode)
}

internal fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    if (isEditableTarget(node) && (node.isFocused || node.isAccessibilityFocused)) {
        return node
    }

    for (childIndex in 0 until node.childCount) {
        val child = node.getChild(childIndex) ?: continue
        val match = findFocusedEditableNode(child)
        if (match != null) {
            return match
        }
    }
    return null
}

internal fun isEditableTarget(node: AccessibilityNodeInfo): Boolean {
    return node.isEditable && node.isEnabled
}

internal fun findClickableParent(
    node: AccessibilityNodeInfo,
    maxDepth: Int
): AccessibilityNodeInfo? {
    var depth = 0
    var current = node.parent
    while (current != null && depth < maxDepth) {
        if (current.isClickable && current.isEnabled) {
            return current
        }
        current = current.parent
        depth += 1
    }
    return null
}
