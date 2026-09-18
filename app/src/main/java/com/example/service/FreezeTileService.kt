package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.data.local.IslandDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class FreezeTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isCurrentlyActive = (tile.state == Tile.STATE_ACTIVE)

        CoroutineScope(Dispatchers.IO).launch {
            val db = IslandDatabase.getInstance(applicationContext)
            db.appDao().setFreezeStateForIsland("island_main", !isCurrentlyActive)
        }

        tile.state = if (isCurrentlyActive) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = if (isCurrentlyActive) "آن‌فریز جزیره" else "فریز همه"
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "فریز همه (Island)"
        tile.updateTile()
    }
}
