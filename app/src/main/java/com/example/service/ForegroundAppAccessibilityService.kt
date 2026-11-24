package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.ScreenDeckApplication
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

/**
 * Lightweight, least-privilege AccessibilityService that listens strictly to
 * TYPE_WINDOW_STATE_CHANGED events to trigger per-app rotation & fullscreen rules
 * instantly, and provides a TYPE_ACCESSIBILITY_OVERLAY engine when needed.
 */
class ForegroundAppAccessibilityService : AccessibilityService() {

    private var accessibilityOverlayEngine: OverlayWindowEngine? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        activeInstance = this
        accessibilityOverlayEngine = OverlayWindowEngine(
            context = this,
            useAccessibilityOverlayType = true
        )
        val controller = (application as? ScreenDeckApplication)?.controller
        controller?.refreshPermissions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (IGNORED_PACKAGES.contains(pkg)) return
        val controller = (application as? ScreenDeckApplication)?.controller ?: return
        controller.onForegroundAppChanged(pkg)
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        accessibilityOverlayEngine?.releaseAll()
        accessibilityOverlayEngine = null
        if (activeInstance === this) {
            activeInstance = null
        }
        isRunning = false
        val controller = (application as? ScreenDeckApplication)?.controller
        controller?.refreshPermissions()
        super.onDestroy()
    }

    private fun applyFallback(state: DeckRuntimeState, hasSystemOverlayPermission: Boolean) {
        val engine = accessibilityOverlayEngine ?: return
        if (hasSystemOverlayPermission) {
            // Foreground service already owns the TYPE_APPLICATION_OVERLAY windows
            engine.releaseAll()
            return
        }
        val enforceRotation = state.preferences.guardEnabled || state.activeAppRulePackage != null
        val shouldApply = (enforceRotation && state.effectiveRotationMode.isForcedLock) ||
            state.effectiveImmersiveMode.isImmersiveActive
        if (shouldApply) {
            engine.applyState(
                enforceRotation = enforceRotation,
                rotationMode = state.effectiveRotationMode,
                immersiveMode = state.effectiveImmersiveMode,
                oledEdgeMaskEnabled = state.preferences.oledEdgeMaskEnabled
            )
        } else {
            engine.applyState(
                enforceRotation = false,
                rotationMode = RotationMode.AUTO_SYSTEM,
                immersiveMode = ImmersiveMode.NORMAL,
                oledEdgeMaskEnabled = false
            )
        }
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        private var activeInstance: ForegroundAppAccessibilityService? = null

        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "android"
        )

        fun applyOverlayFallbackIfNeeded(
            state: DeckRuntimeState,
            hasSystemOverlayPermission: Boolean
        ) {
            activeInstance?.applyFallback(state, hasSystemOverlayPermission)
        }
    }
}
