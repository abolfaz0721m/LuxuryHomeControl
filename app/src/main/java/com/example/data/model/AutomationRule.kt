package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * قانون اتوماسیون (اتصال به وای‌فای خانه، ساعت خاص، باتری ضعیف، و...)
 */
@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerType: TriggerType,
    val triggerValue: String,
    val actionType: ActionType,
    val targetIslandId: String = "island_main",
    val isEnabled: Boolean = true
)

enum class TriggerType {
    TIME_RANGE,       // e.g. 09:00-18:00
    WIFI_CONNECTED,   // e.g. Home_WiFi
    BATTERY_LOW,      // e.g. < 20%
    GAME_LAUNCHED     // e.g. Game package launched
}

enum class ActionType {
    FREEZE_SPACE,
    UNFREEZE_SPACE,
    ENABLE_VPN,
    PANIC_TRIGGER,
    BOOST_PERFORMANCE
}
