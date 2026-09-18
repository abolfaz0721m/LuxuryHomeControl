package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * لاگ رویدادهای امنیتی و وضعیت ایزوله‌سازی جزیره
 */
@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val details: String,
    val severity: String = "INFO" // INFO, WARNING, CRITICAL
)
