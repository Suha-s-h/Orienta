package com.example.service

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.model.PermissionDiagnostics

object PermissionInspector {

    fun inspect(context: Context): PermissionDiagnostics {
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val hasAccessibility = isAccessibilityServiceEnabled(context)
        val hasWriteSettings = Settings.System.canWrite(context)
        val hasWriteSecureSettings = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        return PermissionDiagnostics(
            hasOverlay = hasOverlay,
            hasNotification = hasNotification,
            hasAccessibility = hasAccessibility,
            hasWriteSettings = hasWriteSettings,
            hasWriteSecureSettings = hasWriteSecureSettings
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (ForegroundAppAccessibilityService.isRunning) return true
        val expectedComponent = ComponentName(
            context.packageName,
            ForegroundAppAccessibilityService::class.java.name
        ).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices
            .split(':')
            .any { entry ->
                entry.equals(expectedComponent, ignoreCase = true) ||
                    entry.endsWith("/${ForegroundAppAccessibilityService::class.java.name}", ignoreCase = true)
            }
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStartActivity(context, intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStartActivity(context, intent)
    }

    fun openWriteSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStartActivity(context, intent)
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStartActivity(context, intent)
    }

    private fun safeStartActivity(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                // Ignore if device has no settings activity
            }
        }
    }
}
