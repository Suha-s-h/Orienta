package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.DeckRuntimeState
import com.example.model.RotationMode
import kotlin.math.abs

/**
 * Draggable, magnetic edge-snapping tactile floating mini-deck overlay that lets users
 * switch rotation locks, toggle fullscreen mode, or pin a rule for the current app
 * from inside any other app.
 */
class FloatingDeckOverlay(
    private val context: Context,
    private val onToggleGuard: () -> Unit,
    private val onSelectRotation: (RotationMode) -> Unit,
    private val onCycleImmersive: () -> Unit,
    private val onPinCurrentApp: () -> Unit,
    private val onDismissFloating: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isExpanded = false
    private var lastState: DeckRuntimeState = DeckRuntimeState()

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    fun update(state: DeckRuntimeState) {
        mainHandler.post {
            lastState = state
            val shouldShow = state.preferences.floatingDeckEnabled && Settings.canDrawOverlays(context)
            if (!shouldShow) {
                hideOnMain()
                return@post
            }
            if (rootView == null) {
                showOnMain(state)
            } else {
                refreshContentOnMain(state)
            }
        }
    }

    fun hide() {
        mainHandler.post { hideOnMain() }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOnMain(state: DeckRuntimeState) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - dp(state.preferences.floatingSize.handleDp + 12)
            y = (screenHeight * 0.38f).toInt()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            alpha = state.preferences.floatingOpacity
        }

        layoutParams = params
        rootView = container
        refreshContentOnMain(state)

        try {
            windowManager.addView(container, params)
        } catch (_: Exception) {
            rootView = null
            layoutParams = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun refreshContentOnMain(state: DeckRuntimeState) {
        val container = rootView ?: return
        val params = layoutParams ?: return
        container.alpha = state.preferences.floatingOpacity
        container.removeAllViews()

        if (!isExpanded) {
            val sizePx = dp(state.preferences.floatingSize.handleDp)
            val isAmberActive = state.preferences.guardEnabled && state.effectiveRotationMode.isForcedLock
            val isCyanActive = state.effectiveImmersiveMode.isImmersiveActive
            val strokeColor = when {
                isAmberActive -> OverlayColors.AMBER
                isCyanActive -> OverlayColors.CYAN
                else -> OverlayColors.SLATE_STROKE
            }

            val handle = TextView(context).apply {
                text = when {
                    isAmberActive -> state.effectiveRotationMode.shortLabel
                    isCyanActive -> state.effectiveImmersiveMode.shortLabel
                    else -> "DECK"
                }
                setTextColor(
                    when {
                        isAmberActive -> OverlayColors.AMBER
                        isCyanActive -> OverlayColors.CYAN
                        else -> OverlayColors.INK_PRIMARY
                    }
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (sizePx < dp(44)) 9f else 10f)
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(OverlayColors.HANDLE_BG)
                    setStroke(dp(2), strokeColor)
                }
                elevation = dp(6).toFloat()
            }

            attachDragAndTapListener(handle, params)
            container.addView(handle, LinearLayout.LayoutParams(sizePx, sizePx))
        } else {
            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(18).toFloat()
                    setColor(OverlayColors.PANEL_BG)
                    setStroke(dp(1), OverlayColors.PANEL_STROKE)
                }
            }

            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), 0, dp(2), dp(8))
            }
            val title = TextView(context).apply {
                text = "ORIENTA • ${state.effectiveRotationMode.shortLabel}"
                setTextColor(OverlayColors.INK_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
            }
            val collapseBtn = buildChip("−", false, OverlayColors.INK_SECONDARY) {
                isExpanded = false
                refreshContentOnMain(lastState)
            }
            header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            header.addView(collapseBtn, LinearLayout.LayoutParams(dp(30), dp(28)))
            attachDragOnlyListener(header, params)
            panel.addView(header)

            val row1 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row1.addView(
                buildChip("GUARD", state.preferences.guardEnabled, OverlayColors.AMBER) { onToggleGuard() },
                chipParams(endMargin = 4)
            )
            row1.addView(
                buildChip("AUTO", state.effectiveRotationMode == RotationMode.AUTO_SYSTEM, OverlayColors.AMBER) {
                    onSelectRotation(RotationMode.AUTO_SYSTEM)
                },
                chipParams(endMargin = 4)
            )
            row1.addView(
                buildChip("PORT", state.effectiveRotationMode == RotationMode.PORTRAIT, OverlayColors.AMBER) {
                    onSelectRotation(RotationMode.PORTRAIT)
                },
                chipParams(endMargin = 4)
            )
            row1.addView(
                buildChip("LAND", state.effectiveRotationMode == RotationMode.LANDSCAPE, OverlayColors.AMBER) {
                    onSelectRotation(RotationMode.LANDSCAPE)
                },
                chipParams(endMargin = 0)
            )
            panel.addView(row1)

            val row2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, 0)
            }
            row2.addView(
                buildChip("4-WAY", state.effectiveRotationMode == RotationMode.FULL_SENSOR, OverlayColors.AMBER) {
                    onSelectRotation(RotationMode.FULL_SENSOR)
                },
                chipParams(endMargin = 4)
            )
            row2.addView(
                buildChip("R-LAND", state.effectiveRotationMode == RotationMode.REVERSE_LANDSCAPE, OverlayColors.AMBER) {
                    onSelectRotation(RotationMode.REVERSE_LANDSCAPE)
                },
                chipParams(endMargin = 4)
            )
            row2.addView(
                buildChip(
                    label = if (state.effectiveImmersiveMode.isImmersiveActive) state.effectiveImmersiveMode.shortLabel else "FULL",
                    active = state.effectiveImmersiveMode.isImmersiveActive,
                    accentColor = OverlayColors.CYAN
                ) { onCycleImmersive() },
                chipParams(endMargin = 4)
            )
            row2.addView(
                buildChip("PIN", state.activeAppRulePackage != null, OverlayColors.EMERALD) {
                    onPinCurrentApp()
                },
                chipParams(endMargin = 0)
            )
            panel.addView(row2)

            val footer = TextView(context).apply {
                text = "Hide Floating Control"
                setTextColor(OverlayColors.INK_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), dp(2))
                setOnClickListener {
                    isExpanded = false
                    onDismissFloating()
                }
            }
            panel.addView(footer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            container.addView(panel)
        }

        try {
            windowManager.updateViewLayout(container, params)
        } catch (_: Exception) {
            // Not yet attached or detached
        }
    }

    private fun chipParams(endMargin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(dp(56), dp(36)).apply {
            marginEnd = dp(endMargin)
        }
    }

    private fun buildChip(
        label: String,
        active: Boolean,
        accentColor: Int,
        onClick: () -> Unit
    ): TextView {
        val fill = if (active) Color.argb(56, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        else OverlayColors.CHIP_BG
        val stroke = if (active) accentColor else OverlayColors.PANEL_STROKE
        val textColor = if (active) accentColor else OverlayColors.INK_PRIMARY

        return TextView(context).apply {
            text = label
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(fill)
                setStroke(dp(1), stroke)
            }
            setOnClickListener { onClick() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragAndTapListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) {
                        moved = true
                    }
                    params.x = initialX + dx
                    params.y = (initialY + dy).coerceIn(dp(24), context.resources.displayMetrics.heightPixels - dp(80))
                    updateParamsSafe(params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        isExpanded = true
                        refreshContentOnMain(lastState)
                    } else if (lastState.preferences.floatingSnapToEdge) {
                        val screenW = context.resources.displayMetrics.widthPixels
                        val handleW = dp(lastState.preferences.floatingSize.handleDp)
                        params.x = if (params.x + handleW / 2 < screenW / 2) {
                            dp(8)
                        } else {
                            screenW - handleW - dp(8)
                        }
                        updateParamsSafe(params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragOnlyListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = (initialY + (event.rawY - initialTouchY).toInt())
                        .coerceIn(dp(24), context.resources.displayMetrics.heightPixels - dp(160))
                    updateParamsSafe(params)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateParamsSafe(params: WindowManager.LayoutParams) {
        val view = rootView ?: return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            // Safe ignore
        }
    }

    private fun hideOnMain() {
        val view = rootView ?: return
        try {
            windowManager.removeViewImmediate(view)
        } catch (_: Exception) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // Already detached
            }
        }
        rootView = null
        layoutParams = null
        isExpanded = false
    }

    private object OverlayColors {
        val AMBER = Color.parseColor("#F5A524")
        val CYAN = Color.parseColor("#22D3EE")
        val EMERALD = Color.parseColor("#34D399")
        val SLATE_STROKE = Color.parseColor("#475569")
        val INK_PRIMARY = Color.parseColor("#F1F5F9")
        val INK_SECONDARY = Color.parseColor("#94A3B8")
        val HANDLE_BG = Color.parseColor("#EE14181F")
        val PANEL_BG = Color.parseColor("#F20B0D10")
        val PANEL_STROKE = Color.parseColor("#2A3240")
        val CHIP_BG = Color.parseColor("#1C222C")
    }
}
