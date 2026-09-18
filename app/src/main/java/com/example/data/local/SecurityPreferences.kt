package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * مدیریت تنظیمات امنیتی محلی و حالت‌های جزیره
 */
class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_security_prefs", Context.MODE_PRIVATE)

    private val _isPanicTriggered = MutableStateFlow(prefs.getBoolean(KEY_PANIC, false))
    val isPanicTriggered: StateFlow<Boolean> = _isPanicTriggered

    private val _isStealthMode = MutableStateFlow(prefs.getBoolean(KEY_STEALTH, false))
    val isStealthMode: StateFlow<Boolean> = _isStealthMode

    private val _isBiometricLock = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC, false))
    val isBiometricLock: StateFlow<Boolean> = _isBiometricLock

    private val _activeIslandId = MutableStateFlow(prefs.getString(KEY_ACTIVE_ISLAND, "island_work") ?: "island_work")
    val activeIslandId: StateFlow<String> = _activeIslandId

    private val _isGameBooster = MutableStateFlow(prefs.getBoolean(KEY_GAME_BOOSTER, false))
    val isGameBooster: StateFlow<Boolean> = _isGameBooster

    private val _isVpnActive = MutableStateFlow(prefs.getBoolean(KEY_VPN_ACTIVE, false))
    val isVpnActive: StateFlow<Boolean> = _isVpnActive

    fun setPanicTriggered(triggered: Boolean) {
        prefs.edit().putBoolean(KEY_PANIC, triggered).apply()
        _isPanicTriggered.value = triggered
    }

    fun setStealthMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STEALTH, enabled).apply()
        _isStealthMode.value = enabled
    }

    fun setBiometricLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _isBiometricLock.value = enabled
    }

    fun setActiveIsland(islandId: String) {
        prefs.edit().putString(KEY_ACTIVE_ISLAND, islandId).apply()
        _activeIslandId.value = islandId
    }

    fun setGameBooster(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_BOOSTER, enabled).apply()
        _isGameBooster.value = enabled
    }

    fun setVpnActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_VPN_ACTIVE, active).apply()
        _isVpnActive.value = active
    }

    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun incrementFailedAttempts(): Int {
        val attempts = getFailedAttempts() + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        return attempts
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)

    fun setPinHash(hash: String?) {
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    companion object {
        private const val KEY_PANIC = "pref_panic"
        private const val KEY_STEALTH = "pref_stealth"
        private const val KEY_BIOMETRIC = "pref_biometric"
        private const val KEY_ACTIVE_ISLAND = "pref_active_island"
        private const val KEY_GAME_BOOSTER = "pref_game_booster"
        private const val KEY_VPN_ACTIVE = "pref_vpn_active"
        private const val KEY_FAILED_ATTEMPTS = "pref_failed_attempts"
        private const val KEY_PIN_HASH = "pref_pin_hash"
    }
}
