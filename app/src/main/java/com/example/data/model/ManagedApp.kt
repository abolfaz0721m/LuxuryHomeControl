package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * مدل مدیریت اپلیکیشن‌ها در فضاها (شخصی، جزیره، کلون شده)
 */
@Entity(tableName = "managed_apps")
data class ManagedApp(
    @PrimaryKey
    val uniqueId: String, // packageName + "_" + islandId + "_" + cloneIndex
    val packageName: String,
    val appName: String,
    val islandId: String = "island_main",
    val isIslandApp: Boolean = true,
    val isFrozen: Boolean = false,
    val isHidden: Boolean = false,
    val isCloned: Boolean = false,
    val cloneIndex: Int = 0,
    val fakeAndroidId: String = "",
    val fakeMac: String = "",
    val batteryDrainScore: Float = 1.2f, // Drain rate percentage
    val category: AppCategory = AppCategory.OTHER,
    val isSuspicious: Boolean = false,
    val isVpnEnabled: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)

enum class AppCategory {
    SOCIAL,
    MESSENGER,
    GAMING,
    FINANCE,
    PRODUCTIVITY,
    SYSTEM,
    OTHER
}
