package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ScreenDeckApplication
import com.example.model.RotationMode

class DeckActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val controller = (context.applicationContext as? ScreenDeckApplication)?.controller ?: return

        when (action) {
            ACTION_TOGGLE_GUARD -> controller.toggleGuard()
            ACTION_SET_ROTATION -> {
                val modeName = intent.getStringExtra(EXTRA_ROTATION_MODE)
                val mode = RotationMode.fromNameOrDefault(modeName)
                controller.setGlobalRotationMode(mode, autoEnableGuard = mode.isForcedLock)
            }
            ACTION_CYCLE_IMMERSIVE -> controller.cycleGlobalImmersiveMode()
            ACTION_STOP_ALL -> controller.disableAllOverrides()
        }
    }

    companion object {
        const val ACTION_TOGGLE_GUARD = "com.example.action.TOGGLE_GUARD"
        const val ACTION_SET_ROTATION = "com.example.action.SET_ROTATION"
        const val ACTION_CYCLE_IMMERSIVE = "com.example.action.CYCLE_IMMERSIVE"
        const val ACTION_STOP_ALL = "com.example.action.STOP_ALL"
        const val EXTRA_ROTATION_MODE = "extra_rotation_mode"
    }
}
