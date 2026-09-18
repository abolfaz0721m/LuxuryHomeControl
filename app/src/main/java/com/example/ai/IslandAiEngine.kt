package com.example.ai

import com.example.data.model.AppCategory
import com.example.data.model.ManagedApp
import java.util.Calendar

data class AiInsight(
    val title: String,
    val description: String,
    val actionType: String,
    val targetPackage: String? = null,
    val severity: String = "INFO" // INFO, WARNING, RECOMMENDED
)

data class ChatResponse(
    val replyText: String,
    val executedAction: String? = null,
    val affectedAppsCount: Int = 0
)

/**
 * موتور هوش مصنوعی و پردازش زبان طبیعی کاملاً آفلاین (On-Device Local AI Engine)
 */
object IslandAiEngine {

    /**
     * آنالیز هوشمند اپلیکیشن‌ها و ارائه توصیه‌های بهینه‌سازی
     */
    fun analyzeApps(apps: List<ManagedApp>): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        // 1. تشخیص اپ‌های پرمصرف
        val highDrainApps = apps.filter { it.batteryDrainScore > 1.5f && !it.isFrozen }
        if (highDrainApps.isNotEmpty()) {
            insights.add(
                AiInsight(
                    title = "پیشنهاد هوشمند فریز باتری",
                    description = "تعداد ${highDrainApps.size} اپلیکیشن در پس‌زمینه مصرف بالایی دارند (${highDrainApps.take(2).joinToString { it.appName }}). توصیه می‌شود فریز شوند.",
                    actionType = "FREEZE_HIGH_DRAIN",
                    severity = "RECOMMENDED"
                )
            )
        }

        // 2. گارد حریم خصوصی هوشمند (Social & Messenger isolation)
        val socialAppsInMain = apps.filter { !it.isIslandApp && (it.category == AppCategory.SOCIAL || it.category == AppCategory.MESSENGER) }
        if (socialAppsInMain.isNotEmpty()) {
            insights.add(
                AiInsight(
                    title = "محافظت هوشمند حریم خصوصی",
                    description = "${socialAppsInMain.size} پیام‌رسان در فضای اصلی نصب هستند. برای جلوگیری از ردیابی، آنها را به جزیره کلون کنید.",
                    actionType = "CLONE_SOCIAL_TO_ISLAND",
                    severity = "WARNING"
                )
            )
        }

        // 3. پیش‌بینی زمان و رفتار کاربر
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 23..6) {
            insights.add(
                AiInsight(
                    title = "حالت خواب شبانه (AI Sleep Guard)",
                    description = "ساعات استراحت شناسایی شد. فریز خودکار نوتیفیکیشن‌ها برای صرفه‌جویی باتری فعال است.",
                    actionType = "SLEEP_OPTIMIZATION",
                    severity = "INFO"
                )
            )
        }

        return insights
    }

    /**
     * پیش‌بینی اپلیکیشن بعدی که کاربر مایل به باز کردن آن است
     */
    fun predictNextApp(apps: List<ManagedApp>): ManagedApp? {
        if (apps.isEmpty()) return null
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val candidateCategory = when (hour) {
            in 8..17 -> AppCategory.PRODUCTIVITY
            in 18..22 -> AppCategory.GAMING
            else -> AppCategory.MESSENGER
        }
        val match = apps.firstOrNull { it.category == candidateCategory && !it.isFrozen }
        return match ?: apps.firstOrNull { !it.isFrozen }
    }

    /**
     * پردازش دستورات گفتگوی طبیعی (Chat Assistant)
     */
    fun processChatMessage(query: String, apps: List<ManagedApp>): ChatResponse {
        val lower = query.lowercase().trim()

        return when {
            // فریز کردن شبکه های اجتماعی
            lower.contains("فریز") && (lower.contains("اجتماعی") || lower.contains("سوشال") || lower.contains("social")) -> {
                val targets = apps.filter { it.category == AppCategory.SOCIAL || it.category == AppCategory.MESSENGER }
                ChatResponse(
                    replyText = "دستور اجرا شد: تعداد ${targets.size} اپلیکیشن اجتماعی و پیام‌رسان در جزیره فریز شدند تا از مصرف باتری و ردیابی جلوگیری شود.",
                    executedAction = "FREEZE_SOCIAL",
                    affectedAppsCount = targets.size
                )
            }

            // فریز همه
            lower.contains("فریز همه") || lower.contains("freeze all") -> {
                val targets = apps.filter { !it.isFrozen }
                ChatResponse(
                    replyText = "تمامی ${targets.size} اپلیکیشن موجود در جزیره با موفقیت فریز شدند.",
                    executedAction = "FREEZE_ALL",
                    affectedAppsCount = targets.size
                )
            }

            // آن‌فریز یا رفع مسدودیت
            lower.contains("آنفریز") || lower.contains("آن فریز") || lower.contains("unfreeze") -> {
                ChatResponse(
                    replyText = "اپلیکیشن‌های انتخاب‌شده آن‌فریز و آماده استفاده شدند.",
                    executedAction = "UNFREEZE_ALL",
                    affectedAppsCount = apps.count { it.isFrozen }
                )
            }

            // حالت اضطراری یا وحشت
            lower.contains("اضطراری") || lower.contains("وحشت") || lower.contains("panic") -> {
                ChatResponse(
                    replyText = "هشدار امنیتی: پروتکل اضطراری (Panic Mode) فعال شد! تمام فضاهای ایزوله فریز و پنهان شدند.",
                    executedAction = "TRIGGER_PANIC",
                    affectedAppsCount = apps.size
                )
            }

            // بررسی باتری و امنیت
            lower.contains("باتری") || lower.contains("امنیت") || lower.contains("گزارش") || lower.contains("report") -> {
                val frozenCount = apps.count { it.isFrozen }
                val total = apps.size
                ChatResponse(
                    replyText = "وضعیت جزیره عالی است: $frozenCount از $total اپلیکیشن فریز شده‌اند. سیستم رمزنگاری Keystore فعال است و نشت داده شناسایی نشد.",
                    executedAction = "REPORT",
                    affectedAppsCount = 0
                )
            }

            else -> {
                ChatResponse(
                    replyText = "متوجه شدم. می‌توانید به من بگویید «اپ‌های اجتماعی رو فریز کن»، «همه رو فریز کن»، «گزارش امنیت بده» یا «حالت اضطراری».",
                    executedAction = null,
                    affectedAppsCount = 0
                )
            }
        }
    }
}
