package com.example.model

import android.content.pm.ActivityInfo

enum class RotationMode(
    val shortLabel: String,
    val title: String,
    val angleBadge: String,
    val description: String,
    val screenOrientation: Int,
    val previewDegrees: Float
) {
    AUTO_SYSTEM(
        shortLabel = "AUTO",
        title = "Auto System",
        angleBadge = "SYS",
        description = "Follow system sensor and default app behavior",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        previewDegrees = 0f
    ),
    FULL_SENSOR(
        shortLabel = "4-WAY",
        title = "Full Sensor",
        angleBadge = "360°",
        description = "Rotate freely in all 4 orientations including upside-down",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
        previewDegrees = 0f
    ),
    PORTRAIT(
        shortLabel = "PORT",
        title = "Portrait",
        angleBadge = "0°",
        description = "Force upright vertical lock across all apps",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        previewDegrees = 0f
    ),
    REVERSE_PORTRAIT(
        shortLabel = "R-PORT",
        title = "Rev Portrait",
        angleBadge = "180°",
        description = "Force inverted upside-down portrait orientation",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        previewDegrees = 180f
    ),
    SENSOR_PORTRAIT(
        shortLabel = "S-PORT",
        title = "Sensor Port",
        angleBadge = "0°/180°",
        description = "Flip automatically between upright and reverse portrait",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
        previewDegrees = 0f
    ),
    LANDSCAPE(
        shortLabel = "LAND",
        title = "Landscape",
        angleBadge = "90°",
        description = "Force horizontal landscape lock across any app",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        previewDegrees = 90f
    ),
    REVERSE_LANDSCAPE(
        shortLabel = "R-LAND",
        title = "Rev Land",
        angleBadge = "270°",
        description = "Force flipped horizontal landscape orientation",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        previewDegrees = 270f
    ),
    SENSOR_LANDSCAPE(
        shortLabel = "S-LAND",
        title = "Sensor Land",
        angleBadge = "90°/270°",
        description = "Flip automatically between left and right landscape",
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        previewDegrees = 90f
    );

    val isForcedLock: Boolean
        get() = this != AUTO_SYSTEM

    fun nextQuickCycle(): RotationMode = when (this) {
        AUTO_SYSTEM -> PORTRAIT
        PORTRAIT -> LANDSCAPE
        LANDSCAPE -> FULL_SENSOR
        FULL_SENSOR -> REVERSE_PORTRAIT
        REVERSE_PORTRAIT -> REVERSE_LANDSCAPE
        REVERSE_LANDSCAPE -> SENSOR_LANDSCAPE
        SENSOR_LANDSCAPE -> SENSOR_PORTRAIT
        SENSOR_PORTRAIT -> AUTO_SYSTEM
    }

    companion object {
        fun fromNameOrDefault(name: String?): RotationMode =
            entries.firstOrNull { it.name == name } ?: AUTO_SYSTEM
    }
}

