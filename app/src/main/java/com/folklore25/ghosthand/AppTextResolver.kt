package com.folklore25.ghosthand

import android.content.Context

object AppTextResolver {
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
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
