/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.interaction.effects

import com.folklore25.ghosthand.ActionEffectObservation

object ActionEffectPayloads {
    fun fields(effect: ActionEffectObservation): Map<String, Any?> {
        return linkedMapOf(
            "stateChanged" to effect.stateChanged,
            "beforeSnapshotToken" to effect.beforeSnapshotToken,
            "afterSnapshotToken" to effect.afterSnapshotToken,
            "finalPackageName" to effect.finalPackageName,
            "finalActivity" to effect.finalActivity
        )
    }
}
