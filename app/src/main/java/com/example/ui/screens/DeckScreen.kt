package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BuiltInPresets
import com.example.model.DeckPreset
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode
import com.example.service.SystemScreenBridge
import com.example.ui.components.ViewportTelemetryStage

@Composable
fun DeckScreen(
    state: DeckRuntimeState,
    livePreviewImmersive: Boolean,
    onToggleGuard: (Boolean) -> Unit,
    onSelectRotation: (RotationMode) -> Unit,
    onSelectImmersive: (ImmersiveMode) -> Unit,
    onApplyPreset: (DeckPreset) -> Unit,
    onToggleFloating: (Boolean) -> Unit,
    onToggleLivePreview: () -> Unit,
    onResetAll: () -> Unit,
    onOpenSetupTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = state.preferences.hapticsEnabled

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orienta",
                        style = MaterialTheme.typography.displaySmall,
                        color = colorScheme.onBackground
                    )
                    Text(
                        text = "UNIVERSAL ROTATION & FULLSCREEN CONTROLLER",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Floating Mini-Deck Quick Pill
                val floatingOn = state.preferences.floatingDeckEnabled
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFloating(!floatingOn)
                        }
                        .testTag("quick_floating_toggle"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (floatingOn) colorScheme.secondaryContainer else colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (floatingOn) colorScheme.secondary else colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle floating mini-deck overlay",
                            tint = if (floatingOn) colorScheme.secondary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (floatingOn) "FLOAT ON" else "FLOAT",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (floatingOn) colorScheme.secondary else colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Normal Permissions Readiness Callout (only shown if core overlay/notification permissions missing)
        if (!state.permissions.hasOverlay) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenSetupTab)
                        .testTag("permission_readiness_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.errorContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Permission required",
                            tint = colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Display Over Other Apps",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Required for global rotation lock, fullscreen overlays, and floating controls across all apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "SETUP",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.error
                        )
                    }
                }
            }
        }

        // Live Animated Hardware Telemetry Stage
        item {
            ViewportTelemetryStage(
                state = state,
                livePreviewImmersive = livePreviewImmersive,
                onToggleLivePreview = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleLivePreview()
                },
                onResetAll = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResetAll()
                }
            )
        }

        // Master Guard Shield Card (signature feature from Rotation Control)
        item {
            val guardOn = state.preferences.guardEnabled
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleGuard(!guardOn)
                    }
                    .testTag("master_guard_card"),
                shape = RoundedCornerShape(18.dp),
                color = if (guardOn) colorScheme.primaryContainer.copy(alpha = 0.55f) else colorScheme.surface,
                border = BorderStroke(
                    width = if (guardOn) 1.5.dp else 1.dp,
                    color = if (guardOn) colorScheme.primary else colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (guardOn) colorScheme.primary.copy(alpha = 0.2f)
                                else colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Global rotation guard shield",
                            tint = if (guardOn) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Global Orientation Guard",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (guardOn) {
                                "Active — preventing other apps from overriding orientation"
                            } else {
                                "Standby — enable to enforce lock across all apps"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = guardOn,
                        onCheckedChange = {
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleGuard(it)
                        },
                        modifier = Modifier.testTag("master_guard_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.onPrimary,
                            checkedTrackColor = colorScheme.primary,
                            uncheckedThumbColor = colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        }

        // Quick Presets Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderLabel(
                    title = "QUICK PRESETS",
                    trailing = "ONE-TAP PROFILES"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(BuiltInPresets.all, key = { it.id }) { preset ->
                        val isActive = state.preferences.guardEnabled == preset.enableGuard &&
                            state.preferences.globalRotationMode == preset.rotationMode &&
                            state.preferences.globalImmersiveMode == preset.immersiveMode
                        PresetChipCard(
                            preset = preset,
                            isActive = isActive,
                            onClick = {
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onApplyPreset(preset)
                            }
                        )
                    }
                }
            }
        }

        // 8-Mode Rotation Control Matrix (2 rows x 4 keycaps)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderLabel(
                    title = "ROTATION MATRIX",
                    trailing = state.preferences.globalRotationMode.angleBadge
                )
                val modes = RotationMode.entries
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modes.chunked(4).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val selected = state.preferences.globalRotationMode == mode &&
                                    (state.preferences.guardEnabled || mode == RotationMode.AUTO_SYSTEM)
                                TactileKeycap(
                                    icon = rotationIconFor(mode),
                                    title = mode.shortLabel,
                                    subtitle = mode.angleBadge,
                                    selected = selected,
                                    accentColor = colorScheme.primary,
                                    accentContainer = colorScheme.primaryContainer,
                                    onClick = {
                                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectRotation(mode)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rotation_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4-Mode Fullscreen Immersive Matrix (2 rows x 2 wide keycaps)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderLabel(
                    title = "FULLSCREEN IMMERSIVE",
                    trailing = state.preferences.globalImmersiveMode.title.uppercase()
                )
                val modes = ImmersiveMode.entries
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modes.chunked(2).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val selected = state.preferences.globalImmersiveMode == mode
                                WideImmersiveKeycap(
                                    icon = immersiveIconFor(mode),
                                    title = mode.title,
                                    badge = mode.badge,
                                    description = mode.description,
                                    selected = selected,
                                    onClick = {
                                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectImmersive(mode)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("immersive_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // ADB Permission callout for system-wide immersive (hiding status/nav bar across all apps)
        if (!state.permissions.hasWriteSecureSettings) {
            item {
                val clipboard = LocalClipboardManager.current
                val context = LocalContext.current
                val adbCmd = remember(context.packageName) { SystemScreenBridge.buildAdbGrantCommand(context) }
                var copied by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(adbCmd))
                            copied = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "ADB Grant",
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "System-Wide Immersive (Like Fullscreen Immersive)",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "To hide navigation and status bars across all other apps, Android requires a one-time permission via ADB or Shizuku. Tap to copy the command:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (copied) "✓ Copied to clipboard! Run in ADB or Shizuku" else adbCmd,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (copied) colorScheme.primary else colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy command",
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionHeaderLabel(
    title: String,
    trailing: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun PresetChipCard(
    preset: DeckPreset,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("preset_${preset.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) colorScheme.primaryContainer.copy(alpha = 0.65f) else colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) colorScheme.primary else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) colorScheme.primary
                            else if (preset.immersiveMode.isImmersiveActive) colorScheme.secondary
                            else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                )
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isActive) colorScheme.primary else colorScheme.onSurface
                )
            }
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TactileKeycap(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    accentContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accentContainer.copy(alpha = 0.7f) else colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (selected) accentColor else colorScheme.outlineVariant)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accentColor else colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) accentColor else colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accentColor else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WideImmersiveKeycap(
    icon: ImageVector,
    title: String,
    badge: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.secondary
    Surface(
        modifier = modifier
            .height(106.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colorScheme.secondaryContainer.copy(alpha = 0.65f) else colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) accent else colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) accent.copy(alpha = 0.2f) else colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) accent else colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) accent else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun rotationIconFor(mode: RotationMode): ImageVector = when (mode) {
    RotationMode.AUTO_SYSTEM -> Icons.Default.AutoMode
    RotationMode.FULL_SENSOR -> Icons.Default.ScreenRotation
    RotationMode.PORTRAIT -> Icons.Default.ScreenLockPortrait
    RotationMode.REVERSE_PORTRAIT -> Icons.Default.StayCurrentPortrait
    RotationMode.SENSOR_PORTRAIT -> Icons.Default.ScreenLockRotation
    RotationMode.LANDSCAPE -> Icons.Default.ScreenLockLandscape
    RotationMode.REVERSE_LANDSCAPE -> Icons.Default.StayCurrentLandscape
    RotationMode.SENSOR_LANDSCAPE -> Icons.AutoMirrored.Filled.ScreenShare
}

private fun immersiveIconFor(mode: ImmersiveMode): ImageVector = when (mode) {
    ImmersiveMode.NORMAL -> Icons.Outlined.FullscreenExit
    ImmersiveMode.HIDE_STATUS -> Icons.Default.ViewAgenda
    ImmersiveMode.HIDE_NAVIGATION -> Icons.Default.CropFree
    ImmersiveMode.FULL_IMMERSIVE -> Icons.Default.Fullscreen
}
