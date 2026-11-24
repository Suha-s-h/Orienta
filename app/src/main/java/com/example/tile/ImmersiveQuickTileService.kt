package com.example.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.ScreenDeckApplication

@RequiresApi(Build.VERSION_CODES.N)
class ImmersiveQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val controller = (application as? ScreenDeckApplication)?.controller ?: return
        controller.cycleGlobalImmersiveMode()
        syncTile()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val controller = (application as? ScreenDeckApplication)?.controller ?: return
        val state = controller.runtimeState.value
        val active = state.effectiveImmersiveMode.isImmersiveActive

        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Fullscreen"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = state.effectiveImmersiveMode.title
        }
        tile.updateTile()
    }
}
