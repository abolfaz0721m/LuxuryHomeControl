package com.example.wirelessmic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WirelessMicApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "wireless_mic_stream_channel"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
