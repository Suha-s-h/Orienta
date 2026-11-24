package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScreenDeckApplication
import com.example.data.AppRuleEntity
import com.example.model.DeckPreset
import com.example.model.DeckRuntimeState
import com.example.model.FloatingDeckSize
import com.example.model.ImmersiveMode
import com.example.model.InstalledAppItem
import com.example.model.RotationMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRulesFilter(val label: String) {
    ALL("All Apps"),
    CONFIGURED("Active Rules"),
    USER_ONLY("Downloaded")
}

data class AppRuleRowUi(
    val app: InstalledAppItem,
    val rule: AppRuleEntity?
)

class ScreenDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = (application as ScreenDeckApplication).controller

    val runtimeState: StateFlow<DeckRuntimeState> = controller.runtimeState
    val appRules: StateFlow<List<AppRuleEntity>> = controller.appRulesFlow

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppRulesFilter.ALL)
    val selectedFilter: StateFlow<AppRulesFilter> = _selectedFilter.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val filteredAppRows: StateFlow<List<AppRuleRowUi>> = combine(
        _installedApps,
        appRules,
        _searchQuery,
        _selectedFilter
    ) { apps, rules, query, filter ->
        val ruleMap = rules.associateBy { it.packageName }
        val normalizedQuery = query.trim().lowercase()
        apps
            .map { app -> AppRuleRowUi(app = app, rule = ruleMap[app.packageName]) }
            .filter { row ->
                val matchesFilter = when (filter) {
                    AppRulesFilter.ALL -> true
                    AppRulesFilter.CONFIGURED -> row.rule != null
                    AppRulesFilter.USER_ONLY -> !row.app.isSystemApp
                }
                val matchesQuery = normalizedQuery.isEmpty() ||
                    row.app.label.lowercase().contains(normalizedQuery) ||
                    row.app.packageName.lowercase().contains(normalizedQuery)
                matchesFilter && matchesQuery
            }
            .sortedWith(
                compareByDescending<AppRuleRowUi> { it.rule != null && it.rule.isEnabled }
                    .thenByDescending { it.rule != null }
                    .thenBy { it.app.label.lowercase() }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshInstalledApps()
        refreshPermissions()
    }

    fun refreshPermissions() {
        controller.refreshPermissions()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = controller.repository.loadLaunchableApps()
            _isLoadingApps.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(filter: AppRulesFilter) {
        _selectedFilter.value = filter
    }

    fun setMasterEnabled(enabled: Boolean) {
        controller.setMasterEnabled(enabled)
        emitMessage(if (enabled) "Orienta engine activated" else "Orienta engine paused (Zero battery)")
    }

    fun toggleGuard(enabled: Boolean) {
        controller.setGuardEnabled(enabled)
        if (enabled && !runtimeState.value.permissions.isRotationReady) {
            emitMessage("Grant 'Display over other apps' so Guard can lock rotation in all apps")
        } else {
            emitMessage(if (enabled) "Global Guard armed" else "Global Guard paused")
        }
    }

    fun selectRotationMode(mode: RotationMode) {
        controller.setGlobalRotationMode(mode, autoEnableGuard = mode.isForcedLock)
        if (mode.isForcedLock && !runtimeState.value.permissions.isRotationReady) {
            emitMessage("Mode set to ${mode.title} — grant Overlay permission to apply globally")
        }
    }

    fun selectImmersiveMode(mode: ImmersiveMode) {
        controller.setGlobalImmersiveMode(mode)
        if (mode.isImmersiveActive && !runtimeState.value.permissions.isRotationReady) {
            emitMessage("${mode.title} selected — grant Overlay or Accessibility to apply across apps")
        }
    }

    fun applyPreset(preset: DeckPreset) {
        controller.applyPreset(preset)
        emitMessage("Applied preset: ${preset.title}")
    }

    fun setFloatingDeckEnabled(enabled: Boolean) {
        controller.setFloatingDeckEnabled(enabled)
        if (enabled && !runtimeState.value.permissions.hasOverlay) {
            emitMessage("Grant 'Display over other apps' to show the Floating Mini-Deck")
        }
    }

    fun setFloatingOpacity(opacity: Float) = controller.setFloatingOpacity(opacity)

    fun setFloatingSize(size: FloatingDeckSize) = controller.setFloatingSize(size)

    fun setFloatingSnapToEdge(enabled: Boolean) = controller.setFloatingSnapToEdge(enabled)

    fun setNotificationBarEnabled(enabled: Boolean) = controller.setNotificationBarEnabled(enabled)

    fun setStartOnBoot(enabled: Boolean) = controller.setStartOnBoot(enabled)

    fun setSyncSystemAutoRotate(enabled: Boolean) = controller.setSyncSystemAutoRotate(enabled)

    fun setOledEdgeMaskEnabled(enabled: Boolean) = controller.setOledEdgeMaskEnabled(enabled)

    fun setHapticsEnabled(enabled: Boolean) = controller.setHapticsEnabled(enabled)

    fun saveAppRule(
        packageName: String,
        appLabel: String,
        rotationMode: RotationMode,
        immersiveMode: ImmersiveMode,
        presetTag: String? = null
    ) {
        controller.saveAppRule(
            packageName = packageName,
            appLabel = appLabel,
            rotationMode = rotationMode,
            immersiveMode = immersiveMode,
            isEnabled = true,
            presetTag = presetTag
        )
        val hint = if (!runtimeState.value.permissions.hasAccessibility) {
            "Saved rule for $appLabel (enable Accessibility for auto-switching)"
        } else {
            "Saved auto-rule for $appLabel"
        }
        emitMessage(hint)
    }

    fun toggleAppRuleEnabled(packageName: String, enabled: Boolean) {
        controller.toggleAppRuleEnabled(packageName, enabled)
    }

    fun deleteAppRule(packageName: String, appLabel: String) {
        controller.deleteAppRule(packageName)
        emitMessage("Removed rule for $appLabel")
    }

    fun disableAllOverrides() {
        controller.disableAllOverrides()
        emitMessage("All overrides cleared")
    }

    fun notifyMessage(message: String) {
        emitMessage(message)
    }

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }
}
