package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

object SystemScreenBridge {

    fun buildAdbGrantCommand(context: Context): String {
        return "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    }

    fun syncSystemAutoRotate(
        context: Context,
        rotationMode: RotationMode,
        guardActive: Boolean,
        syncEnabled: Boolean
    ) {
        if (!syncEnabled || !Settings.System.canWrite(context)) return
        try {
            val autoRotateValue = when {
                !guardActive -> 1
                rotationMode == RotationMode.AUTO_SYSTEM ||
                    rotationMode == RotationMode.FULL_SENSOR ||
                    rotationMode == RotationMode.SENSOR_PORTRAIT ||
                    rotationMode == RotationMode.SENSOR_LANDSCAPE -> 1
                else -> 0
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                autoRotateValue
            )
        } catch (_: SecurityException) {
            // Ignored when WRITE_SETTINGS is revoked
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    fun applyAdbImmersiveBoostIfPermitted(
        context: Context,
        immersiveMode: ImmersiveMode
    ): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        return try {
            val value = when (immersiveMode) {
                ImmersiveMode.NORMAL -> null
                ImmersiveMode.HIDE_STATUS -> "immersive.status=*"
                ImmersiveMode.HIDE_NAVIGATION -> "immersive.navigation=*"
                ImmersiveMode.FULL_IMMERSIVE -> "immersive.full=*"
            }
            Settings.Global.putString(
                context.contentResolver,
                "policy_control",
                value
            )
            true
        } catch (_: Exception) {
            try {
                if (immersiveMode == ImmersiveMode.NORMAL) {
                    Settings.Global.putString(context.contentResolver, "policy_control", "")
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
