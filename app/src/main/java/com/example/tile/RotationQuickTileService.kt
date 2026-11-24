package com.example.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.ScreenDeckApplication
import com.example.model.RotationMode

@RequiresApi(Build.VERSION_CODES.N)
class RotationQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val controller = (application as? ScreenDeckApplication)?.controller ?: return
        val current = controller.runtimeState.value
        if (!current.preferences.guardEnabled && current.preferences.globalRotationMode == RotationMode.AUTO_SYSTEM) {
            controller.setGlobalRotationMode(RotationMode.PORTRAIT, autoEnableGuard = true)
        } else {
            controller.cycleGlobalRotationMode()
        }
        syncTile()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val controller = (application as? ScreenDeckApplication)?.controller ?: return
        val state = controller.runtimeState.value
        val active = state.preferences.guardEnabled && state.effectiveRotationMode.isForcedLock

        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Rotation Lock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) state.effectiveRotationMode.title else "Standby"
        }
        tile.updateTile()
    }
}
