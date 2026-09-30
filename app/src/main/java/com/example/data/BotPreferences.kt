package com.example.data

import android.content.Context
import android.content.SharedPreferences

class BotPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bot_prefs_secure", Context.MODE_PRIVATE)

    var mainScriptName: String
        get() = prefs.getString("main_script_name", "hrbot_full-31.py") ?: "hrbot_full-31.py"
        set(value) = prefs.edit().putString("main_script_name", value).apply()

    var bootAutoStart: Boolean
        get() = prefs.getBoolean("boot_auto_start", true)
        set(value) = prefs.edit().putBoolean("boot_auto_start", value).apply()

    var autoRestartOnCrash: Boolean
        get() = prefs.getBoolean("auto_restart_on_crash", true)
        set(value) = prefs.edit().putBoolean("auto_restart_on_crash", value).apply()

    var wakeLockEnabled: Boolean
        get() = prefs.getBoolean("wake_lock_enabled", true)
        set(value) = prefs.edit().putBoolean("wake_lock_enabled", value).apply()

    var apiToken: String
        get() = prefs.getString("highrise_api_token", "") ?: ""
        set(value) = prefs.edit().putString("highrise_api_token", value).apply()

    var roomId: String
        get() = prefs.getString("highrise_room_id", "") ?: ""
        set(value) = prefs.edit().putString("highrise_room_id", value).apply()

    var botPrefix: String
        get() = prefs.getString("bot_prefix", "!") ?: "!"
        set(value) = prefs.edit().putString("bot_prefix", value).apply()

    var ownerId: String
        get() = prefs.getString("bot_owner_id", "") ?: ""
        set(value) = prefs.edit().putString("bot_owner_id", value).apply()

    var maskTokensInLogs: Boolean
        get() = prefs.getBoolean("mask_tokens_in_logs", true)
        set(value) = prefs.edit().putBoolean("mask_tokens_in_logs", value).apply()

    var radioStreamUrl: String
        get() = prefs.getString("radio_stream_url", "") ?: ""
        set(value) = prefs.edit().putString("radio_stream_url", value).apply()

    var isServiceRequestedRunning: Boolean
        get() = prefs.getBoolean("service_requested_running", false)
        set(value) = prefs.edit().putBoolean("service_requested_running", value).apply()
}
