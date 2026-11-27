package com.example

import com.example.model.RotationMode
import com.example.service.SystemScreenBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemScreenBridgeTest {

    @Test
    fun `inactive guard never writes to system auto-rotate`() {
        for (mode in RotationMode.entries) {
            assertNull(
                SystemScreenBridge.resolveTargetAutoRotateValue(
                    rotationMode = mode,
                    guardActive = false,
                    syncEnabled = true
                )
            )
        }
    }

    @Test
    fun `disabled sync never writes to system auto-rotate`() {
        for (mode in RotationMode.entries) {
            assertNull(
                SystemScreenBridge.resolveTargetAutoRotateValue(
                    rotationMode = mode,
                    guardActive = true,
                    syncEnabled = false
                )
            )
        }
    }

    @Test
    fun `active guard maps fixed and sensor modes accurately`() {
        assertEquals(0, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.PORTRAIT, true, true))
        assertEquals(0, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.REVERSE_PORTRAIT, true, true))
        assertEquals(0, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.LANDSCAPE, true, true))
        assertEquals(0, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.REVERSE_LANDSCAPE, true, true))

        assertEquals(1, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.AUTO_SYSTEM, true, true))
        assertEquals(1, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.FULL_SENSOR, true, true))
        assertEquals(1, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.SENSOR_PORTRAIT, true, true))
        assertEquals(1, SystemScreenBridge.resolveTargetAutoRotateValue(RotationMode.SENSOR_LANDSCAPE, true, true))
    }
}
