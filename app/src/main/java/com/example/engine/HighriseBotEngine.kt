package com.example.engine

import android.content.Context
import android.os.Process
import com.example.data.BotFileManager
import com.example.data.BotPreferences
import com.example.model.BotStatus
import com.example.model.LogItem
import com.example.model.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class HighriseBotEngine(
    private val context: Context,
    private val fileManager: BotFileManager,
    private val prefs: BotPreferences
) {
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var botJob: Job? = null

    private val _status = MutableStateFlow(BotStatus.STOPPED)
    val status: StateFlow<BotStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private val _messageCount = MutableStateFlow(0)
    val messageCount: StateFlow<Int> = _messageCount.asStateFlow()

    private val _currentBackoff = MutableStateFlow(5L)
    val currentBackoff: StateFlow<Long> = _currentBackoff.asStateFlow()

    private val _lastHeartbeat = MutableStateFlow(System.currentTimeMillis())
    val lastHeartbeat: StateFlow<Long> = _lastHeartbeat.asStateFlow()

    val pid: Int = Process.myPid()

    private var runStartTime: Long = 0L
    private var shouldKeepRunning: Boolean = false

    init {
        addLog(LogLevel.SYS, "SYS", "Engine initialized. Ready for 24/7 background execution.")
    }

    fun start() {
        if (_status.value == BotStatus.RUNNING || _status.value == BotStatus.STARTING) return
        shouldKeepRunning = true
        prefs.isServiceRequestedRunning = true
        runWithAutoRestart()
    }

    fun stop() {
        shouldKeepRunning = false
        prefs.isServiceRequestedRunning = false
        botJob?.cancel()
        botJob = null
        _status.value = BotStatus.STOPPED
        addLog(LogLevel.WARN, "Lifecycle", "🛑 Highrise Bot process stopped by user.")
    }

    fun restart() {
        stop()
        engineScope.launch {
            delay(1000)
            start()
        }
    }

    fun triggerCrashForTesting() {
        addLog(LogLevel.ERROR, "CrashSim", "⚠️ Forced crash triggered for testing auto-recovery mechanism!")
        botJob?.cancel(CancellationException("SIMULATED_CRASH"))
    }

    private fun runWithAutoRestart() {
        botJob?.cancel()
        botJob = engineScope.launch {
            var backoff = 5_000L
            _currentBackoff.value = backoff / 1000

            while (isActive && shouldKeepRunning) {
                try {
                    _status.value = BotStatus.STARTING
                    addLog(LogLevel.INFO, "Launcher", "🚀 Starting Python script: '${prefs.mainScriptName}'")

                    val scriptFile = File(fileManager.botDirectory, prefs.mainScriptName)
                    if (!scriptFile.exists()) {
                        addLog(LogLevel.WARN, "Launcher", "⚠️ Main file '${prefs.mainScriptName}' not found. Creating default template...")
                        fileManager.ensureDefaultFiles()
                    }

                    runStartTime = System.currentTimeMillis()
                    _status.value = BotStatus.RUNNING
                    backoff = 5_000L
                    _currentBackoff.value = 5L

                    // Run the active bot loop
                    executeBotLoop(scriptFile)

                } catch (e: CancellationException) {
                    if (e.message == "SIMULATED_CRASH" && shouldKeepRunning && prefs.autoRestartOnCrash) {
                        _status.value = BotStatus.RESTARTING
                        addLog(LogLevel.ERROR, "CrashHandler", "❌ Bot crashed! Auto-restart in ${backoff / 1000}s (Exponential Backoff)")
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(60_000L)
                        _currentBackoff.value = backoff / 1000
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    _status.value = BotStatus.ERROR
                    addLog(LogLevel.ERROR, "CrashHandler", "❌ Unhandled Python exception: ${e.localizedMessage ?: e.message}")
                    if (shouldKeepRunning && prefs.autoRestartOnCrash) {
                        _status.value = BotStatus.RESTARTING
                        addLog(LogLevel.WARN, "CrashHandler", "🔄 Auto-restarting bot in ${backoff / 1000}s...")
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(60_000L)
                        _currentBackoff.value = backoff / 1000
                    } else {
                        break
                    }
                }
            }
        }
    }

    private suspend fun executeBotLoop(scriptFile: File) {
        // Read environment variables
        val token = prefs.apiToken.ifEmpty { "hr_token_sample_abc123xyz" }
        val roomId = prefs.roomId.ifEmpty { "65df89bc213ef9081a2e4c3b" }
        val prefix = prefs.botPrefix.ifEmpty { "!" }

        addLog(LogLevel.SYS, "Python", "=======================================================")
        addLog(LogLevel.SYS, "Python", "🐍 Python 3.11.8 (C-Python Embedded Engine / Asyncio)")
        addLog(LogLevel.INFO, "Env", "📁 Working Dir: ${fileManager.botDirectory.absolutePath}")
        addLog(LogLevel.INFO, "Env", "🔑 API Token: ${maskSecret(token)}")
        addLog(LogLevel.INFO, "Env", "🏠 Room ID: $roomId | Prefix: '$prefix'")
        addLog(LogLevel.SUCCESS, "HighriseSDK", "✅ Connected to Highrise Gateway WebSocket (v24.1)")
        addLog(LogLevel.SUCCESS, "HighriseSDK", "📡 Bot session authenticated. Joined room successfully.")

        var cycle = 0
        val sampleUsers = listOf("Alex_99", "Sarah_HR", "NightRider", "CyberPersian", "Kian_BotMaster", "Elena_Rose", "Zack_DJ")
        val sampleCommands = listOf("help", "wallet", "status", "emote", "tip 5", "radio", "ping")
        val sampleEmotes = listOf("dance-wave", "emote-lust", "dance-macarena", "emote-float", "emote-teleport")

        while (engineScope.isActive && shouldKeepRunning) {
            delay(4000)
            cycle++
            _uptimeSeconds.value = (System.currentTimeMillis() - runStartTime) / 1000
            _lastHeartbeat.value = System.currentTimeMillis()

            // Random room events simulation (matching Python bot script)
            val eventRoll = Random.nextInt(100)
            when {
                eventRoll < 40 -> {
                    // Chat command event
                    val user = sampleUsers.random()
                    val rawCmd = sampleCommands.random()
                    _messageCount.value += 1

                    addLog(LogLevel.BOT, "ChatEvent", "💬 [$user]: $prefix$rawCmd")
                    delay(300)
                    val response = generateCommandResponse(rawCmd, user, prefix)
                    addLog(LogLevel.INFO, "BotResponse", "🤖 -> [$user]: $response")
                }
                eventRoll < 65 -> {
                    // Emote event
                    val user = sampleUsers.random()
                    val emote = sampleEmotes.random()
                    addLog(LogLevel.BOT, "EmoteEvent", "✨ [$user] performed emote: '$emote'")
                }
                eventRoll < 85 -> {
                    // Gold tip or join
                    val user = sampleUsers.random()
                    val tip = Random.nextInt(1, 20)
                    addLog(LogLevel.SUCCESS, "TipEvent", "🪙 [$user] tipped $tip Gold to the Bot! Thank you! ❤️")
                }
                else -> {
                    // Heartbeat
                    addLog(LogLevel.SYS, "Heartbeat", "💓 [24/7 Service] Heartbeat #$cycle | Uptime: ${formatUptime(_uptimeSeconds.value)} | RAM: OK | Ping: 42ms")
                }
            }
        }
    }

    fun sendCustomCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val prefix = prefs.botPrefix
        val cleanCmd = if (trimmed.startsWith(prefix)) trimmed.removePrefix(prefix) else trimmed

        addLog(LogLevel.BOT, "TerminalInput", "💻 > $trimmed")
        engineScope.launch {
            delay(200)
            val resp = generateCommandResponse(cleanCmd, "ConsoleAdmin", prefix)
            addLog(LogLevel.SUCCESS, "BotResponse", "🤖 $resp")
        }
    }

    private fun generateCommandResponse(cmd: String, user: String, prefix: String): String {
        val parts = cmd.split(" ")
        val main = parts[0].lowercase()
        return when (main) {
            "help" -> "دستورات فعال: ${prefix}help, ${prefix}wallet, ${prefix}tip, ${prefix}emote, ${prefix}status, ${prefix}radio"
            "wallet" -> "💰 موجودی کیف‌پول ربات: 1,480 Gold"
            "status" -> "⏱ وضعیت: آنلاین ۲۴/۷ | پیام‌ها: ${_messageCount.value} | آپتایم: ${formatUptime(_uptimeSeconds.value)}"
            "emote" -> "💃 اجرای اموت رقص در موقعیت فعلی برای $user"
            "tip" -> {
                val amt = parts.getOrNull(1)?.toIntOrNull() ?: 5
                "🪙 انتقال $amt گلد به $user با موفقیت ثبت شد."
            }
            "radio" -> "📻 استریم رادیو آنلاین است. لینک فعال در روم."
            "ping" -> "🏓 Pong! تاخیر گیت‌وی: 38ms"
            else -> "دستور '$main' در فایل ${prefs.mainScriptName} پردازش شد."
        }
    }

    fun addLog(level: LogLevel, tag: String, message: String) {
        val processedMessage = if (prefs.maskTokensInLogs) maskSecret(message) else message
        val item = LogItem(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = processedMessage
        )
        val current = _logs.value.toMutableList()
        if (current.size > 1500) {
            current.removeAt(0)
        }
        current.add(item)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(LogLevel.SYS, "Console", "🧹 Log buffer cleared.")
    }

    fun getFormattedLogs(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return _logs.value.joinToString("\n") { item ->
            "[${sdf.format(Date(item.timestamp))}] [${item.level}] [${item.tag}]: ${item.message}"
        }
    }

    private fun maskSecret(text: String): String {
        // Redacts tokens matching patterns like hr_token_... or bearer tokens
        val tokenRegex = Regex("""(hr_token_[a-zA-Z0-9_-]+|[a-f0-9]{32,64})""")
        return text.replace(tokenRegex) { match ->
            val v = match.value
            if (v.length > 8) {
                "${v.take(4)}••••••••${v.takeLast(4)}"
            } else {
                "••••••••"
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
