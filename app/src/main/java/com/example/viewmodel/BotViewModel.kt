package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BotFileManager
import com.example.data.BotPreferences
import com.example.data.PipPackageManager
import com.example.engine.HighriseBotEngine
import com.example.model.BotFileItem
import com.example.model.BotStatus
import com.example.model.LogItem
import com.example.model.LogLevel
import com.example.model.PythonPackageItem
import com.example.service.BotForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BotViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    val prefs = BotPreferences(context)
    val fileManager = BotFileManager(context, prefs)
    private val db = AppDatabase.getInstance(context)
    val packageManager = PipPackageManager(context, db, fileManager)
    val engine: HighriseBotEngine = BotForegroundService.getOrCreateEngine(context)

    val botStatus: StateFlow<BotStatus> = engine.status
    val logs: StateFlow<List<LogItem>> = engine.logs
    val uptimeSeconds: StateFlow<Long> = engine.uptimeSeconds
    val messageCount: StateFlow<Int> = engine.messageCount
    val currentBackoff: StateFlow<Long> = engine.currentBackoff
    val lastHeartbeat: StateFlow<Long> = engine.lastHeartbeat

    private val _files = MutableStateFlow<List<BotFileItem>>(emptyList())
    val files: StateFlow<List<BotFileItem>> = _files.asStateFlow()

    private val _selectedFileForEdit = MutableStateFlow<BotFileItem?>(null)
    val selectedFileForEdit: StateFlow<BotFileItem?> = _selectedFileForEdit.asStateFlow()

    private val _editingContent = MutableStateFlow("")
    val editingContent: StateFlow<String> = _editingContent.asStateFlow()

    val installedPackages: StateFlow<List<PythonPackageItem>> = packageManager.packagesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _pipLogEntries = MutableStateFlow<List<String>>(emptyList())
    val pipLogEntries: StateFlow<List<String>> = _pipLogEntries.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _installProgress = MutableStateFlow(0f)
    val installProgress: StateFlow<Float> = _installProgress.asStateFlow()

    private val _installStatusText = MutableStateFlow("")
    val installStatusText: StateFlow<String> = _installStatusText.asStateFlow()

    private val _scanDiscovered = MutableStateFlow<List<String>>(emptyList())
    val scanDiscovered: StateFlow<List<String>> = _scanDiscovered.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Settings State
    private val _bootAutoStart = MutableStateFlow(prefs.bootAutoStart)
    val bootAutoStart: StateFlow<Boolean> = _bootAutoStart.asStateFlow()

    private val _autoRestartOnCrash = MutableStateFlow(prefs.autoRestartOnCrash)
    val autoRestartOnCrash: StateFlow<Boolean> = _autoRestartOnCrash.asStateFlow()

    private val _wakeLockEnabled = MutableStateFlow(prefs.wakeLockEnabled)
    val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled.asStateFlow()

    private val _apiToken = MutableStateFlow(prefs.apiToken)
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _roomId = MutableStateFlow(prefs.roomId)
    val roomId: StateFlow<String> = _roomId.asStateFlow()

    private val _botPrefix = MutableStateFlow(prefs.botPrefix)
    val botPrefix: StateFlow<String> = _botPrefix.asStateFlow()

    private val _ownerId = MutableStateFlow(prefs.ownerId)
    val ownerId: StateFlow<String> = _ownerId.asStateFlow()

    private val _maskTokensInLogs = MutableStateFlow(prefs.maskTokensInLogs)
    val maskTokensInLogs: StateFlow<Boolean> = _maskTokensInLogs.asStateFlow()

    init {
        viewModelScope.launch {
            fileManager.ensureDefaultFiles()
            refreshFiles()
            packageManager.initializeDefaultPackages()
        }

        viewModelScope.launch {
            packageManager.pipLogs.collect { logLine ->
                val list = _pipLogEntries.value.toMutableList()
                list.add(logLine)
                _pipLogEntries.value = list
            }
        }
    }

    fun startBot() {
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopBot() {
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun restartBot() {
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_RESTART
        }
        context.startService(intent)
    }

    fun triggerCrashForTesting() {
        engine.triggerCrashForTesting()
    }

    fun sendTerminalCommand(input: String) {
        engine.sendCustomCommand(input)
    }

    fun clearLogs() {
        engine.clearLogs()
    }

    fun getFormattedLogs(): String = engine.getFormattedLogs()

    fun refreshFiles() {
        viewModelScope.launch {
            _files.value = fileManager.listFiles()
        }
    }

    fun importFile(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, name) = fileManager.importFromUri(uri)
            if (success) {
                refreshFiles()
                engine.addLog(LogLevel.SUCCESS, "FileManager", "📥 File imported: '$name'")
            } else {
                engine.addLog(LogLevel.ERROR, "FileManager", "❌ Failed to import: $name")
            }
            onResult(success, name)
        }
    }

    fun openFileForEdit(file: BotFileItem) {
        viewModelScope.launch {
            val content = fileManager.readFileContent(file.name)
            _selectedFileForEdit.value = file
            _editingContent.value = content
        }
    }

    fun updateEditingContent(newContent: String) {
        _editingContent.value = newContent
    }

    fun saveEditingContent(onSaved: (Boolean) -> Unit) {
        val currentFile = _selectedFileForEdit.value ?: return
        viewModelScope.launch {
            val ok = fileManager.saveFileContent(currentFile.name, _editingContent.value)
            if (ok) {
                engine.addLog(LogLevel.INFO, "FileManager", "💾 Saved file '${currentFile.name}' (${_editingContent.value.length} chars)")
                refreshFiles()
            }
            _selectedFileForEdit.value = null
            onSaved(ok)
        }
    }

    fun closeEditor() {
        _selectedFileForEdit.value = null
        _editingContent.value = ""
    }

    fun setAsMainFile(fileName: String) {
        prefs.mainScriptName = fileName
        engine.addLog(LogLevel.INFO, "Launcher", "📌 Main execution script set to '$fileName'")
        refreshFiles()
    }

    fun createNewFile(name: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = fileManager.createNewFile(name, content)
            if (ok) {
                engine.addLog(LogLevel.SUCCESS, "FileManager", "📄 Created new file '$name'")
                refreshFiles()
            }
            onResult(ok)
        }
    }

    fun deleteFile(fileName: String) {
        viewModelScope.launch {
            val ok = fileManager.deleteFile(fileName)
            if (ok) {
                engine.addLog(LogLevel.WARN, "FileManager", "🗑 Deleted file '$fileName'")
                refreshFiles()
            }
        }
    }

    fun scanDependencies() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = packageManager.scanDependencies()
            _scanDiscovered.value = result.discoveredPackages
            _isScanning.value = false
            engine.addLog(
                LogLevel.INFO,
                "PackageManager",
                "🔍 Scanned dependencies: ${result.discoveredPackages.size} packages found (source: ${if (result.fromRequirements) "requirements.txt" else "imports scan"})"
            )
        }
    }

    fun installDiscoveredPackages() {
        val pkgs = _scanDiscovered.value
        if (pkgs.isEmpty()) return

        viewModelScope.launch {
            _isInstalling.value = true
            for ((index, pkg) in pkgs.withIndex()) {
                _installStatusText.value = "نصب پکیج ${index + 1} از ${pkgs.size}: $pkg"
                _installProgress.value = (index + 0.1f) / pkgs.size
                packageManager.installPackage(pkg) { step, pct ->
                    _installStatusText.value = "[$pkg] $step"
                }
                _installProgress.value = (index + 1f) / pkgs.size
            }
            _isInstalling.value = false
            _installStatusText.value = "تمام پکیج‌های کشف‌شده نصب شدند ✅"
            engine.addLog(LogLevel.SUCCESS, "PackageManager", "✅ Auto-install completed for ${pkgs.size} packages.")
        }
    }

    fun installManualPackage(spec: String) {
        viewModelScope.launch {
            _isInstalling.value = true
            _installStatusText.value = "در حال نصب $spec..."
            _installProgress.value = 0.2f
            val ok = packageManager.installPackage(spec) { step, pct ->
                _installStatusText.value = step
                _installProgress.value = pct
            }
            _isInstalling.value = false
            _installStatusText.value = if (ok) "نصب با موفقیت انجام شد ✅" else "خطا در نصب ❌"
        }
    }

    fun uninstallPackage(name: String) {
        viewModelScope.launch {
            packageManager.uninstallPackage(name)
        }
    }

    fun clearPipLogs() {
        _pipLogEntries.value = emptyList()
    }

    // Settings Updates
    fun updateBootAutoStart(enabled: Boolean) {
        prefs.bootAutoStart = enabled
        _bootAutoStart.value = enabled
    }

    fun updateAutoRestartOnCrash(enabled: Boolean) {
        prefs.autoRestartOnCrash = enabled
        _autoRestartOnCrash.value = enabled
    }

    fun updateWakeLockEnabled(enabled: Boolean) {
        prefs.wakeLockEnabled = enabled
        _wakeLockEnabled.value = enabled
    }

    fun updateApiToken(token: String) {
        prefs.apiToken = token
        _apiToken.value = token
    }

    fun updateRoomId(roomId: String) {
        prefs.roomId = roomId
        _roomId.value = roomId
    }

    fun updateBotPrefix(prefix: String) {
        prefs.botPrefix = prefix
        _botPrefix.value = prefix
    }

    fun updateOwnerId(ownerId: String) {
        prefs.ownerId = ownerId
        _ownerId.value = ownerId
    }

    fun updateMaskTokens(enabled: Boolean) {
        prefs.maskTokensInLogs = enabled
        _maskTokensInLogs.value = enabled
    }
}
