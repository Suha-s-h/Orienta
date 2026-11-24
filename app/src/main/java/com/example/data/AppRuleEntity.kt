package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ImmersiveMode
import com.example.model.RotationMode

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val rotationModeName: String = RotationMode.AUTO_SYSTEM.name,
    val immersiveModeName: String = ImmersiveMode.NORMAL.name,
    val isEnabled: Boolean = true,
    val presetTag: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val rotationMode: RotationMode
        get() = RotationMode.fromNameOrDefault(rotationModeName)

    val immersiveMode: ImmersiveMode
        get() = ImmersiveMode.fromNameOrDefault(immersiveModeName)
}
