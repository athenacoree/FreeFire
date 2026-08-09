package com.jules.gameguard.data

import android.content.Context
import android.content.SharedPreferences

class GameGuardPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gameguard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MODO_JUEGO_ACTIVO = "modo_juego_activo"
        private const val KEY_CONFIGURABLE_SERVER = "configurable_server"
        private const val KEY_LAST_CLOSED_APPS = "last_closed_apps"
        private const val KEY_LAST_SESSION_FEEDBACK = "last_session_feedback"
        private const val DEFAULT_SERVER = "8.8.4.4"
    }

    var isModoJuegoActivo: Boolean
        get() = prefs.getBoolean(KEY_MODO_JUEGO_ACTIVO, false)
        set(value) = prefs.edit().putBoolean(KEY_MODO_JUEGO_ACTIVO, value).apply()

    var configurableServer: String
        get() = prefs.getString(KEY_CONFIGURABLE_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) = prefs.edit().putString(KEY_CONFIGURABLE_SERVER, value).apply()

    var lastClosedApps: String
        get() = prefs.getString(KEY_LAST_CLOSED_APPS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CLOSED_APPS, value).apply()

    var lastSessionFeedback: String
        get() = prefs.getString(KEY_LAST_SESSION_FEEDBACK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SESSION_FEEDBACK, value).apply()
}
