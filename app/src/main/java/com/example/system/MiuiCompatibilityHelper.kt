package com.example.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * مدیریت سازگاری اختصاصی با MIUI، HyperOS و گوشی‌های POCO (مخصوصاً POCO X4 Pro 5G)
 */
object MiuiCompatibilityHelper {

    /**
     * بررسی آیا دستگاه از برندهای شیائومی، ردمی یا پوکو است
     */
    fun isXiaomiOrPoco(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("poco") || brand.contains("redmi")
    }

    /**
     * نام دستگاه و نسخه رابط کاربری
     */
    fun getDeviceSummary(): String {
        val model = Build.MODEL
        val miuiVersion = getSystemProperty("ro.miui.ui.version.name") ?: "Standard Android"
        return "دستگاه: $model | سیستم: MIUI / HyperOS ($miuiVersion)"
    }

    /**
     * استخراج مشخصه‌های سیستمی MIUI با Reflection
     */
    @SuppressLint("PrivateApi")
    fun getSystemProperty(propName: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val getMethod = c.getMethod("get", String::class.java)
            val result = getMethod.invoke(c, propName) as? String
            if (result.isNullOrBlank()) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * بررسی فعال بودن MIUI Optimization
     * بهینه‌سازی MIUI باعث تداخل در ایجاد Work Profile و Dual Apps می‌شود
     */
    fun isMiuiOptimizationEnabled(): Boolean {
        val value = getSystemProperty("persist.sys.miui_optimization")
        return value == null || value == "true" || value == "1"
    }

    /**
     * بررسی اینکه آیا اپلیکیشن از بهینه‌سازی باتری معاف است؟
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * ساخت Intent برای درخواست معافیت از محدودیت باتری (جلوگیری از Kill شدن پس‌زمینه توسط MIUI)
     */
    @SuppressLint("BatteryLife")
    fun getBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * باز کردن صفحه مدیریت شروع خودکار (Autostart) در مرکز امنیت MIUI
     */
    fun openMiuiAutostart(context: Context): Boolean {
        val intents = arrayOf(
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.miui.securitycenter", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Continue to next intent
            }
        }
        return false
    }

    /**
     * باز کردن تنظیمات Developer Options برای خاموش کردن MIUI Optimization
     */
    fun openDeveloperOptions(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * دستورالعمل‌های گام‌به‌گام رفع مشکلات MIUI
     */
    fun getMiuiOptimizationGuide(): List<String> = listOf(
        "۱. به «تنظیمات گوشی» > «درباره گوشی (About Phone)» رفته و ۷ بار روی «MIUI version» بزنید تا گزینه‌های توسعه‌دهنده فعال شود.",
        "۲. به «تنظیمات بیشتر (Additional Settings)» > «گزینه‌های توسعه‌دهنده (Developer Options)» بروید.",
        "۳. در انتهای صفحه، گزینه «بهینه‌سازی MIUI (MIUI Optimization)» را بیابید و آن را خاموش (Turn Off) کنید.",
        "۴. همچنین «اشکال‌زدایی USB (تنظیمات امنیتی)» را در صورت نیاز به ADB فعال فرمایید.",
        "۵. در بخش باتری و امنیت، اجازه «شروع خودکار (Autostart)» را به Island Pro Max بدهید."
    )
}
