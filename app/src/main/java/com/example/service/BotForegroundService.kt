package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.BotFileManager
import com.example.data.BotPreferences
import com.example.engine.HighriseBotEngine
import com.example.model.BotStatus
import com.example.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BotForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var prefs: BotPreferences
    private lateinit var fileManager: BotFileManager
    private lateinit var db: AppDatabase
    private lateinit var wakeLock: PowerManager.WakeLock
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val CHANNEL_ID = "hr_bot_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.hrbot.ACTION_START"
        const val ACTION_STOP = "com.example.hrbot.ACTION_STOP"
        const val ACTION_RESTART = "com.example.hrbot.ACTION_RESTART"

        // Singleton instance access for UI ViewModel
        @Volatile
        var engineInstance: HighriseBotEngine? = null
            private set

        fun getOrCreateEngine(context: Context): HighriseBotEngine {
            return engineInstance ?: synchronized(this) {
                val c = context.applicationContext
                val p = BotPreferences(c)
                val fm = BotFileManager(c, p)
                val engine = HighriseBotEngine(c, fm, p)
                engineInstance = engine
                engine
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = BotPreferences(this)
        fileManager = BotFileManager(this, prefs)
        db = AppDatabase.getInstance(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HighriseBot::24_7_WakeLock")

        createNotificationChannel()
        setupNetworkMonitoring()

        val engine = getOrCreateEngine(this)

        // Observe engine status to update notification
        serviceScope.launch {
            engine.status.collect { status ->
                updateNotification(status)
            }
        }

        serviceScope.launch {
            engine.logs.collect { logs ->
                val lastLog = logs.lastOrNull()?.message ?: "در حال آماده‌سازی..."
                if (engine.status.value == BotStatus.RUNNING) {
                    updateNotification(BotStatus.RUNNING, lastLog)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val engine = getOrCreateEngine(this)

        when (action) {
            ACTION_START -> {
                acquireWakeLock()
                startForeground(NOTIFICATION_ID, buildNotification(BotStatus.RUNNING, "ربات در حال اجراست"))
                engine.start()
            }
            ACTION_STOP -> {
                engine.stop()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_RESTART -> {
                acquireWakeLock()
                engine.restart()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (prefs.wakeLockEnabled && !wakeLock.isHeld) {
            try {
                wakeLock.acquire(24 * 60 * 60 * 1000L) // 24 hours safe acquire
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                engineInstance?.addLog(LogLevel.SYS, "Network", "🌐 اینترنت متصل شد. اتصال گیت‌وی آماده است.")
            }

            override fun onLost(network: Network) {
                engineInstance?.addLog(LogLevel.WARN, "Network", "⚠️ ارتباط اینترنت قطع شد! انتظار برای اتصال مجدد...")
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "سرویس ۲۴/۷ ربات هایرایز"
            val desc = "وضعیت مداوم اجرای ربات پایتون در پس‌زمینه"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: BotStatus, detailText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BotForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val restartIntent = Intent(this, BotForegroundService::class.java).apply {
            action = ACTION_RESTART
        }
        val restartPendingIntent = PendingIntent.getService(
            this,
            2,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = when (status) {
            BotStatus.RUNNING -> "🟢 ربات هایرایز: در حال اجرا ۲۴/۷"
            BotStatus.STARTING -> "🟡 ربات هایرایز: در حال راه‌اندازی…"
            BotStatus.RESTARTING -> "🟠 ربات هایرایز: در حال اتصال مجدد…"
            BotStatus.ERROR -> "🔴 ربات هایرایز: بروز خطا"
            BotStatus.STOPPED -> "⚪️ ربات هایرایز: متوقف شده"
        }

        val iconRes = android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(statusTitle)
            .setContentText(detailText)
            .setSmallIcon(iconRes)
            .setContentIntent(contentPendingIntent)
            .setOngoing(status == BotStatus.RUNNING || status == BotStatus.STARTING || status == BotStatus.RESTARTING)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "توقف ربات", stopPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "ریاستارت", restartPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: BotStatus, detailText: String = "") {
        val text = if (detailText.isNotEmpty()) detailText else when (status) {
            BotStatus.RUNNING -> "ربات در روم فعال است | آپتایم مداوم"
            BotStatus.STARTING -> "در حال آماده‌سازی محیط و بارگذاری اسکریپت"
            BotStatus.RESTARTING -> "اتصال قطع شد؛ تلاش مجدد با Backoff"
            BotStatus.ERROR -> "خطا رخ داد؛ در حال بررسی مجدد"
            BotStatus.STOPPED -> "سرویس متوقف است"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status, text))
    }

    override fun onDestroy() {
        releaseWakeLock()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
