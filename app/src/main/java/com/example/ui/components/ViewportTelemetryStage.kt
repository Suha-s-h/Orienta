package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

@Composable
fun ViewportTelemetryStage(
    state: DeckRuntimeState,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val rotationMode = state.effectiveRotationMode
    val immersiveMode = state.effectiveImmersiveMode
    val guardActive = state.preferences.guardEnabled || state.activeAppRulePackage != null

    val animatedDegrees by animateFloatAsState(
        targetValue = if (guardActive) rotationMode.previewDegrees else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "viewport_rotation"
    )
    val statusBarAlpha by animateFloatAsState(
        targetValue = if (immersiveMode.hidesStatusBar) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "status_bar_alpha"
    )
    val navBarAlpha by animateFloatAsState(
        targetValue = if (immersiveMode.hidesNavBar) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_bar_alpha"
    )

    val ledColor by animateColorAsState(
        targetValue = when {
            guardActive && rotationMode.isForcedLock -> colorScheme.primary
            immersiveMode.isImmersiveActive -> colorScheme.secondary
            else -> colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        },
        label = "led_color"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ledColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(220f, 160f),
                        radius = 480f
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Hardware Viewport Canvas
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colorScheme.surfaceContainerLow)
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryAccent = colorScheme.primary
                    val secondaryAccent = colorScheme.secondary
                    val frameColor = colorScheme.outline
                    val screenFill = colorScheme.background
                    val barColor = colorScheme.onSurfaceVariant

                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dialRadius = size.minDimension * 0.48f

                        // Subtle precision telemetry tick ring
                        drawCircle(
                            color = frameColor.copy(alpha = 0.25f),
                            radius = dialRadius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Cardinal degree pips
                        listOf(0f, 90f, 180f, 270f).forEach { deg ->
                            rotate(degrees = deg, pivot = center) {
                                drawLine(
                                    color = if (guardActive && rotationMode.previewDegrees == deg && rotationMode.isForcedLock) {
                                        primaryAccent
                                    } else {
                                        frameColor.copy(alpha = 0.45f)
                                    },
                                    start = Offset(center.x, center.y - dialRadius - 2.dp.toPx()),
                                    end = Offset(center.x, center.y - dialRadius + 4.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        // Rotating Device Frame
                        rotate(degrees = animatedDegrees, pivot = center) {
                            val phoneW = size.minDimension * 0.44f
                            val phoneH = size.minDimension * 0.72f
                            val left = center.x - phoneW / 2f
                            val top = center.y - phoneH / 2f

                            // Outer chassis
                            drawRoundRect(
                                color = screenFill,
                                topLeft = Offset(left, top),
                                size = Size(phoneW, phoneH),
                                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                            )
                            drawRoundRect(
                                color = if (guardActive && rotationMode.isForcedLock) primaryAccent else frameColor,
                                topLeft = Offset(left, top),
                                size = Size(phoneW, phoneH),
                                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                                style = Stroke(width = 1.8.dp.toPx())
                            )

                            // Immersive corner brackets when fullscreen active
                            if (immersiveMode.isImmersiveActive) {
                                val pad = 5.dp.toPx()
                                val len = 7.dp.toPx()
                                val bStroke = 1.8.dp.toPx()
                                // Top-left
                                drawLine(secondaryAccent, Offset(left + pad, top + pad), Offset(left + pad + len, top + pad), bStroke)
                                drawLine(secondaryAccent, Offset(left + pad, top + pad), Offset(left + pad, top + pad + len), bStroke)
                                // Top-right
                                drawLine(secondaryAccent, Offset(left + phoneW - pad, top + pad), Offset(left + phoneW - pad - len, top + pad), bStroke)
                                drawLine(secondaryAccent, Offset(left + phoneW - pad, top + pad), Offset(left + phoneW - pad, top + pad + len), bStroke)
                                // Bottom-left
                                drawLine(secondaryAccent, Offset(left + pad, top + phoneH - pad), Offset(left + pad + len, top + phoneH - pad), bStroke)
                                drawLine(secondaryAccent, Offset(left + pad, top + phoneH - pad), Offset(left + pad, top + phoneH - pad - len), bStroke)
                                // Bottom-right
                                drawLine(secondaryAccent, Offset(left + phoneW - pad, top + phoneH - pad), Offset(left + phoneW - pad - len, top + phoneH - pad), bStroke)
                                drawLine(secondaryAccent, Offset(left + phoneW - pad, top + phoneH - pad), Offset(left + phoneW - pad, top + phoneH - pad - len), bStroke)
                            }

                            // Top status bar indicator (fades away when hidden)
                            if (statusBarAlpha > 0.05f) {
                                drawRoundRect(
                                    color = barColor.copy(alpha = 0.65f * statusBarAlpha),
                                    topLeft = Offset(left + 7.dp.toPx(), top + 5.dp.toPx()),
                                    size = Size(phoneW - 14.dp.toPx(), 3.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }

                            // Orientation UP chevron indicator inside screen
                            val triY = center.y - 4.dp.toPx()
                            drawLine(
                                color = if (guardActive && rotationMode.isForcedLock) primaryAccent else barColor.copy(alpha = 0.7f),
                                start = Offset(center.x - 5.dp.toPx(), triY + 4.dp.toPx()),
                                end = Offset(center.x, triY - 2.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = if (guardActive && rotationMode.isForcedLock) primaryAccent else barColor.copy(alpha = 0.7f),
                                start = Offset(center.x, triY - 2.dp.toPx()),
                                end = Offset(center.x + 5.dp.toPx(), triY + 4.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Bottom nav bar indicator (fades away when hidden)
                            if (navBarAlpha > 0.05f) {
                                drawRoundRect(
                                    color = barColor.copy(alpha = 0.65f * navBarAlpha),
                                    topLeft = Offset(left + phoneW * 0.25f, top + phoneH - 7.dp.toPx()),
                                    size = Size(phoneW * 0.50f, 2.5.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Right-side Live Telemetry Readout & Actions
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ledColor)
                        )
                        Text(
                            text = state.activeSourceLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (guardActive || immersiveMode.isImmersiveActive) ledColor else colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = rotationMode.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryPill(
                            text = rotationMode.angleBadge,
                            accent = colorScheme.primary,
                            active = guardActive && rotationMode.isForcedLock
                        )
                        TelemetryPill(
                            text = immersiveMode.shortLabel,
                            accent = colorScheme.secondary,
                            active = immersiveMode.isImmersiveActive
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Reset All Overrides Action
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onResetAll)
                            .testTag("reset_all_overrides_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset rotation and fullscreen to default",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESET OVERRIDES",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryPill(
    text: String,
    accent: Color,
    active: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (active) accent.copy(alpha = 0.7f) else colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) accent else colorScheme.onSurfaceVariant
        )
    }
}
