package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.model.DeckPreferences
import com.example.model.ImmersiveMode
import com.example.model.InstalledAppItem
import com.example.model.RotationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScreenDeckRepository(
    private val context: Context,
    private val appRuleDao: AppRuleDao,
    private val preferencesStore: DeckPreferencesStore
) {
    val preferencesFlow: Flow<DeckPreferences> = preferencesStore.preferencesFlow
    val appRulesFlow: Flow<List<AppRuleEntity>> = appRuleDao.observeAllRules()

    suspend fun updatePreferences(transform: (DeckPreferences) -> DeckPreferences) {
        preferencesStore.update(transform)
    }

    suspend fun upsertAppRule(
        packageName: String,
        appLabel: String,
        rotationMode: RotationMode,
        immersiveMode: ImmersiveMode,
        isEnabled: Boolean = true,
        presetTag: String? = null
    ) {
        appRuleDao.upsertRule(
            AppRuleEntity(
                packageName = packageName,
                appLabel = appLabel,
                rotationModeName = rotationMode.name,
                immersiveModeName = immersiveMode.name,
                isEnabled = isEnabled,
                presetTag = presetTag,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setAppRuleEnabled(packageName: String, enabled: Boolean) {
        appRuleDao.setRuleEnabled(packageName, enabled)
    }

    suspend fun deleteAppRule(packageName: String) {
        appRuleDao.deleteRule(packageName)
    }

    suspend fun clearAllAppRules() {
        appRuleDao.clearAllRules()
    }

    suspend fun loadLaunchableApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val selfPkg = context.packageName
        resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
                val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                InstalledAppItem(
                    packageName = pkg,
                    label = label,
                    isSystemApp = isSystem
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun resolveAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }
}
