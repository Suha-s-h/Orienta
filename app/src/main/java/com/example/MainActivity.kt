package com.example

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.PermissionInspector
import com.example.ui.screens.AppRulesScreen
import com.example.ui.screens.ControlsSetupScreen
import com.example.ui.screens.DeckScreen
import com.example.ui.theme.ScreenDeckTheme
import com.example.ui.viewmodel.ScreenDeckViewModel

private enum class DeckDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DECK("Deck", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_deck"),
    APP_RULES("App Rules", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_app_rules"),
    SETUP("Controls", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_controls")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            ScreenDeckTheme {
                ScreenDeckAppRoot(
                    onApplyActivityImmersivePreview = { active ->
                        applyActivityImmersive(active)
                    }
                )
            }
        }
    }

    private fun applyActivityImmersive(immersive: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun ScreenDeckAppRoot(
    onApplyActivityImmersivePreview: (Boolean) -> Unit,
    viewModel: ScreenDeckViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorScheme = MaterialTheme.colorScheme

    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val filteredAppRows by viewModel.filteredAppRows.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val livePreviewImmersive by viewModel.livePreviewImmersive.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(DeckDestination.DECK) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        viewModel.notifyMessage(
            if (granted) "Notification control bar enabled"
            else "Notifications denied — you can enable them in Android Settings"
        )
    }

    // Automatically refresh live permission checks whenever user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Bind in-app Live Fullscreen Preview toggle to Activity WindowInsetsControllerCompat
    LaunchedEffect(livePreviewImmersive) {
        onApplyActivityImmersivePreview(livePreviewImmersive)
    }

    // Collect ViewModel snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Return to main Deck tab on back press from secondary tabs
    BackHandler(enabled = currentDestination != DeckDestination.DECK) {
        currentDestination = DeckDestination.DECK
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        DeckDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colorScheme.primary,
                                    selectedTextColor = colorScheme.primary,
                                    indicatorColor = colorScheme.primaryContainer,
                                    unselectedIconColor = colorScheme.onSurfaceVariant,
                                    unselectedTextColor = colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = colorScheme.surface
                    ) {
                        DeckDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colorScheme.primary,
                                    selectedTextColor = colorScheme.primary,
                                    indicatorColor = colorScheme.primaryContainer,
                                    unselectedIconColor = colorScheme.onSurfaceVariant,
                                    unselectedTextColor = colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)

                    when (currentDestination) {
                        DeckDestination.DECK -> DeckScreen(
                            state = runtimeState,
                            livePreviewImmersive = livePreviewImmersive,
                            onToggleGuard = viewModel::toggleGuard,
                            onSelectRotation = viewModel::selectRotationMode,
                            onSelectImmersive = viewModel::selectImmersiveMode,
                            onApplyPreset = viewModel::applyPreset,
                            onToggleFloating = viewModel::setFloatingDeckEnabled,
                            onToggleLivePreview = {
                                viewModel.setLivePreviewImmersive(!livePreviewImmersive)
                            },
                            onResetAll = viewModel::disableAllOverrides,
                            onOpenSetupTab = { currentDestination = DeckDestination.SETUP },
                            modifier = contentModifier
                        )

                        DeckDestination.APP_RULES -> AppRulesScreen(
                            state = runtimeState,
                            rows = filteredAppRows,
                            isLoading = isLoadingApps,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFilterSelected = viewModel::selectFilter,
                            onSaveRule = viewModel::saveAppRule,
                            onToggleRule = viewModel::toggleAppRuleEnabled,
                            onDeleteRule = viewModel::deleteAppRule,
                            onOpenAccessibilitySettings = {
                                PermissionInspector.openAccessibilitySettings(context)
                            },
                            modifier = contentModifier
                        )

                        DeckDestination.SETUP -> ControlsSetupScreen(
                            state = runtimeState,
                            onRequestOverlayPermission = {
                                PermissionInspector.openOverlaySettings(context)
                            },
                            onRequestAccessibilityPermission = {
                                PermissionInspector.openAccessibilitySettings(context)
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !runtimeState.permissions.hasNotification) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    PermissionInspector.openNotificationSettings(context)
                                }
                            },
                            onRequestWriteSettingsPermission = {
                                PermissionInspector.openWriteSettings(context)
                            },
                            onRefreshPermissions = viewModel::refreshPermissions,
                            onToggleFloating = viewModel::setFloatingDeckEnabled,
                            onFloatingOpacityChange = viewModel::setFloatingOpacity,
                            onFloatingSizeChange = viewModel::setFloatingSize,
                            onFloatingSnapChange = viewModel::setFloatingSnapToEdge,
                            onNotificationBarChange = viewModel::setNotificationBarEnabled,
                            onStartOnBootChange = viewModel::setStartOnBoot,
                            onSyncAutoRotateChange = viewModel::setSyncSystemAutoRotate,
                            onOledMaskChange = viewModel::setOledEdgeMaskEnabled,
                            onHapticsChange = viewModel::setHapticsEnabled,
                            onShowMessage = viewModel::notifyMessage,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
