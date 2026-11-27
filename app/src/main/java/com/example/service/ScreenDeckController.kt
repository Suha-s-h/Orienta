package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.data.AppRuleEntity
import com.example.data.ScreenDeckRepository
import com.example.model.DeckPreferences
import com.example.model.DeckPreset
import com.example.model.DeckRuntimeState
import com.example.model.FloatingDeckSize
import com.example.model.ImmersiveMode
import com.example.model.PermissionDiagnostics
import com.example.model.RotationMode
import com.example.tile.ImmersiveQuickTileService
import com.example.tile.RotationQuickTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ScreenDeckController(
    private val context: Context,
    val repository: ScreenDeckRepository
) {
    private data class ForegroundApp(val packageName: String, val label: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val foregroundAppFlow = MutableStateFlow<ForegroundApp?>(null)
    private val serviceRunningFlow = MutableStateFlow(false)
    private val permissionsFlow = MutableStateFlow(PermissionInspector.inspect(context))

    val appRulesFlow: StateFlow<List<AppRuleEntity>> = repository.appRulesFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val runtimeState: StateFlow<DeckRuntimeState> = combine(
        repository.preferencesFlow,
        appRulesFlow,
        foregroundAppFlow,
        serviceRunningFlow,
        permissionsFlow
    ) { prefs, rules, fgApp, isRunning, permissions ->
        resolveRuntimeState(prefs, rules, fgApp, isRunning, permissions)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = DeckRuntimeState(permissions = PermissionInspector.inspect(context))
        )

    init {
        scope.launch {
            runtimeState
                .debounce(50L)
                .collect { state ->
                    applySideEffects(state)
                }
        }
    }

    private fun resolveRuntimeState(
        prefs: DeckPreferences,
        rules: List<AppRuleEntity>,
        fgApp: ForegroundApp?,
        isRunning: Boolean,
        permissions: PermissionDiagnostics
    ): DeckRuntimeState {
        val fgPkg = fgApp?.packageName
        if (!prefs.masterEnabled) {
            return DeckRuntimeState(
                preferences = prefs,
                effectiveRotationMode = RotationMode.AUTO_SYSTEM,
                effectiveImmersiveMode = ImmersiveMode.NORMAL,
                activeSourceLabel = "Orienta Standby",
                activeAppRulePackage = null,
                foregroundPackage = fgPkg,
                foregroundAppLabel = fgApp?.label,
                isServiceRunning = isRunning,
                permissions = permissions
            )
        }

        val matchingRule = if (fgPkg != null && fgPkg != context.packageName) {
            rules.firstOrNull { it.packageName == fgPkg && it.isEnabled }
        } else {
            null
        }

        val effectiveRotation: RotationMode
        val effectiveImmersive: ImmersiveMode
        val sourceLabel: String

        if (matchingRule != null) {
            effectiveRotation = matchingRule.rotationMode
            effectiveImmersive = matchingRule.immersiveMode
            sourceLabel = "Rule: ${matchingRule.appLabel}"
        } else if (prefs.guardEnabled) {
            effectiveRotation = prefs.globalRotationMode
            effectiveImmersive = prefs.globalImmersiveMode
            sourceLabel = "Global Guard"
        } else {
            effectiveRotation = RotationMode.AUTO_SYSTEM
            effectiveImmersive = prefs.globalImmersiveMode
            sourceLabel = if (prefs.globalImmersiveMode.isImmersiveActive) {
                "Immersive Active"
            } else {
                "Standby"
            }
        }

        val fgLabel = fgApp?.let { matchingRule?.appLabel ?: it.label }

        return DeckRuntimeState(
            preferences = prefs,
            effectiveRotationMode = effectiveRotation,
            effectiveImmersiveMode = effectiveImmersive,
            activeSourceLabel = sourceLabel,
            activeAppRulePackage = matchingRule?.packageName,
            foregroundPackage = fgPkg,
            foregroundAppLabel = fgLabel,
            isServiceRunning = isRunning,
            permissions = permissions
        )
    }

    private var lastSyncedAutoRotateKey: Triple<RotationMode, Boolean, Boolean>? = null
    private var lastSyncedImmersiveMode: ImmersiveMode? = null
    private var lastTileKey: Triple<Boolean, RotationMode, ImmersiveMode>? = null

    private fun applySideEffects(state: DeckRuntimeState) {
        val masterOn = state.preferences.masterEnabled
        if (!masterOn) {
            // Reset ADB boost once only if we previously enabled it in this session; never touch system auto-rotate.
            if (lastSyncedImmersiveMode != null && lastSyncedImmersiveMode != ImmersiveMode.NORMAL) {
                SystemScreenBridge.applyAdbImmersiveBoostIfPermitted(
                    context = context,
                    immersiveMode = ImmersiveMode.NORMAL
                )
            }
            lastSyncedAutoRotateKey = null
            lastSyncedImmersiveMode = ImmersiveMode.NORMAL

            ForegroundAppAccessibilityService.applyOverlayFallbackIfNeeded(state = state)

            if (state.isServiceRunning) {
                stopForegroundService()
            }

            val tileKey = Triple(false, RotationMode.AUTO_SYSTEM, ImmersiveMode.NORMAL)
            if (tileKey != lastTileKey) {
                lastTileKey = tileKey
                requestQuickSettingsTileUpdates()
            }
            return
        }

        val shouldKeepServiceAlive = masterOn && (
            state.preferences.guardEnabled ||
            state.effectiveImmersiveMode.isImmersiveActive ||
            state.preferences.floatingDeckEnabled ||
            state.activeAppRulePackage != null
        )

        val guardActive = masterOn && (state.preferences.guardEnabled || state.activeAppRulePackage != null)
        val autoRotateKey = Triple(state.effectiveRotationMode, guardActive, state.preferences.syncSystemAutoRotate)
        if (autoRotateKey != lastSyncedAutoRotateKey) {
            lastSyncedAutoRotateKey = autoRotateKey
            SystemScreenBridge.syncSystemAutoRotate(
                context = context,
                rotationMode = state.effectiveRotationMode,
                guardActive = guardActive,
                syncEnabled = state.preferences.syncSystemAutoRotate
            )
        }

        if (state.effectiveImmersiveMode != lastSyncedImmersiveMode) {
            lastSyncedImmersiveMode = state.effectiveImmersiveMode
            SystemScreenBridge.applyAdbImmersiveBoostIfPermitted(
                context = context,
                immersiveMode = state.effectiveImmersiveMode
            )
        }

        ForegroundAppAccessibilityService.applyOverlayFallbackIfNeeded(state = state)

        if (shouldKeepServiceAlive && !state.isServiceRunning && (state.permissions.hasOverlay || state.permissions.hasNotification || state.permissions.hasAccessibility)) {
            startOrUpdateForegroundService()
        } else if (!shouldKeepServiceAlive && state.isServiceRunning) {
            stopForegroundService()
        }

        val tileKey = Triple(state.preferences.guardEnabled, state.effectiveRotationMode, state.effectiveImmersiveMode)
        if (tileKey != lastTileKey) {
            lastTileKey = tileKey
            requestQuickSettingsTileUpdates()
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(masterEnabled = enabled) }
            if (!enabled) {
                stopForegroundService()
            }
        }
    }

    fun refreshPermissions() {
        permissionsFlow.value = PermissionInspector.inspect(context)
    }

    fun onServiceRunningChanged(running: Boolean) {
        serviceRunningFlow.value = running
    }

    fun onForegroundAppChanged(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        if (foregroundAppFlow.value?.packageName != packageName) {
            val label = repository.resolveAppLabel(packageName)
            foregroundAppFlow.value = ForegroundApp(packageName, label)
        }
    }

    fun setGuardEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(guardEnabled = enabled) }
        }
    }

    fun toggleGuard() {
        setGuardEnabled(!runtimeState.value.preferences.guardEnabled)
    }

    fun setGlobalRotationMode(mode: RotationMode, autoEnableGuard: Boolean = true) {
        scope.launch {
            repository.updatePreferences { prefs ->
                prefs.copy(
                    globalRotationMode = mode,
                    guardEnabled = if (autoEnableGuard && mode.isForcedLock) true else prefs.guardEnabled
                )
            }
        }
    }

    fun cycleGlobalRotationMode() {
        val next = runtimeState.value.preferences.globalRotationMode.nextQuickCycle()
        setGlobalRotationMode(next, autoEnableGuard = next.isForcedLock)
    }

    fun setGlobalImmersiveMode(mode: ImmersiveMode) {
        scope.launch {
            repository.updatePreferences { it.copy(globalImmersiveMode = mode) }
        }
    }

    fun cycleGlobalImmersiveMode() {
        val next = runtimeState.value.preferences.globalImmersiveMode.nextCycle()
        setGlobalImmersiveMode(next)
    }

    fun applyPreset(preset: DeckPreset) {
        scope.launch {
            repository.updatePreferences { prefs ->
                prefs.copy(
                    guardEnabled = preset.enableGuard,
                    globalRotationMode = preset.rotationMode,
                    globalImmersiveMode = preset.immersiveMode
                )
            }
        }
    }

    fun setFloatingDeckEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(floatingDeckEnabled = enabled) }
        }
    }

    fun setFloatingOpacity(opacity: Float) {
        scope.launch {
            repository.updatePreferences { it.copy(floatingOpacity = opacity.coerceIn(0.35f, 1.0f)) }
        }
    }

    fun setFloatingSize(size: FloatingDeckSize) {
        scope.launch {
            repository.updatePreferences { it.copy(floatingSize = size) }
        }
    }

    fun setFloatingSnapToEdge(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(floatingSnapToEdge = enabled) }
        }
    }

    fun setNotificationBarEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(notificationBarEnabled = enabled) }
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(startOnBoot = enabled) }
        }
    }

    fun setSyncSystemAutoRotate(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(syncSystemAutoRotate = enabled) }
        }
    }

    fun setOledEdgeMaskEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(oledEdgeMaskEnabled = enabled) }
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        scope.launch {
            repository.updatePreferences { it.copy(hapticsEnabled = enabled) }
        }
    }

    fun saveAppRule(
        packageName: String,
        appLabel: String,
        rotationMode: RotationMode,
        immersiveMode: ImmersiveMode,
        isEnabled: Boolean = true,
        presetTag: String? = null
    ) {
        scope.launch {
            repository.upsertAppRule(
                packageName = packageName,
                appLabel = appLabel,
                rotationMode = rotationMode,
                immersiveMode = immersiveMode,
                isEnabled = isEnabled,
                presetTag = presetTag
            )
        }
    }

    fun pinCurrentForegroundAppRule(): String? {
        val state = runtimeState.value
        val fgPkg = state.foregroundPackage
        if (fgPkg.isNullOrBlank() || fgPkg == context.packageName) return null
        val label = state.foregroundAppLabel ?: repository.resolveAppLabel(fgPkg)
        saveAppRule(
            packageName = fgPkg,
            appLabel = label,
            rotationMode = state.effectiveRotationMode,
            immersiveMode = state.effectiveImmersiveMode,
            isEnabled = true,
            presetTag = "Pinned"
        )
        return label
    }

    fun toggleAppRuleEnabled(packageName: String, enabled: Boolean) {
        scope.launch {
            repository.setAppRuleEnabled(packageName, enabled)
        }
    }

    fun deleteAppRule(packageName: String) {
        scope.launch {
            repository.deleteAppRule(packageName)
        }
    }

    fun disableAllOverrides() {
        scope.launch {
            repository.updatePreferences {
                it.copy(
                    guardEnabled = false,
                    globalRotationMode = RotationMode.AUTO_SYSTEM,
                    globalImmersiveMode = ImmersiveMode.NORMAL,
                    floatingDeckEnabled = false
                )
            }
            stopForegroundService()
        }
    }

    fun startOrUpdateForegroundService() {
        try {
            val intent = Intent(context, ScreenDeckForegroundService::class.java).apply {
                action = ScreenDeckForegroundService.ACTION_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // Foreground service restrictions on background boot or revoked permissions
        }
    }

    fun stopForegroundService() {
        try {
            val intent = Intent(context, ScreenDeckForegroundService::class.java)
            context.stopService(intent)
        } catch (_: Exception) {
            // Safe ignore
        }
    }

    private fun requestQuickSettingsTileUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, RotationQuickTileService::class.java)
                )
                TileService.requestListeningState(
                    context,
                    ComponentName(context, ImmersiveQuickTileService::class.java)
                )
            } catch (_: Exception) {
                // Safe fallback if tiles are not yet added
            }
        }
    }
}
