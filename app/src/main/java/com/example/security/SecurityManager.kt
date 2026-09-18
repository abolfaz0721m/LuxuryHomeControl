package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.local.AppDao
import com.example.data.local.SecurityPreferences
import com.example.data.model.SecurityLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SecurityManager(
    private val context: Context,
    private val appDao: AppDao,
    private val securityPreferences: SecurityPreferences
) {
    /**
     * بررسی وضعیت روت دستگاه (Root Detection)
     */
    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/mu"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    /**
     * اجرای حالت وحشت (Panic Mode)
     * فریز فوری همه اپ‌ها، مخفی‌سازی، و لاگ امنیتی
     */
    fun triggerPanicMode(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            securityPreferences.setPanicTriggered(true)
            appDao.setFreezeStateForIsland("island_main", true)
            appDao.setFreezeStateForIsland("island_secret", true)
            appDao.setFreezeStateForIsland("island_gaming", true)
            appDao.insertLog(
                SecurityLog(
                    eventType = "PANIC_TRIGGERED",
                    details = "حالت اضطراری فعال شد. تمامی اپ‌های جزایر ایزوله فریز و محافظت شدند.",
                    severity = "CRITICAL"
                )
            )
        }
    }

    /**
     * بازیابی از حالت اضطراری
     */
    fun restoreFromPanic(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            securityPreferences.setPanicTriggered(false)
            appDao.insertLog(
                SecurityLog(
                    eventType = "PANIC_RESTORED",
                    details = "سیستم از حالت اضطراری خارج شد.",
                    severity = "INFO"
                )
            )
        }
    }

    /**
     * اجرای خود تخریبی (Self-Destruct) پس از خطاهای مکرر
     */
    fun executeSelfDestruct(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            appDao.clearAllApps()
            appDao.clearLogs()
            securityPreferences.resetFailedAttempts()
            securityPreferences.setPinHash(null)
            securityPreferences.setPanicTriggered(true)
        }
    }

    /**
     * تغییر وضعیت حالت پنهان (Stealth Mode)
     */
    fun toggleStealthMode(enable: Boolean) {
        securityPreferences.setStealthMode(enable)
        // Can conditionally toggle launcher component or flag
    }
}
