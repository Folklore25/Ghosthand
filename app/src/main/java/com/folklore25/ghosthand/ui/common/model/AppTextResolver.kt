/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.ui.common.model

import com.folklore25.ghosthand.R

import android.content.Context

object AppTextResolver {
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context
    }

    fun getString(resId: Int): String {
        val context = appContext ?: error("AppTextResolver not initialized")
        return context.getString(resId)
    }

    fun getString(resId: Int, vararg args: Any): String {
        val context = appContext ?: error("AppTextResolver not initialized")
        return context.getString(resId, *args)
    }
}
