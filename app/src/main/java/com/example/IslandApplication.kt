package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.data.local.IslandDatabase
import com.example.data.model.IslandProfile
import com.example.data.model.SpaceType
import com.example.service.IslandKeepAliveService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IslandApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Seed default spaces if database is empty
        CoroutineScope(Dispatchers.IO).launch {
            val db = IslandDatabase.getInstance(this@IslandApplication)
            val dao = db.appDao()
            val existing = dao.getProfileById("island_main")
            if (existing == null) {
                dao.insertProfiles(
                    listOf(
                        IslandProfile(
                            id = "island_main",
                            name = "جزیره کاری (Work Profile)",
                            description = "محیط امن و ایزوله اصلی برای کار و پیام‌رسان‌ها",
                            type = SpaceType.WORK,
                            colorHex = 0xFF00E5FF,
                            iconName = "Work"
                        ),
                        IslandProfile(
                            id = "island_secret",
                            name = "فضای مخفی (Secret Vault)",
                            description = "رمزگذاری‌شده با AES-256 و محافظت بیومتریک",
                            type = SpaceType.SECRET,
                            colorHex = 0xFFFF4081,
                            iconName = "Lock",
                            isBiometricLocked = true
                        ),
                        IslandProfile(
                            id = "island_gaming",
                            name = "جزیره گیمینگ (Game Island)",
                            description = "حداکثر عملکرد، بدون نوتیفیکیشن و فریز پس‌زمینه",
                            type = SpaceType.GAMING,
                            colorHex = 0xFF76FF03,
                            iconName = "Gamepad"
                        ),
                        IslandProfile(
                            id = "island_guest",
                            name = "حالت مهمان (Guest Space)",
                            description = "محدود کردن دسترسی‌ها و اطلاعات حساس",
                            type = SpaceType.GUEST,
                            colorHex = 0xFFFFAB00,
                            iconName = "Person"
                        )
                    )
                )
            }
        }

        // Start Foreground Keep-Alive service if enabled
        try {
            val serviceIntent = Intent(this, IslandKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
