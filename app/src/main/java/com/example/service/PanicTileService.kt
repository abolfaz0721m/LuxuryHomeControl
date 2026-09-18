package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.data.local.IslandDatabase
import com.example.data.local.SecurityPreferences
import com.example.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@RequiresApi(Build.VERSION_CODES.N)
class PanicTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val context = applicationContext
        val db = IslandDatabase.getInstance(context)
        val prefs = SecurityPreferences(context)
        val securityManager = SecurityManager(context, db.appDao(), prefs)

        securityManager.triggerPanicMode(CoroutineScope(Dispatchers.IO))

        tile.state = Tile.STATE_ACTIVE
        tile.label = "وحشت فعال شد!"
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "حالت وحشت (Panic)"
        tile.updateTile()
    }
}
