package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
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
            ACTION_EXIT_APP -> {
                // 1. Immediately shut down foreground service and dismiss notification synchronously
                ScreenDeckForegroundService.activeInstance?.shutdownAndClearNotification()

                // 2. Clear any lingering system notification explicitly
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(4041)
                nm?.cancelAll()

                // 3. Persist master power switch to OFF
                controller.setMasterEnabled(false)

                // 4. Clear all active rotation locks and immersive overrides
                controller.disableAllOverrides()

                // 5. Ensure service is stopped if instance wasn't cached
                val serviceIntent = Intent(context, ScreenDeckForegroundService::class.java)
                context.stopService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_GUARD = "com.example.action.TOGGLE_GUARD"
        const val ACTION_SET_ROTATION = "com.example.action.SET_ROTATION"
        const val ACTION_CYCLE_IMMERSIVE = "com.example.action.CYCLE_IMMERSIVE"
        const val ACTION_STOP_ALL = "com.example.action.STOP_ALL"
        const val ACTION_EXIT_APP = "com.example.action.EXIT_APP"
        const val EXTRA_ROTATION_MODE = "extra_rotation_mode"
    }
}
