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

        // New properties
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_PING_ALERT_THRESHOLD = "ping_alert_threshold"
        private const val KEY_PING_VIBRATE_ALERT_ENABLED = "ping_vibrate_alert_enabled"
        private const val KEY_PING_SOUND_ALERT_ENABLED = "ping_sound_alert_enabled"
        private const val KEY_AUTO_RAM_CLEAN_INTERVAL = "auto_ram_clean_interval"
        private const val KEY_IS_ONBOARDING_COMPLETED = "is_onboarding_completed"
        private const val KEY_TOTAL_RAM_CLEANED_MB = "total_ram_cleaned_mb"

        // VPN & Advanced optimization preferences
        private const val KEY_EXCLUSIVE_APP_PACKAGE = "exclusive_app_package"
        private const val KEY_EXCLUSIVE_CONNECTION_ENABLED = "exclusive_connection_enabled"
        private const val KEY_ACTIVE_PERFORMANCE_PROFILE = "active_performance_profile" // "BATTERY", "BALANCED", "ULTRA_GAMING"
        private const val KEY_DNS_OPTIMIZATION_ENABLED = "dns_optimization_enabled"
        private const val KEY_DNS_PROVIDER = "dns_provider" // "CLOUDFLARE", "GOOGLE", "ADGUARD"
        private const val KEY_CALL_BLOCKING_ENABLED = "call_blocking_enabled"

        // AI Engine preferences
        private const val KEY_AI_AUTO_OPTIMIZATION_ENABLED = "ai_auto_optimization_enabled"
        private const val KEY_AI_SENSITIVITY = "ai_sensitivity" // "CONSERVATIVE", "BALANCED", "AGGRESSIVE"
    }

    var isAiAutoOptimizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_AUTO_OPTIMIZATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_AUTO_OPTIMIZATION_ENABLED, value).apply()

    var aiSensitivity: String
        get() = prefs.getString(KEY_AI_SENSITIVITY, "BALANCED") ?: "BALANCED"
        set(value) = prefs.edit().putString(KEY_AI_SENSITIVITY, value).apply()

    var isCallBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_BLOCKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CALL_BLOCKING_ENABLED, value).apply()

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

    // Getters and setters for new preferences
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    var accentColor: String
        get() = prefs.getString(KEY_ACCENT_COLOR, "CYAN") ?: "CYAN"
        set(value) = prefs.edit().putString(KEY_ACCENT_COLOR, value).apply()

    var pingAlertThresholdMs: Int
        get() = prefs.getInt(KEY_PING_ALERT_THRESHOLD, 100)
        set(value) = prefs.edit().putInt(KEY_PING_ALERT_THRESHOLD, value).apply()

    var isPingVibrateAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_PING_VIBRATE_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PING_VIBRATE_ALERT_ENABLED, value).apply()

    var isPingSoundAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_PING_SOUND_ALERT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PING_SOUND_ALERT_ENABLED, value).apply()

    var autoRamCleanIntervalMins: Int
        get() = prefs.getInt(KEY_AUTO_RAM_CLEAN_INTERVAL, 0) // 0 means disabled
        set(value) = prefs.edit().putInt(KEY_AUTO_RAM_CLEAN_INTERVAL, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDING_COMPLETED, value).apply()

    var totalRamCleanedMb: Long
        get() = prefs.getLong(KEY_TOTAL_RAM_CLEANED_MB, 0L)
        set(value) = prefs.edit().putLong(KEY_TOTAL_RAM_CLEANED_MB, value).apply()

    var exclusiveAppPackage: String
        get() = prefs.getString(KEY_EXCLUSIVE_APP_PACKAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXCLUSIVE_APP_PACKAGE, value).apply()

    var isExclusiveConnectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXCLUSIVE_CONNECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_EXCLUSIVE_CONNECTION_ENABLED, value).apply()

    var activePerformanceProfile: String
        get() = prefs.getString(KEY_ACTIVE_PERFORMANCE_PROFILE, "ULTRA_GAMING") ?: "ULTRA_GAMING"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PERFORMANCE_PROFILE, value).apply()

    var isDnsOptimizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_DNS_OPTIMIZATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DNS_OPTIMIZATION_ENABLED, value).apply()

    var dnsProvider: String
        get() = prefs.getString(KEY_DNS_PROVIDER, "CLOUDFLARE") ?: "CLOUDFLARE"
        set(value) = prefs.edit().putString(KEY_DNS_PROVIDER, value).apply()
}
