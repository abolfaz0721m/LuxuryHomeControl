package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiInsight
import com.example.ai.IslandAiEngine
import com.example.data.local.IslandDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.model.IslandProfile
import com.example.data.model.ManagedApp
import com.example.data.model.SecurityLog
import com.example.security.CryptoEngine
import com.example.security.SecurityManager
import com.example.system.IslandPolicyManager
import com.example.system.MiuiCompatibilityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = IslandDatabase.getInstance(application)
    private val dao = db.appDao()
    val preferences = SecurityPreferences(application)
    val policyManager = IslandPolicyManager(application)
    val securityManager = SecurityManager(application, dao, preferences)

    // Flow states from DB
    val profiles: StateFlow<List<IslandProfile>> = dao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val islandApps: StateFlow<List<ManagedApp>> = dao.getIslandApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalApps: StateFlow<List<ManagedApp>> = dao.getPersonalApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs: StateFlow<List<SecurityLog>> = dao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPanicTriggered = preferences.isPanicTriggered
    val isStealthMode = preferences.isStealthMode
    val isGameBooster = preferences.isGameBooster
    val isVpnActive = preferences.isVpnActive
    val activeIslandId = preferences.activeIslandId

    // UI Feedback state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // AI Insights State
    private val _aiInsights = MutableStateFlow<List<AiInsight>>(emptyList())
    val aiInsights: StateFlow<List<AiInsight>> = _aiInsights.asStateFlow()

    // AI Chat history: list of Pair(MessageText, isUser)
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            Pair("سلام! من دستیار هوشمند محلی Island Pro Max هستم. برای مدیریت خودکار جزیره‌ها، فریز اپ‌ها یا تست امنیت در خدمتم.", false)
        )
    )
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    // Device & MIUI status
    val isXiaomiOrPoco = MiuiCompatibilityHelper.isXiaomiOrPoco()
    val miuiSummary = MiuiCompatibilityHelper.getDeviceSummary()
    val isMiuiOptimizationActive = MiuiCompatibilityHelper.isMiuiOptimizationEnabled()
    val isIgnoringBatteryOptimizations = MiuiCompatibilityHelper.isIgnoringBatteryOptimizations(application)
    val isRooted = securityManager.isDeviceRooted()

    init {
        loadInitialAppsIfEmpty()
    }

    private fun loadInitialAppsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val installedPersonal = policyManager.loadInstalledApps(forIsland = false)
            val installedIsland = policyManager.loadInstalledApps(forIsland = true)

            if (installedPersonal.isNotEmpty()) {
                dao.insertApps(installedPersonal)
            }
            if (installedIsland.isNotEmpty()) {
                dao.insertApps(installedIsland)
            }

            // Generate initial AI Insights
            val currentIslandList = if (installedIsland.isNotEmpty()) installedIsland else installedPersonal
            _aiInsights.value = IslandAiEngine.analyzeApps(currentIslandList)
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showToast(msg: String) {
        _userMessage.value = msg
    }

    fun switchActiveIsland(islandId: String) {
        preferences.setActiveIsland(islandId)
        showToast("فضای کاری به $islandId تغییر یافت")
    }

    /**
     * فریز یا آن‌فریز اپلیکیشن
     */
    fun toggleAppFreeze(app: ManagedApp) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFrozenState = !app.isFrozen
            policyManager.freezePackage(app.packageName, newFrozenState)
            dao.setAppFreeze(app.uniqueId, newFrozenState)
            dao.insertLog(
                SecurityLog(
                    eventType = if (newFrozenState) "APP_FROZEN" else "APP_UNFROZEN",
                    details = "اپلیکیشن ${app.appName} (${app.packageName}) ${if (newFrozenState) "فریز" else "آن‌فریز"} شد."
                )
            )
            // Update AI Insights
            val updated = islandApps.value.map { if (it.uniqueId == app.uniqueId) it.copy(isFrozen = newFrozenState) else it }
            _aiInsights.value = IslandAiEngine.analyzeApps(updated)
        }
    }

    /**
     * فریز دسته‌جمعی تمامی اپ‌های جزیره
     */
    fun freezeAllIslandApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = islandApps.value
            for (app in apps) {
                policyManager.freezePackage(app.packageName, true)
            }
            dao.setFreezeStateForIsland("island_main", true)
            dao.insertLog(
                SecurityLog(
                    eventType = "BATCH_FREEZE",
                    details = "تمامی اپلیکیشن‌های جزیره به صورت گروهی فریز شدند."
                )
            )
            showToast("تمامی اپلیکیشن‌های جزیره فریز شدند")
        }
    }

    /**
     * رفع مسدودیت و آن‌فریز دسته‌جمعی
     */
    fun unfreezeAllIslandApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = islandApps.value
            for (app in apps) {
                policyManager.freezePackage(app.packageName, false)
            }
            dao.setFreezeStateForIsland("island_main", false)
            dao.insertLog(
                SecurityLog(
                    eventType = "BATCH_UNFREEZE",
                    details = "تمامی اپلیکیشن‌های جزیره فعال شدند."
                )
            )
            showToast("تمامی اپلیکیشن‌ها آن‌فریز شدند")
        }
    }

    /**
     * کلون کردن یک اپلیکیشن از فضای شخصی به جزیره کاری
     */
    fun cloneAppToIsland(personalApp: ManagedApp) {
        viewModelScope.launch(Dispatchers.IO) {
            val clonedApp = personalApp.copy(
                uniqueId = "${personalApp.packageName}_island_main_${System.currentTimeMillis() % 1000}",
                islandId = "island_main",
                isIslandApp = true,
                isCloned = true,
                cloneIndex = 1,
                fakeAndroidId = CryptoEngine.generateVirtualAndroidId(),
                fakeMac = CryptoEngine.generateVirtualMac(),
                isFrozen = false
            )
            dao.insertApp(clonedApp)
            dao.insertLog(
                SecurityLog(
                    eventType = "APP_CLONED",
                    details = "اپلیکیشن ${personalApp.appName} با شناسه مجازی به جزیره کلون شد."
                )
            )
            showToast("اپلیکیشن ${personalApp.appName} به جزیره کلون شد")
        }
    }

    /**
     * حذف کامل جزیره (Destroy Island)
     */
    fun destroyIsland() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = policyManager.destroyWorkProfile()
            dao.deleteAppsByIsland("island_main")
            dao.insertLog(
                SecurityLog(
                    eventType = "ISLAND_DESTROYED",
                    details = "جزیره اصلی با تمامی اطلاعات ایزوله شده به طور کامل نابود شد.",
                    severity = "WARNING"
                )
            )
            if (success) {
                showToast("جزیره با موفقیت نابود شد")
            } else {
                showToast("برای حذف کامل Work Profile، آن را در صفحه تنظیمات لغو کنید")
            }
        }
    }

    /**
     * فعال‌سازی حالت اضطراری (Panic)
     */
    fun triggerPanic() {
        securityManager.triggerPanicMode(viewModelScope)
        showToast("حالت اضطراری فعال شد! تمام جزیره‌ها قفل شدند.")
    }

    /**
     * غیرفعال‌سازی حالت اضطراری
     */
    fun restoreFromPanic() {
        securityManager.restoreFromPanic(viewModelScope)
        showToast("سیستم از حالت اضطراری خارج شد.")
    }

    /**
     * روشن/خاموش حالت مخفی (Stealth Mode)
     */
    fun toggleStealth(enable: Boolean) {
        securityManager.toggleStealthMode(enable)
        showToast(if (enable) "حالت پنهان فعال شد (*#*#1337#*#* در دایلر)" else "حالت پنهان غیرفعال شد")
    }

    /**
     * روشن/خاموش بوستر بازی (Game Booster)
     */
    fun toggleGameBooster(enable: Boolean) {
        preferences.setGameBooster(enable)
        if (enable) {
            viewModelScope.launch(Dispatchers.IO) {
                // Freeze background apps except gaming
                val nonGaming = islandApps.value.filter { it.category != com.example.data.model.AppCategory.GAMING }
                for (app in nonGaming) {
                    dao.setAppFreeze(app.uniqueId, true)
                }
            }
            showToast("Game Booster فعال شد: افزایش فریم‌ریت و فریز اپ‌های پس‌زمینه")
        } else {
            showToast("Game Booster غیرفعال شد")
        }
    }

    /**
     * تغییر وضعیت VPN ایزوله محلی
     */
    fun toggleVpn(enable: Boolean) {
        preferences.setVpnActive(enable)
        showToast(if (enable) "VPN ایزوله برای اپ‌های جزیره فعال شد" else "VPN ایزوله خاموش شد")
    }

    /**
     * ارسال پیام به چت هوش مصنوعی محلی
     */
    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        val currentHistory = _chatHistory.value.toMutableList()
        currentHistory.add(Pair(query, true))
        _chatHistory.value = currentHistory

        viewModelScope.launch(Dispatchers.Default) {
            val response = IslandAiEngine.processChatMessage(query, islandApps.value)
            // Execute requested action if any
            when (response.executedAction) {
                "FREEZE_SOCIAL" -> {
                    val socials = islandApps.value.filter { it.category == com.example.data.model.AppCategory.SOCIAL || it.category == com.example.data.model.AppCategory.MESSENGER }
                    for (app in socials) {
                        dao.setAppFreeze(app.uniqueId, true)
                    }
                }
                "FREEZE_ALL" -> freezeAllIslandApps()
                "UNFREEZE_ALL" -> unfreezeAllIslandApps()
                "TRIGGER_PANIC" -> triggerPanic()
            }

            val updatedHistory = _chatHistory.value.toMutableList()
            updatedHistory.add(Pair(response.replyText, false))
            _chatHistory.value = updatedHistory
        }
    }

    /**
     * خروجی بکاپ رمزنگاری شده محلی
     */
    fun createEncryptedBackup(): String {
        return try {
            val json = JSONObject()
            val appsArray = JSONArray()
            for (app in islandApps.value) {
                val appObj = JSONObject().apply {
                    put("packageName", app.packageName)
                    put("appName", app.appName)
                    put("islandId", app.islandId)
                    put("fakeAndroidId", app.fakeAndroidId)
                }
                appsArray.put(appObj)
            }
            json.put("apps", appsArray)
            json.put("timestamp", System.currentTimeMillis())
            json.put("version", "IslandProMax_6.4.2_Plus")

            val rawString = json.toString()
            val encrypted = CryptoEngine.encrypt(rawString)
            android.util.Base64.encodeToString(encrypted.first + encrypted.second, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
}