enum class ImmersiveMode(
    val shortLabel: String,
    val title: String,
    val badge: String,
    val description: String,
    val hidesStatusBar: Boolean,
    val hidesNavBar: Boolean,
    val policyControlValue: String
) {
    NORMAL(
        shortLabel = "OFF",
        title = "Normal Bars",
        badge = "STD",
        description = "Show standard system status and navigation bars",
        hidesStatusBar = false,
        hidesNavBar = false,
        policyControlValue = ""
    ),
    HIDE_STATUS(
        shortLabel = "STATUS",
        title = "Hide Status",
        badge = "TOP",
        description = "Conceal top status clock and icons while keeping navigation",
        hidesStatusBar = true,
        hidesNavBar = false,
        policyControlValue = "immersive.status=*"
    ),
    HIDE_NAVIGATION(
        shortLabel = "NAV",
        title = "Hide Nav Bar",
        badge = "BTM",
        description = "Conceal bottom gesture pill or navigation buttons",
        hidesStatusBar = false,
        hidesNavBar = true,
        policyControlValue = "immersive.navigation=*"
    ),
    FULL_IMMERSIVE(
        shortLabel = "FULL",
        title = "Full Immersive",
        badge = "MAX",
        description = "Edge-to-edge immersion hiding both status and navigation bars",
        hidesStatusBar = true,
        hidesNavBar = true,
        policyControlValue = "immersive.full=*"
    );

    val isImmersiveActive: Boolean
        get() = this != NORMAL

    fun nextCycle(): ImmersiveMode = when (this) {
        NORMAL -> FULL_IMMERSIVE
        FULL_IMMERSIVE -> HIDE_STATUS
        HIDE_STATUS -> HIDE_NAVIGATION
        HIDE_NAVIGATION -> NORMAL
    }

    companion object {
        fun fromNameOrDefault(name: String?): ImmersiveMode =
            entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

enum class FloatingDeckSize(val label: String, val handleDp: Int) {
    COMPACT("Compact", 40),
    STANDARD("Standard", 48),
    LARGE("Large", 56);

    companion object {
        fun fromNameOrDefault(name: String?): FloatingDeckSize =
            entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

data class DeckPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val rotationMode: RotationMode,
    val immersiveMode: ImmersiveMode,
    val enableGuard: Boolean
)

object BuiltInPresets {
    val all: List<DeckPreset> = listOf(
        DeckPreset(
            id = "cinema",
            title = "Cinema",
            subtitle = "Sensor Land + Full",
            rotationMode = RotationMode.SENSOR_LANDSCAPE,
            immersiveMode = ImmersiveMode.FULL_IMMERSIVE,
            enableGuard = true
        ),
        DeckPreset(
            id = "gaming",
            title = "Gaming",
            subtitle = "Lock Land + Full",
            rotationMode = RotationMode.LANDSCAPE,
            immersiveMode = ImmersiveMode.FULL_IMMERSIVE,
            enableGuard = true
        ),
        DeckPreset(
            id = "reader",
            title = "Reader",
            subtitle = "Portrait + Hide Top",
            rotationMode = RotationMode.PORTRAIT,
            immersiveMode = ImmersiveMode.HIDE_STATUS,
            enableGuard = true
        ),
        DeckPreset(
            id = "desk_dock",
            title = "Desk Dock",
            subtitle = "4-Way Sensor + Std",
            rotationMode = RotationMode.FULL_SENSOR,
            immersiveMode = ImmersiveMode.NORMAL,
            enableGuard = true
        ),
        DeckPreset(
            id = "reset",
            title = "System Std",
            subtitle = "Auto + Normal Bars",
            rotationMode = RotationMode.AUTO_SYSTEM,
            immersiveMode = ImmersiveMode.NORMAL,
            enableGuard = false
        )
    )
}

data class DeckPreferences(
    val masterEnabled: Boolean = true,
    val guardEnabled: Boolean = false,
    val globalRotationMode: RotationMode = RotationMode.AUTO_SYSTEM,
    val globalImmersiveMode: ImmersiveMode = ImmersiveMode.NORMAL,
    val notificationBarEnabled: Boolean = true,
    val floatingDeckEnabled: Boolean = false,
    val floatingOpacity: Float = 0.88f,
    val floatingSize: FloatingDeckSize = FloatingDeckSize.STANDARD,
    val floatingSnapToEdge: Boolean = true,
    val startOnBoot: Boolean = false,
    val syncSystemAutoRotate: Boolean = true,
    val oledEdgeMaskEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true
)

data class PermissionDiagnostics(
    val hasOverlay: Boolean = false,
    val hasNotification: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasWriteSettings: Boolean = false,
    val hasWriteSecureSettings: Boolean = false
) {
    val coreReadyCount: Int
        get() = listOf(hasOverlay, hasNotification, hasAccessibility).count { it }

    val isRotationReady: Boolean
        get() = hasOverlay || hasAccessibility

    val isFullyConfigured: Boolean
        get() = hasOverlay && hasNotification && hasAccessibility
}

data class DeckRuntimeState(
    val preferences: DeckPreferences = DeckPreferences(),
    val effectiveRotationMode: RotationMode = RotationMode.AUTO_SYSTEM,
    val effectiveImmersiveMode: ImmersiveMode = ImmersiveMode.NORMAL,
    val activeSourceLabel: String = "Standby",
    val activeAppRulePackage: String? = null,
    val foregroundPackage: String? = null,
    val foregroundAppLabel: String? = null,
    val isServiceRunning: Boolean = false,
    val permissions: PermissionDiagnostics = PermissionDiagnostics()
)

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean
)
