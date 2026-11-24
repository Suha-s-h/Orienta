package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.ScreenDeckApplication
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

/**
 * Lightweight, least-privilege AccessibilityService that:
 * 1. Listens to TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOWS_CHANGED to trigger per-app
 *    rotation & fullscreen rules instantly and continuously enforce immersive flags.
 * 2. Listens to TYPE_VIEW_CLICKED and TYPE_VIEW_FOCUSED to yield overlay focus whenever
 *    the user clicks an editable text input / search box, allowing the soft keyboard (IME)
 *    to display immediately and smoothly.
 * 3. Automatically restores full immersive mode once the user dismisses the keyboard,
 *    clicks a non-editable result item, navigates back, or after an inactivity safety timeout.
 */
class ForegroundAppAccessibilityService : AccessibilityService() {

    private var accessibilityOverlayEngine: OverlayWindowEngine? = null
    private var lastForegroundPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isKeyboardActive = false

    private val keyboardSafetyResetRunnable = Runnable {
        if (isKeyboardActive) {
            isKeyboardActive = false
            resumeAllImmersiveFromKeyboard()
        }
    }

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        activeInstance = this
        accessibilityOverlayEngine = OverlayWindowEngine(
            context = this,
            useAccessibilityOverlayType = true
        )

        // Ensure runtime serviceInfo includes all required event types
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            serviceInfo = info
        } catch (_: Exception) {}

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

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                if (isEditableInputEvent(event)) {
                    pauseAllImmersiveForKeyboard()
                } else if (isKeyboardActive) {
                    // User tapped a non-editable result item, button, link or outside area
                    resumeAllImmersiveFromKeyboard()
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (isKeyboardActive) {
                    val imePresent = isKeyboardWindowPresent()
                    if (!imePresent) {
                        // Soft keyboard was dismissed (e.g. back button / keyboard close)
                        resumeAllImmersiveFromKeyboard()
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() }
                if (pkg != null && !IGNORED_PACKAGES.contains(pkg)) {
                    if (pkg != lastForegroundPackage) {
                        lastForegroundPackage = pkg
                        val controller = (application as? ScreenDeckApplication)?.controller
                        controller?.onForegroundAppChanged(pkg)
                        if (isKeyboardActive) {
                            resumeAllImmersiveFromKeyboard()
                        }
                    }
                }

                // Continuously keep immersive flags enforced on top of newly focused foreground app
                if (!isKeyboardActive) {
                    accessibilityOverlayEngine?.reapplyImmersiveFlags()
                    ScreenDeckForegroundService.activeInstance?.reapplyImmersiveFlags()
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(keyboardSafetyResetRunnable)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
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

    private fun isImmersiveModeActive(): Boolean {
        val controller = (application as? ScreenDeckApplication)?.controller
        val state = controller?.runtimeState?.value ?: return false
        return state.preferences.masterEnabled && state.effectiveImmersiveMode.isImmersiveActive
    }

    private fun pauseAllImmersiveForKeyboard() {
        if (!isImmersiveModeActive()) return
        mainHandler.removeCallbacks(keyboardSafetyResetRunnable)
        isKeyboardActive = true
        accessibilityOverlayEngine?.pauseForKeyboard()
        ScreenDeckForegroundService.activeInstance?.pauseImmersiveForKeyboard()
        // 15-second safety fallback: re-engage immersive mode if keyboard was left open/idle
        mainHandler.postDelayed(keyboardSafetyResetRunnable, 15000L)
    }

    private fun resumeAllImmersiveFromKeyboard() {
        mainHandler.removeCallbacks(keyboardSafetyResetRunnable)
        if (!isKeyboardActive) return
        isKeyboardActive = false
        accessibilityOverlayEngine?.resumeFromKeyboard()
        ScreenDeckForegroundService.activeInstance?.resumeImmersiveFromKeyboard()
    }

    private fun isEditableInputEvent(event: AccessibilityEvent): Boolean {
        if (event.isPassword) return true

        val className = event.className?.toString() ?: ""
        if (isEditableClassName(className)) return true

        val node = try { event.source } catch (_: Exception) { null }
        if (node != null) {
            try {
                if (node.isEditable) return true
                val nodeClass = node.className?.toString() ?: ""
                if (isEditableClassName(nodeClass)) return true
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    try { node.recycle() } catch (_: Exception) {}
                }
            }
        }
        return false
    }

    private fun isEditableClassName(className: String): Boolean {
        return className.contains("EditText", ignoreCase = true) ||
            className.contains("SearchView", ignoreCase = true) ||
            className.contains("SearchAutoComplete", ignoreCase = true) ||
            className.contains("AutoCompleteTextView", ignoreCase = true) ||
            className.contains("TextInputEditText", ignoreCase = true) ||
            className.contains("InputMethod", ignoreCase = true)
    }

    private fun isKeyboardWindowPresent(): Boolean {
        return try {
            val currentWindows = windows
            currentWindows.any { window ->
                window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun applyImmersiveOverlay(state: DeckRuntimeState) {
        val engine = accessibilityOverlayEngine ?: return
        val shouldApply = state.preferences.masterEnabled && state.effectiveImmersiveMode.isImmersiveActive
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
