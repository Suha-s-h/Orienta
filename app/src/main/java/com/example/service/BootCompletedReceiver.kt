package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ScreenDeckApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val app = context.applicationContext as? ScreenDeckApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = app.controller.repository.preferencesFlow.first()
                if (prefs.startOnBoot && (prefs.guardEnabled || prefs.globalImmersiveMode.isImmersiveActive || prefs.floatingDeckEnabled)) {
                    app.controller.startOrUpdateForegroundService()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
