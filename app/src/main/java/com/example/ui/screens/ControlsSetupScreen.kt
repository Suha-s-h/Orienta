package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.model.DeckRuntimeState
import com.example.model.FloatingDeckSize
import com.example.service.SystemScreenBridge
import kotlin.math.roundToInt

@Composable
fun ControlsSetupScreen(
    state: DeckRuntimeState,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onToggleFloating: (Boolean) -> Unit,
    onFloatingOpacityChange: (Float) -> Unit,
    onFloatingSizeChange: (FloatingDeckSize) -> Unit,
    onFloatingSnapChange: (Boolean) -> Unit,
    onNotificationBarChange: (Boolean) -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onSyncAutoRotateChange: (Boolean) -> Unit,
    onOledMaskChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val adbCommand = remember(context.packageName) { SystemScreenBridge.buildAdbGrantCommand(context) }
    var adbExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Controls & Setup",
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.onBackground
                )
                Text(
                    text = "PERMISSIONS, FLOATING DECK & SYSTEM SURFACES",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Normal Android Permissions Checklist
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANDROID PERMISSIONS (NO ROOT)",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.permissions.coreReadyCount}/3 CORE READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.permissions.isFullyConfigured) colorScheme.tertiary else colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column {
                        PermissionRow(
                            icon = Icons.Default.Layers,
                            title = "Display Over Other Apps",
                            subtitle = "Core permission for global rotation lock, fullscreen overlays, and floating controls.",
                            granted = state.permissions.hasOverlay,
                            badge = "REQUIRED",
                            onClick = onRequestOverlayPermission,
                            testTag = "perm_overlay_row"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        PermissionRow(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Accessibility Auto-Switch",
                            subtitle = "Instantly applies per-app rules when switching apps & strengthens fullscreen immersion.",
                            granted = state.permissions.hasAccessibility,
                            badge = "RECOMMENDED",
                            onClick = onRequestAccessibilityPermission,
                            testTag = "perm_accessibility_row"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        PermissionRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Notification Control Bar",
                            subtitle = "Keeps one-tap Guard, Portrait, Landscape, 4-Way, and Fullscreen buttons in your shade.",
                            granted = state.permissions.hasNotification,
                            badge = "REQUIRED",
                            onClick = onRequestNotificationPermission,
                            testTag = "perm_notifications_row"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        PermissionRow(
                            icon = Icons.Default.SettingsSuggest,
                            title = "Modify System Settings",
                            subtitle = "Optional sync with Android's built-in Auto-Rotate setting.",
                            granted = state.permissions.hasWriteSettings,
                            badge = "OPTIONAL",
                            onClick = onRequestWriteSettingsPermission,
                            testTag = "perm_write_settings_row"
                        )
                    }
                }
            }
        }

        // Section 2: Floating Mini-Deck Studio
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FLOATING MINI-DECK",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ToggleRow(
                            title = "Show Floating Control",
                            subtitle = "Draggable edge handle to flip rotation, fullscreen, or pin app rules inside any app.",
                            checked = state.preferences.floatingDeckEnabled,
                            onCheckedChange = onToggleFloating,
                            testTag = "switch_floating_deck"
                        )

                        HorizontalDivider(color = colorScheme.outlineVariant)

                        // Handle Size Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Handle Size",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FloatingDeckSize.entries.forEach { size ->
                                    val selected = state.preferences.floatingSize == size
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onFloatingSizeChange(size) }
                                            .testTag("floating_size_${size.name.lowercase()}"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) colorScheme.secondary else colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = size.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) colorScheme.secondary else colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Opacity Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Overlay Opacity",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "${(state.preferences.floatingOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.secondary
                                )
                            }
                            Slider(
                                value = state.preferences.floatingOpacity,
                                onValueChange = onFloatingOpacityChange,
                                valueRange = 0.35f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = colorScheme.secondary,
                                    activeTrackColor = colorScheme.secondary,
                                    inactiveTrackColor = colorScheme.surfaceContainerHigh
                                )
                            )
                        }

                        ToggleRow(
                            title = "Magnetic Edge Snap",
                            subtitle = "Automatically docks the floating button cleanly against the nearest screen edge.",
                            checked = state.preferences.floatingSnapToEdge,
                            onCheckedChange = onFloatingSnapChange,
                            testTag = "switch_edge_snap"
                        )
                    }
                }
            }
        }

        // Section 3: Quick Settings Tiles & Engine Behavior
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CONTROL SURFACES & ENGINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ToggleRow(
                            title = "Interactive Notification Bar",
                            subtitle = "Displays Guard, Auto, Port, Land, 4-Way, and Fullscreen buttons right in the notification shade.",
                            checked = state.preferences.notificationBarEnabled,
                            onCheckedChange = onNotificationBarChange,
                            testTag = "switch_notif_bar"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        ToggleRow(
                            title = "OLED Clean Bar Masking",
                            subtitle = "Masks status and navigation bar regions cleanly in immersive modes without blocking touches or typing.",
                            checked = state.preferences.oledEdgeMaskEnabled,
                            onCheckedChange = onOledMaskChange,
                            testTag = "switch_oled_mask"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        ToggleRow(
                            title = "Sync System Auto-Rotate",
                            subtitle = "Keeps Android's system auto-rotate setting aligned with your selected rotation lock.",
                            checked = state.preferences.syncSystemAutoRotate,
                            onCheckedChange = onSyncAutoRotateChange,
                            testTag = "switch_sync_autorotate"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        ToggleRow(
                            title = "Restore on Device Boot",
                            subtitle = "Automatically re-arms ScreenDeck after your phone restarts.",
                            checked = state.preferences.startOnBoot,
                            onCheckedChange = onStartOnBootChange,
                            testTag = "switch_start_boot"
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        ToggleRow(
                            title = "Tactile Haptic Feedback",
                            subtitle = "Subtle hardware click feedback on keycap presses and guard toggles.",
                            checked = state.preferences.hapticsEnabled,
                            onCheckedChange = onHapticsChange,
                            testTag = "switch_haptics"
                        )
                    }
                }
            }
        }

        // Quick Settings Tile Tip Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Quick Settings Tiles",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Quick Settings Tiles Ready",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Swipe down twice from the top of your screen, tap the pencil (Edit) icon, and drag 'Rotation Lock' and 'Fullscreen Mode' into your Quick Settings panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Optional Advanced ADB True-Immersive Boost (collapsed by default)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (state.permissions.hasWriteSecureSettings) colorScheme.tertiary.copy(alpha = 0.6f)
                    else colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { adbExpanded = !adbExpanded }
                            .testTag("adb_boost_expand_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.permissions.hasWriteSecureSettings) Icons.Default.VerifiedUser else Icons.Default.Terminal,
                            contentDescription = "Optional ADB boost",
                            tint = if (state.permissions.hasWriteSecureSettings) colorScheme.tertiary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optional ADB Boost (Advanced)",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (state.permissions.hasWriteSecureSettings) {
                                    "WRITE_SECURE_SETTINGS Active — Native policy_control enabled"
                                } else {
                                    "Not required. Optional one-time command for legacy OEM policy_control."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.permissions.hasWriteSecureSettings) colorScheme.tertiary else colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (adbExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (adbExpanded) "Collapse ADB section" else "Expand ADB section",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = adbExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ScreenDeck already works with normal Android permissions above. If your device ROM supports system policy_control, you may optionally grant WRITE_SECURE_SETTINGS via computer or Wireless Debugging:",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.surfaceContainerLow)
                                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = adbCommand,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.secondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(adbCommand))
                                        onShowMessage("Copied ADB command to clipboard")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("copy_adb_command_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ADB command",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy")
                                }
                                Button(
                                    onClick = {
                                        onRefreshPermissions()
                                        val granted = state.permissions.hasWriteSecureSettings
                                        onShowMessage(
                                            if (granted) "ADB Boost verified and active"
                                            else "Standard normal-permission mode active (ADB not granted)"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("verify_adb_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.secondary,
                                        contentColor = colorScheme.onSecondary
                                    )
                                ) {
                                    Text("Verify Now")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    badge: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (granted) colorScheme.tertiaryContainer
                    else colorScheme.surfaceContainerHigh
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else icon,
                contentDescription = title,
                tint = if (granted) colorScheme.tertiary else colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (granted) colorScheme.tertiaryContainer
                    else colorScheme.primaryContainer
                )
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (granted) "GRANTED" else if (badge == "OPTIONAL") "SETUP" else "GRANT",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) colorScheme.tertiary else colorScheme.primary
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.onPrimary,
                checkedTrackColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.onSurfaceVariant,
                uncheckedTrackColor = colorScheme.surfaceContainerHigh
            )
        )
    }
}
