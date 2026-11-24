package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.BuiltInPresets
import com.example.model.DeckRuntimeState
import com.example.model.ImmersiveMode
import com.example.model.RotationMode
import com.example.ui.viewmodel.AppRuleRowUi
import com.example.ui.viewmodel.AppRulesFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRulesScreen(
    state: DeckRuntimeState,
    rows: List<AppRuleRowUi>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilter: AppRulesFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (AppRulesFilter) -> Unit,
    onSaveRule: (String, String, RotationMode, ImmersiveMode, String?) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onDeleteRule: (String, String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var editingRow by remember { mutableStateOf<AppRuleRowUi?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Rules",
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.onBackground
                )
                Text(
                    text = "PER-APP ROTATION & FULLSCREEN AUTOMATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            val activeCount = rows.count { it.rule != null && it.rule.isEnabled }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activeCount > 0) colorScheme.primaryContainer else colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (activeCount > 0) colorScheme.primary else colorScheme.outlineVariant
                )
            ) {
                Text(
                    text = "$activeCount ACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (activeCount > 0) colorScheme.primary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.permissions.hasAccessibility) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenAccessibilitySettings)
                    .testTag("enable_accessibility_banner"),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.secondaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Enable Accessibility Service",
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Auto-Switch Accessibility",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to allow ScreenDeck to detect app launches and switch rules automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "ENABLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_apps_input"),
            placeholder = {
                Text(
                    text = "Search installed apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search apps",
                    tint = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppRulesFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onFilterSelected(filter) }
                        .testTag("filter_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colorScheme.primaryContainer else colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) colorScheme.primary else colorScheme.outlineVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (selectedFilter == AppRulesFilter.CONFIGURED) {
                            "No per-app rules yet"
                        } else {
                            "No matching apps found"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedFilter == AppRulesFilter.CONFIGURED) {
                            "Switch to All Apps and tap any app to configure automatic rotation and fullscreen modes."
                        } else {
                            "Try a different search keyword or switch filters."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (selectedFilter == AppRulesFilter.CONFIGURED) {
                        OutlinedButton(
                            onClick = { onFilterSelected(AppRulesFilter.ALL) },
                            modifier = Modifier.testTag("browse_all_apps_button")
                        ) {
                            Text("Browse All Apps")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows, key = { it.app.packageName }) { row ->
                    AppRuleRowCard(
                        row = row,
                        onClick = { editingRow = row },
                        onToggleEnabled = { enabled ->
                            onToggleRule(row.app.packageName, enabled)
                        }
                    )
                }
            }
        }
    }

    val currentEdit = editingRow
    if (currentEdit != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingRow = null },
            sheetState = sheetState,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface
        ) {
            AppRuleEditorSheet(
                row = currentEdit,
                onSave = { rot, imm, presetTag ->
                    onSaveRule(currentEdit.app.packageName, currentEdit.app.label, rot, imm, presetTag)
                    editingRow = null
                },
                onDelete = {
                    onDeleteRule(currentEdit.app.packageName, currentEdit.app.label)
                    editingRow = null
                },
                onDismiss = { editingRow = null }
            )
        }
    }
}

@Composable
private fun AppRuleRowCard(
    row: AppRuleRowUi,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val rule = row.rule
    val isRuleActive = rule != null && rule.isEnabled

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("app_rule_card_${row.app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = if (isRuleActive) 1.25.dp else 1.dp,
            color = if (isRuleActive) colorScheme.primary.copy(alpha = 0.7f) else colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstalledAppIcon(
                packageName = row.app.packageName,
                appLabel = row.app.label
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = row.app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (rule != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBadge(
                            text = rule.rotationMode.shortLabel,
                            active = isRuleActive && rule.rotationMode.isForcedLock,
                            isCyan = false
                        )
                        MiniBadge(
                            text = rule.immersiveMode.shortLabel,
                            active = isRuleActive && rule.immersiveMode.isImmersiveActive,
                            isCyan = true
                        )
                    }
                } else {
                    Text(
                        text = "Follows global deck",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (rule != null) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorScheme.onPrimary,
                        checkedTrackColor = colorScheme.primary
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure rule for ${row.app.label}",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InstalledAppIcon(
    packageName: String,
    appLabel: String
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "$appLabel icon",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appLabel.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    active: Boolean,
    isCyan: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (isCyan) colorScheme.secondary else colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else colorScheme.surfaceContainerHigh)
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.6f) else colorScheme.outlineVariant,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) accent else colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppRuleEditorSheet(
    row: AppRuleRowUi,
    onSave: (RotationMode, ImmersiveMode, String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var chosenRotation by remember(row) {
        mutableStateOf(row.rule?.rotationMode ?: RotationMode.LANDSCAPE)
    }
    var chosenImmersive by remember(row) {
        mutableStateOf(row.rule?.immersiveMode ?: ImmersiveMode.FULL_IMMERSIVE)
    }
    var chosenPresetTag by remember(row) { mutableStateOf(row.rule?.presetTag) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstalledAppIcon(row.app.packageName, row.app.label)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.app.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = row.app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // One-tap preset shortcuts
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "QUICK PRESETS",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BuiltInPresets.all.filter { it.id != "reset" }.forEach { preset ->
                    val matched = chosenRotation == preset.rotationMode &&
                        chosenImmersive == preset.immersiveMode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                chosenRotation = preset.rotationMode
                                chosenImmersive = preset.immersiveMode
                                chosenPresetTag = preset.title
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (matched) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (matched) colorScheme.primary else colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (matched) colorScheme.primary else colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Rotation selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ROTATION OVERRIDE",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RotationMode.entries.forEach { mode ->
                    val selected = chosenRotation == mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                chosenRotation = mode
                                chosenPresetTag = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colorScheme.primary else colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = "${mode.shortLabel} (${mode.angleBadge})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colorScheme.primary else colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Immersive selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FULLSCREEN MODE",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImmersiveMode.entries.forEach { mode ->
                    val selected = chosenImmersive == mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                chosenImmersive = mode
                                chosenPresetTag = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colorScheme.secondary else colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colorScheme.secondary else colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (row.rule != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("delete_app_rule_button"),
                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove rule",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove")
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = { onSave(chosenRotation, chosenImmersive, chosenPresetTag) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_app_rule_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text("Save App Rule")
            }
        }
    }
}
