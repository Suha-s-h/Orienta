package com.example.service

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

/**
 * Enforces global screen orientation lock (identical to Rotation Control) and
 * fullscreen immersive control (identical to Fullscreen Immersive) using normal
 * Android overlay / accessibility permissions.
 */
class OverlayWindowEngine(
    private val context: Context,
    private val useAccessibilityOverlayType: Boolean = false
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rotationView: View? = null
    private var immersiveHostView: View? = null
    private var statusMaskView: View? = null
    private var navMaskView: View? = null

    private var currentRotation: RotationMode? = null
    private var currentImmersive: ImmersiveMode? = null
    private var currentMaskEnabled: Boolean = false
    private var isPausedForKeyboard: Boolean = false

    private val overlayWindowType: Int
        get() = when {
            useAccessibilityOverlayType -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }

    fun canAttachOverlays(): Boolean {
        return useAccessibilityOverlayType || Settings.canDrawOverlays(context)
    }

    fun applyState(
        enforceRotation: Boolean,
        rotationMode: RotationMode,
        immersiveMode: ImmersiveMode,
        oledEdgeMaskEnabled: Boolean
    ) {
        mainHandler.post {
            if (!canAttachOverlays()) {
                releaseAllOnMain()
                return@post
            }

            val targetRotation = if (enforceRotation && rotationMode.isForcedLock) {
                rotationMode
            } else {
                RotationMode.AUTO_SYSTEM
            }
            applyRotationOnMain(targetRotation)
            if (isPausedForKeyboard) {
                currentImmersive = immersiveMode
                currentMaskEnabled = oledEdgeMaskEnabled
            } else {
                applyImmersiveOnMain(immersiveMode, oledEdgeMaskEnabled)
            }
        }
    }

    fun releaseAll() {
        mainHandler.post { releaseAllOnMain() }
    }

    private fun releaseAllOnMain() {
        isPausedForKeyboard = false
        removeViewSafe(rotationView)
        rotationView = null
        currentRotation = null

        removeViewSafe(immersiveHostView)
        immersiveHostView = null

        removeViewSafe(statusMaskView)
        statusMaskView = null

        removeViewSafe(navMaskView)
        navMaskView = null

        currentImmersive = null
        currentMaskEnabled = false
    }

    private fun applyRotationOnMain(mode: RotationMode) {
        if (mode == RotationMode.AUTO_SYSTEM) {
            removeViewSafe(rotationView)
            rotationView = null
            currentRotation = RotationMode.AUTO_SYSTEM
            return
        }

        val params = WindowManager.LayoutParams(
            0,
            0,
            overlayWindowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenOrientation = mode.screenOrientation
        }

        val existing = rotationView
        if (existing == null) {
            val view = View(context).apply {
                isClickable = false
                isFocusable = false
                alpha = 0f
            }
            if (addViewSafe(view, params)) {
                rotationView = view
                currentRotation = mode
            }
        } else if (currentRotation != mode) {
            updateViewSafe(existing, params)
            currentRotation = mode
        }
    }

    private fun applyImmersiveOnMain(mode: ImmersiveMode, oledEdgeMaskEnabled: Boolean) {
        if (mode == ImmersiveMode.NORMAL) {
            removeViewSafe(immersiveHostView)
            immersiveHostView = null
            removeViewSafe(statusMaskView)
            statusMaskView = null
            removeViewSafe(navMaskView)
            navMaskView = null
            currentImmersive = ImmersiveMode.NORMAL
            currentMaskEnabled = oledEdgeMaskEnabled
            return
        }

        if (currentImmersive == mode && currentMaskEnabled == oledEdgeMaskEnabled && immersiveHostView != null) {
            return
        }

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
            if (mode.hidesStatusBar) {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            } else {
                0
            }

        val hostParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType,
            baseFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val host = immersiveHostView ?: View(context).apply {
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            setBackgroundColor(Color.TRANSPARENT)
            setOnApplyWindowInsetsListener { v, insets ->
                currentImmersive?.let { applySystemUiFlagsToView(v, it) }
                insets
            }
        }

        if (immersiveHostView == null) {
            if (addViewSafe(host, hostParams)) {
                immersiveHostView = host
            }
        } else {
            updateViewSafe(host, hostParams)
        }

        applySystemUiFlagsToView(host, mode)

        // Clean up any legacy edge masks
        removeViewSafe(statusMaskView)
        statusMaskView = null
        removeViewSafe(navMaskView)
        navMaskView = null

        currentImmersive = mode
        currentMaskEnabled = false
    }

    fun reapplyImmersiveFlags() {
        mainHandler.post {
            if (isPausedForKeyboard) return@post
            val host = immersiveHostView ?: return@post
            val mode = currentImmersive ?: return@post
            if (mode.isImmersiveActive) {
                applySystemUiFlagsToView(host, mode)
            }
        }
    }

    fun pauseForKeyboard() {
        mainHandler.post {
            if (isPausedForKeyboard) return@post
            isPausedForKeyboard = true
            removeViewSafe(immersiveHostView)
            immersiveHostView = null
        }
    }

    fun resumeFromKeyboard() {
        mainHandler.post {
            if (!isPausedForKeyboard) return@post
            isPausedForKeyboard = false
            val mode = currentImmersive
            if (mode != null && mode.isImmersiveActive) {
                val targetMode = mode
                currentImmersive = null // Force re-creation
                applyImmersiveOnMain(targetMode, currentMaskEnabled)
            }
        }
    }

    val isImmersivePausedForKeyboard: Boolean
        get() = isPausedForKeyboard

    private fun buildEdgeMaskParams(heightPx: Int, gravityEdge: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx.coerceAtLeast(1),
            overlayWindowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = gravityEdge or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun applySystemUiFlagsToView(view: View, mode: ImmersiveMode) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = view.windowInsetsController
                if (controller != null) {
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    var typesToHide = 0
                    var typesToShow = 0
                    if (mode.hidesStatusBar) {
                        typesToHide = typesToHide or WindowInsets.Type.statusBars()
                    } else {
                        typesToShow = typesToShow or WindowInsets.Type.statusBars()
                    }
                    if (mode.hidesNavBar) {
                        typesToHide = typesToHide or WindowInsets.Type.navigationBars()
                    } else {
                        typesToShow = typesToShow or WindowInsets.Type.navigationBars()
                    }
                    if (typesToShow != 0) controller.show(typesToShow)
                    if (typesToHide != 0) controller.hide(typesToHide)
                }
            }

            @Suppress("DEPRECATION")
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            if (mode.hidesStatusBar) {
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            if (mode.hidesNavBar) {
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
            @Suppress("DEPRECATION")
            view.systemUiVisibility = flags
        } catch (_: Exception) {
            // Safe fallback across vendor WindowManager differences
        }
    }

    private fun getSystemBarHeightPx(resourceName: String, fallbackDp: Int): Int {
        val resId = context.resources.getIdentifier(resourceName, "dimen", "android")
        if (resId > 0) {
            val px = context.resources.getDimensionPixelSize(resId)
            if (px > 0) return px
        }
        return (fallbackDp * context.resources.displayMetrics.density).toInt()
    }

    private fun addViewSafe(view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            windowManager.addView(view, params)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun updateViewSafe(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            // View might have been detached by system
        }
    }

    private fun removeViewSafe(view: View?) {
        if (view == null) return
        try {
            windowManager.removeViewImmediate(view)
        } catch (_: Exception) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // Already removed
            }
        }
    }
}
