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
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() }
        if (pkg != null && !IGNORED_PACKAGES.contains(pkg)) {
            val controller = (application as? ScreenDeckApplication)?.controller
            controller?.onForegroundAppChanged(pkg)
        }
        // Keep immersive flags enforced on top of newly focused foreground app
        accessibilityOverlayEngine?.reapplyImmersiveFlags()
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

    private fun applyImmersiveOverlay(state: DeckRuntimeState) {
        val engine = accessibilityOverlayEngine ?: return
        val shouldApply = state.effectiveImmersiveMode.isImmersiveActive
        engine.applyState(
            enforceRotation = false,
            rotationMode = RotationMode.AUTO_SYSTEM,
            immersiveMode = if (shouldApply) state.effectiveImmersiveMode else ImmersiveMode.NORMAL,
            oledEdgeMaskEnabled = false
        )
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
            activeInstance?.applyImmersiveOverlay(state)
        }
    }
}
