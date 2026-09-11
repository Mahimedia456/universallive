package com.universallive.app.push

import android.content.Context

object PushTokenStore {
    private const val PREFS = "universallive_push"
    private const val KEY_TOKEN = "fcm_token"

    fun save(context: Context, token: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun read(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
}
