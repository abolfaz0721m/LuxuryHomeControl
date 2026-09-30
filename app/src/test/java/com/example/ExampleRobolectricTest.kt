package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BotFileManager
import com.example.data.BotPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Highrise 24/7 Bot Runner", appName)
  }

  @Test
  fun `bot preferences save and load config`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = BotPreferences(context)
    prefs.botPrefix = "$"
    assertEquals("$", prefs.botPrefix)
    prefs.bootAutoStart = true
    assertTrue(prefs.bootAutoStart)
  }

  @Test
  fun `bot file manager initializes default files`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = BotPreferences(context)
    val fileManager = BotFileManager(context, prefs)
    fileManager.ensureDefaultFiles()
    val files = fileManager.listFiles()
    assertTrue(files.any { it.name == "hrbot_full-31.py" })
    assertTrue(files.any { it.name == "requirements.txt" })
  }
}
