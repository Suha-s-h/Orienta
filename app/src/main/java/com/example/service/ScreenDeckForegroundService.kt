package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.ScreenDeckApplication
import com.example.model.DeckRuntimeState
import com.example.model.RotationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenDeckForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: ScreenDeckController
    private lateinit var overlayEngine: OverlayWindowEngine
    private lateinit var floatingDeck: FloatingDeckOverlay

    private var stateCollectionJob: Job? = null
    private var isScreenOn = true

    // Cache notification state to avoid redundant rebuilds
    private var lastNotifRotation: RotationMode? = null
    private var lastNotifImmersiveTitle: String? = null
    private var lastNotifGuardEnabled: Boolean? = null
    private var lastNotifBarEnabled: Boolean? = null
    private var lastNotifSourceLabel: String? = null

    // Screen on/off receiver for zero-idle-wakeup battery optimization
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        controller = (application as ScreenDeckApplication).controller
        overlayEngine = OverlayWindowEngine(this, useAccessibilityOverlayType = false)
        floatingDeck = FloatingDeckOverlay(
            context = this,
            onToggleGuard = { controller.toggleGuard() },
            onSelectRotation = { mode -> controller.setGlobalRotationMode(mode) },
            onCycleImmersive = { controller.cycleGlobalImmersiveMode() },
            onPinCurrentApp = {
                val label = controller.pinCurrentForegroundAppRule()
                val msg = if (label != null) {
                    "Pinned current mode for $label"
                } else {
                    "Enable Accessibility to pin rules for open apps"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            },
            onDismissFloating = { controller.setFloatingDeckEnabled(false) }
        )

        ensureNotificationChannel()
        startForegroundSafe(controller.runtimeState.value)
        controller.onServiceRunningChanged(true)
        activeInstance = this

        // Register screen on/off receiver for battery optimization
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        startStateCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = controller.runtimeState.value
        if (!state.preferences.masterEnabled) {
            try {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()
            return START_NOT_STICKY
        }
        if (isScreenOn) {
            applyRuntimeState(state)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stateCollectionJob?.cancel()
        serviceScope.cancel()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        overlayEngine.releaseAll()
        floatingDeck.hide()
        controller.onServiceRunningChanged(false)
        super.onDestroy()
    }

    fun setKeyboardActive(active: Boolean) {
        overlayEngine.setKeyboardActive(active)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onScreenOff() {
        isScreenOn = false
        // Pause: release overlays and floating deck to save battery
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        overlayEngine.releaseAll()
        floatingDeck.hide()
    }

    private fun onScreenOn() {
        isScreenOn = true
        // Resume: re-apply current state and restart collection
        val currentState = controller.runtimeState.value
        applyRuntimeState(currentState)
        startStateCollection()
    }

    private fun startStateCollection() {
        stateCollectionJob?.cancel()
        stateCollectionJob = serviceScope.launch {
            controller.runtimeState
                .collect { state ->
                    if (isScreenOn) {
                        applyRuntimeState(state)
                    }
                }
        }
    }

    private fun applyRuntimeState(state: DeckRuntimeState) {
        val enforceRotation = state.preferences.guardEnabled || state.activeAppRulePackage != null
        overlayEngine.applyState(
            enforceRotation = enforceRotation,
            rotationMode = state.effectiveRotationMode,
            immersiveMode = state.effectiveImmersiveMode,
            oledEdgeMaskEnabled = false
        )
        floatingDeck.update(state)
        updateNotificationIfChanged(state)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundSafe(state: DeckRuntimeState) {
        val notification = buildNotification(state)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Safe fallback if system blocks foreground start temporarily
        }
    }

    /** Only rebuild and post the notification when displayed values actually change. */
    private fun updateNotificationIfChanged(state: DeckRuntimeState) {
        val currentRotation = state.effectiveRotationMode
        val currentImmersiveTitle = state.effectiveImmersiveMode.title
        val currentGuard = state.preferences.guardEnabled
        val currentBarEnabled = state.preferences.notificationBarEnabled
        val currentSource = state.activeSourceLabel

        if (currentRotation == lastNotifRotation &&
            currentImmersiveTitle == lastNotifImmersiveTitle &&
            currentGuard == lastNotifGuardEnabled &&
            currentBarEnabled == lastNotifBarEnabled &&
            currentSource == lastNotifSourceLabel
        ) {
            return // No visible change — skip notification rebuild
        }

        lastNotifRotation = currentRotation
        lastNotifImmersiveTitle = currentImmersiveTitle
        lastNotifGuardEnabled = currentGuard
        lastNotifBarEnabled = currentBarEnabled
        lastNotifSourceLabel = currentSource

        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, buildNotification(state))
        } catch (_: Exception) {
            // Ignore if POST_NOTIFICATIONS not yet granted
        }
    }

    private fun buildNotification(state: DeckRuntimeState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_rotation)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent)
            .setContentTitle("Orienta • ${state.activeSourceLabel}")
            .setContentText("${state.effectiveRotationMode.title}  |  ${state.effectiveImmersiveMode.title}")

        if (state.preferences.notificationBarEnabled) {
            val views = buildRemoteViews(state)
            builder
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views)
                .setCustomBigContentView(views)
        }

        return builder.build()
    }

    private fun buildRemoteViews(state: DeckRuntimeState): RemoteViews {
        val views = RemoteViews(packageName, R.layout.notification_control_bar)
        val guardText = if (state.preferences.guardEnabled) "GUARD ON" else "STANDBY"
        views.setTextViewText(R.id.notif_title, "ORIENTA  •  $guardText")
        views.setTextViewText(
            R.id.notif_status,
            "${state.effectiveRotationMode.shortLabel}  |  ${state.effectiveImmersiveMode.shortLabel}"
        )

        val guardBg = if (state.preferences.guardEnabled) R.drawable.bg_notif_btn_active else R.drawable.bg_notif_btn
        val autoBg = if (state.effectiveRotationMode == RotationMode.AUTO_SYSTEM) R.drawable.bg_notif_btn_active else R.drawable.bg_notif_btn
        val portBg = if (state.effectiveRotationMode == RotationMode.PORTRAIT) R.drawable.bg_notif_btn_active else R.drawable.bg_notif_btn
        val landBg = if (state.effectiveRotationMode == RotationMode.LANDSCAPE) R.drawable.bg_notif_btn_active else R.drawable.bg_notif_btn
        val sensorBg = if (state.effectiveRotationMode == RotationMode.FULL_SENSOR) R.drawable.bg_notif_btn_active else R.drawable.bg_notif_btn
        val immBg = if (state.effectiveImmersiveMode.isImmersiveActive) R.drawable.bg_notif_btn_cyan else R.drawable.bg_notif_btn

        views.setInt(R.id.btn_guard, "setBackgroundResource", guardBg)
        views.setInt(R.id.btn_auto, "setBackgroundResource", autoBg)
        views.setInt(R.id.btn_portrait, "setBackgroundResource", portBg)
        views.setInt(R.id.btn_landscape, "setBackgroundResource", landBg)
        views.setInt(R.id.btn_sensor, "setBackgroundResource", sensorBg)
        views.setInt(R.id.btn_immersive, "setBackgroundResource", immBg)

        views.setTextViewText(
            R.id.btn_immersive,
            if (state.effectiveImmersiveMode.isImmersiveActive) state.effectiveImmersiveMode.shortLabel else "FULL"
        )

        views.setOnClickPendingIntent(R.id.btn_guard, actionPendingIntent(201, DeckActionReceiver.ACTION_TOGGLE_GUARD))
        views.setOnClickPendingIntent(R.id.btn_auto, rotationPendingIntent(202, RotationMode.AUTO_SYSTEM))
        views.setOnClickPendingIntent(R.id.btn_portrait, rotationPendingIntent(203, RotationMode.PORTRAIT))
        views.setOnClickPendingIntent(R.id.btn_landscape, rotationPendingIntent(204, RotationMode.LANDSCAPE))
        views.setOnClickPendingIntent(R.id.btn_sensor, rotationPendingIntent(205, RotationMode.FULL_SENSOR))
        views.setOnClickPendingIntent(R.id.btn_immersive, actionPendingIntent(206, DeckActionReceiver.ACTION_CYCLE_IMMERSIVE))
        views.setOnClickPendingIntent(R.id.btn_exit, actionPendingIntent(207, DeckActionReceiver.ACTION_EXIT_APP))

        return views
    }

    private fun actionPendingIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(this, DeckActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun rotationPendingIntent(requestCode: Int, mode: RotationMode): PendingIntent {
        val intent = Intent(this, DeckActionReceiver::class.java).apply {
            action = DeckActionReceiver.ACTION_SET_ROTATION
            putExtra(DeckActionReceiver.EXTRA_ROTATION_MODE, mode.name)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val ACTION_SYNC = "com.example.action.SYNC_DECK"
        private const val CHANNEL_ID = "orienta_control_bar"
        private const val NOTIFICATION_ID = 4041

        @Volatile
        var activeInstance: ScreenDeckForegroundService? = null
            private set
    }
}
