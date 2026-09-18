package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * مدل نمایانگر یک جزیره یا فضای ایزوله (Work Profile, Gaming Space, Secret Space, etc.)
 */
@Entity(tableName = "island_profiles")
data class IslandProfile(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val type: SpaceType = SpaceType.WORK,
    val colorHex: Long = 0xFF00E5FF, // Neon Cyan default
    val iconName: String = "Work",
    val isFrozen: Boolean = false,
    val isBiometricLocked: Boolean = false,
    val appCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SpaceType {
    WORK,
    PERSONAL,
    SECRET,
    GAMING,
    GUEST
}
