package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DeckPreferences
import com.example.model.FloatingDeckSize
import com.example.model.ImmersiveMode
import com.example.model.RotationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.screenDeckDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "screendeck_prefs"
)

class DeckPreferencesStore(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val GUARD_ENABLED = booleanPreferencesKey("guard_enabled")
        val GLOBAL_ROTATION = stringPreferencesKey("global_rotation")
        val GLOBAL_IMMERSIVE = stringPreferencesKey("global_immersive")
        val NOTIFICATION_BAR = booleanPreferencesKey("notification_bar")
        val FLOATING_ENABLED = booleanPreferencesKey("floating_enabled")
        val FLOATING_OPACITY = floatPreferencesKey("floating_opacity")
        val FLOATING_SIZE = stringPreferencesKey("floating_size")
        val FLOATING_SNAP = booleanPreferencesKey("floating_snap")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val SYNC_AUTO_ROTATE = booleanPreferencesKey("sync_auto_rotate")
        val OLED_EDGE_MASK = booleanPreferencesKey("oled_edge_mask")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val preferencesFlow: Flow<DeckPreferences> = context.screenDeckDataStore.data.map { prefs ->
        prefs.toDeckPreferences()
    }

    suspend fun update(transform: (DeckPreferences) -> DeckPreferences) {
        context.screenDeckDataStore.edit { mutable ->
            val current = mutable.toDeckPreferences()
            val next = transform(current)
            mutable[Keys.MASTER_ENABLED] = next.masterEnabled
            mutable[Keys.GUARD_ENABLED] = next.guardEnabled
            mutable[Keys.GLOBAL_ROTATION] = next.globalRotationMode.name
            mutable[Keys.GLOBAL_IMMERSIVE] = next.globalImmersiveMode.name
            mutable[Keys.NOTIFICATION_BAR] = next.notificationBarEnabled
            mutable[Keys.FLOATING_ENABLED] = next.floatingDeckEnabled
            mutable[Keys.FLOATING_OPACITY] = next.floatingOpacity.coerceIn(0.35f, 1.0f)
            mutable[Keys.FLOATING_SIZE] = next.floatingSize.name
            mutable[Keys.FLOATING_SNAP] = next.floatingSnapToEdge
            mutable[Keys.START_ON_BOOT] = next.startOnBoot
            mutable[Keys.SYNC_AUTO_ROTATE] = next.syncSystemAutoRotate
            mutable[Keys.OLED_EDGE_MASK] = next.oledEdgeMaskEnabled
            mutable[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
        }
    }

    private fun Preferences.toDeckPreferences(): DeckPreferences = DeckPreferences(
        masterEnabled = this[Keys.MASTER_ENABLED] ?: true,
        guardEnabled = this[Keys.GUARD_ENABLED] ?: false,
        globalRotationMode = RotationMode.fromNameOrDefault(this[Keys.GLOBAL_ROTATION]),
        globalImmersiveMode = ImmersiveMode.fromNameOrDefault(this[Keys.GLOBAL_IMMERSIVE]),
        notificationBarEnabled = this[Keys.NOTIFICATION_BAR] ?: true,
        floatingDeckEnabled = this[Keys.FLOATING_ENABLED] ?: false,
        floatingOpacity = (this[Keys.FLOATING_OPACITY] ?: 0.88f).coerceIn(0.35f, 1.0f),
        floatingSize = FloatingDeckSize.fromNameOrDefault(this[Keys.FLOATING_SIZE]),
        floatingSnapToEdge = this[Keys.FLOATING_SNAP] ?: true,
        startOnBoot = this[Keys.START_ON_BOOT] ?: false,
        syncSystemAutoRotate = this[Keys.SYNC_AUTO_ROTATE] ?: true,
        oledEdgeMaskEnabled = this[Keys.OLED_EDGE_MASK] ?: false,
        hapticsEnabled = this[Keys.HAPTICS_ENABLED] ?: true
    )
}
