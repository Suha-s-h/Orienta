package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.ScreenDeckApplication
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

/**
 * Lightweight, least-privilege AccessibilityService that listens to
 * window and focus events to trigger per-app rotation & fullscreen rules
 * instantly, and provides a TYPE_ACCESSIBILITY_OVERLAY engine with smart
 * keyboard auto-yield for seamless typing in search and inputs.
 */
class ForegroundAppAccessibilityService : AccessibilityService() {

    private var accessibilityOverlayEngine: OverlayWindowEngine? = null
    private var isKeyboardYielded: Boolean = false
    private var lastForegroundPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    accessibilityOverlayEngine?.releaseAll()
                }
                Intent.ACTION_SCREEN_ON -> {
                    val controller = (application as? ScreenDeckApplication)?.controller
                    controller?.runtimeState?.value?.let { applyImmersiveOverlay(it) }
                }
            }
        }
    }

    private val keyboardSafetyRunnable = Runnable {
        if (isKeyboardYielded && !isInputMethodVisible()) {
            dispatchKeyboardActive(false)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        activeInstance = this
        accessibilityOverlayEngine = OverlayWindowEngine(
            context = this,
            useAccessibilityOverlayType = true
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        val controller = (application as? ScreenDeckApplication)?.controller
        controller?.refreshPermissions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType

        // 1. Detect focus or clicks on search and input fields to yield immediately
        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
        ) {
            val isInput = event.source?.isEditable == true ||
                event.className?.contains("EditText", ignoreCase = true) == true ||
                event.className?.contains("SearchView", ignoreCase = true) == true ||
                event.className?.contains("AutoCompleteTextView", ignoreCase = true) == true ||
                event.isPassword
            if (isInput) {
                dispatchKeyboardActive(true)
            }
            return
        }

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        // 2. Track Soft Keyboard (IME) window presence
        val kbVisible = isInputMethodVisible()
        if (kbVisible) {
            dispatchKeyboardActive(true)
        } else if (isKeyboardYielded) {
            dispatchKeyboardActive(false)
        }

        // 3. Track foreground app changes with deduplication for zero redundant CPU work
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() }
        if (pkg != null && !IGNORED_PACKAGES.contains(pkg)) {
            if (pkg != lastForegroundPackage) {
                lastForegroundPackage = pkg
                val controller = (application as? ScreenDeckApplication)?.controller
                controller?.onForegroundAppChanged(pkg)
            }
        }

        // Keep immersive flags enforced on top of newly focused foreground app
        if (!isKeyboardYielded) {
            accessibilityOverlayEngine?.reapplyImmersiveFlags()
        }
    }

    private fun isInputMethodVisible(): Boolean {
        return try {
            val windowList = windows ?: return false
            val displayHeight = resources.displayMetrics.heightPixels
            val minKbHeight = (displayHeight * 0.12).toInt()
            for (w in windowList) {
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    val rect = Rect()
                    w.getBoundsInScreen(rect)
                    if (rect.height() >= minKbHeight || rect.height() > 100) {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun dispatchKeyboardActive(active: Boolean) {
        if (isKeyboardYielded == active) return
        isKeyboardYielded = active
        mainHandler.removeCallbacks(keyboardSafetyRunnable)
        if (active) {
            // Safety timeout: auto-restore if keyboard doesn't actually open or is cancelled
            mainHandler.postDelayed(keyboardSafetyRunnable, 3500)
        }
        accessibilityOverlayEngine?.setKeyboardActive(active)
        ScreenDeckForegroundService.activeInstance?.setKeyboardActive(active)
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        mainHandler.removeCallbacks(keyboardSafetyRunnable)
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
