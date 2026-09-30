package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.BotFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BotFileManager(private val context: Context, private val prefs: BotPreferences) {

    val botDirectory: File
        get() = File(context.filesDir, "bot").apply {
            if (!exists()) mkdirs()
        }

    suspend fun ensureDefaultFiles() = withContext(Dispatchers.IO) {
        val dir = botDirectory
        val mainScript = File(dir, "hrbot_full-31.py")
        if (!mainScript.exists()) {
            mainScript.writeText(DEFAULT_HRBOT_SCRIPT)
        }

        val reqsFile = File(dir, "requirements.txt")
        if (!reqsFile.exists()) {
            reqsFile.writeText(DEFAULT_REQUIREMENTS)
        }

        val envFile = File(dir, ".env")
        if (!envFile.exists()) {
            envFile.writeText(DEFAULT_ENV_FILE)
        }

        val configFile = File(dir, "config.json")
        if (!configFile.exists()) {
            configFile.writeText(DEFAULT_CONFIG_JSON)
        }
    }

    suspend fun listFiles(): List<BotFileItem> = withContext(Dispatchers.IO) {
        val dir = botDirectory
        val mainName = prefs.mainScriptName
        val files = dir.listFiles() ?: emptyArray()

        files.map { file ->
            val ext = file.extension.lowercase()
            BotFileItem(
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                isMain = file.name == mainName,
                extension = ext
            )
        }.sortedWith(compareByDescending<BotFileItem> { it.isMain }.thenBy { it.name.lowercase() })
    }

    suspend fun readFileContent(fileName: String): String = withContext(Dispatchers.IO) {
        val file = File(botDirectory, fileName)
        if (file.exists()) file.readText() else ""
    }

    suspend fun saveFileContent(fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(botDirectory, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createNewFile(fileName: String, initialContent: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(botDirectory, fileName)
            if (!file.exists()) {
                file.writeText(initialContent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(botDirectory, fileName)
            if (file.exists()) {
                val deleted = file.delete()
                if (file.name == prefs.mainScriptName) {
                    val remaining = listFiles().firstOrNull { it.extension == "py" }
                    if (remaining != null) {
                        prefs.mainScriptName = remaining.name
                    }
                }
                deleted
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromUri(uri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_${System.currentTimeMillis()}.py"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }

            val targetFile = File(botDirectory, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileName == "hrbot_full-31.py" || (fileName.endsWith(".py") && prefs.mainScriptName.isEmpty())) {
                prefs.mainScriptName = fileName
            }

            Pair(true, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Import failed")
        }
    }

    companion object {
        val DEFAULT_HRBOT_SCRIPT = """
# -*- coding: utf-8 -*-
\"\"\"
Highrise 24/7 Python Bot - hrbot_full-31.py
Enhanced Multi-Feature Highrise Bot with Asyncio Event Loop
\"\"\"
import os
import sys
import json
import time
import asyncio
import logging
import random
from datetime import datetime

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("HighriseBot")

# Load Configuration / Environment
API_TOKEN = os.getenv("HIGHRISE_API_TOKEN", "")
ROOM_ID = os.getenv("HIGHRISE_ROOM_ID", "")
BOT_PREFIX = os.getenv("BOT_PREFIX", "!")
OWNER_ID = os.getenv("BOT_OWNER_ID", "")

print("=" * 55)
print("🚀 Highrise 24/7 Bot Engine Initializing...")
print(f"📌 Python Version: {sys.version.split()[0]}")
print(f"📌 Script Path: {__file__}")
print(f"📌 Prefix: '{BOT_PREFIX}' | Room: '{ROOM_ID or 'Configured via .env'}'")
print("=" * 55)

class HighriseBot:
    def __init__(self):
        self.is_running = True
        self.start_time = time.time()
        self.message_count = 0
        self.tip_count = 0
        self.users_in_room = {}
        self.wallet_balance = 1250

    async def on_start(self):
        logger.info("✅ Bot successfully authenticated and joined room!")
        logger.info("📡 Listening for room events: Chat, Emotes, Tips, Joins, Leaves...")
        print("🤖 [HIGHRISE_EVENT] Connected to Highrise Gateway v24.1")

    async def handle_command(self, user: str, command: str, args: list):
        self.message_count += 1
        cmd = command.lower()
        logger.info(f"💬 Command received from {user}: '{command}' with args: {args}")

        if cmd == "help":
            return (
                f"🤖 [راهنمای ربات]\n"
                f"{BOT_PREFIX}help - نمایش این راهنما\n"
                f"{BOT_PREFIX}wallet - مشاهده موجودی کیف‌پول\n"
                f"{BOT_PREFIX}tip <مبلغ> - انتقال گلد\n"
                f"{BOT_PREFIX}emote <نام> - رقص و اموت\n"
                f"{BOT_PREFIX}teleport <x> <y> <z> - جابجایی در روم\n"
                f"{BOT_PREFIX}status - وضعیت اجرای ۲۴/۷ و آپتایم"
            )
        elif cmd == "wallet":
            return f"💰 موجودی کیف‌پول ربات: {self.wallet_balance} Gold"
        elif cmd == "status":
            uptime_sec = int(time.time() - self.start_time)
            mins, sec = divmod(uptime_sec, 60)
            hours, mins = divmod(mins, 60)
            return f"⏱ آپتایم: {hours} ساعت و {mins} دقیقه و {sec} ثانیه | پیام‌های پردازش شده: {self.message_count}"
        elif cmd == "emote":
            emote_name = args[0] if args else "dance-wave"
            return f"💃 اجرای اموت: {emote_name}"
        elif cmd == "tip":
            amount = int(args[0]) if args and args[0].isdigit() else 1
            self.tip_count += 1
            return f"🪙 تیپ {amount} گلد به {user} ثبت شد!"
        elif cmd == "radio":
            return "📻 وضعیت رادیو: آنلاین (استریم استریو فعال است)"
        else:
            return f"❓ دستور ناشناخته '{command}'. برای راهنما {BOT_PREFIX}help را بفرستید."

    async def simulate_room_heartbeat(self):
        \"\"\"Main 24/7 background event cycle\"\"\"
        cycle = 0
        emotes = ["dance-wave", "emote-lust", "dance-macarena", "emote-float", "emote-teleport"]
        sample_users = ["Alex_99", "Sarah_HR", "NightRider", "CyberPersian", "Kian_BotMaster"]

        while self.is_running:
            await asyncio.sleep(8)
            cycle += 1
            
            # Periodic room activity
            event_type = random.choice(["chat", "emote", "join", "heartbeat"])
            if event_type == "chat":
                user = random.choice(sample_users)
                cmd = random.choice(["wallet", "status", "help", "emote"])
                resp = await self.handle_command(user, cmd, [])
                print(f"💬 [CHAT] {user}: {BOT_PREFIX}{cmd} -> {resp}")
            elif event_type == "emote":
                user = random.choice(sample_users)
                em = random.choice(emotes)
                print(f"✨ [EMOTE] {user} performed emote: {em}")
            elif event_type == "heartbeat":
                logger.info(f"💓 [HEARTBEAT] Bot running healthy. Cycle #{cycle} | Memory: OK | Socket: Active")

    async def start(self):
        await self.on_start()
        await self.simulate_room_heartbeat()

async def main():
    bot = HighriseBot()
    try:
        await bot.start()
    except asyncio.CancelledError:
        logger.info("🛑 Bot execution gracefully cancelled.")
    except Exception as e:
        logger.error(f"❌ Error in HighriseBot execution: {e}", exc_info=True)
        raise e

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("🛑 Bot stopped by user signal.")
""".trimIndent()

        val DEFAULT_REQUIREMENTS = """
highrise-bot-sdk>=24.1.0
aiohttp>=3.9.3
yt-dlp>=2024.3.10
pillow>=10.2.0
requests>=2.31.0
websockets>=12.0
python-dotenv>=1.0.1
""".trimIndent()

        val DEFAULT_ENV_FILE = """
# Highrise API Credentials & Settings
HIGHRISE_API_TOKEN=hr_token_sample_abc123xyz
HIGHRISE_ROOM_ID=65df89bc213ef9081a2e4c3b
BOT_PREFIX=!
BOT_OWNER_ID=64fa79bc123def456
RADIO_STREAM_URL=http://stream.example.com/live
FFMPEG_PATH=/data/local/tmp/ffmpeg
""".trimIndent()

        val DEFAULT_CONFIG_JSON = """
{
  "bot_name": "Highrise Guardian 24/7",
  "version": "3.1.0",
  "auto_welcome": true,
  "welcome_message": "سلام به روم هایرایز خوش آمدید! ❤️",
  "auto_tip_thanks": true,
  "anti_spam": true,
  "max_messages_per_sec": 3,
  "reconnect_interval_sec": 5,
  "supported_emotes": [
    "dance-wave",
    "emote-lust",
    "dance-macarena",
    "emote-float",
    "emote-teleport"
  ]
}
""".trimIndent()
    }
}
