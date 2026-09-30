package com.example.model

enum class BotStatus {
    STOPPED,
    STARTING,
    RUNNING,
    RESTARTING,
    ERROR
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    BOT,
    SYS
}

data class LogItem(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "Bot",
    val message: String
)

data class BotFileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isMain: Boolean = false,
    val extension: String = ""
)

data class PythonPackageItem(
    val name: String,
    val version: String,
    val summary: String = "",
    val isCustom: Boolean = false,
    val isInstalled: Boolean = true
)
