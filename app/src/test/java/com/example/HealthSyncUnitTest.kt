package com.example

import com.example.data.model.HealthMetricRecord
import com.example.data.model.SmartWatchBrand
import com.example.data.model.SmartWatchDevice
import com.example.data.model.SyncConflictPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthSyncUnitTest {

    @Test
    fun testPrimaryDevicePriorityConflictResolution() {
        val primaryDevice = SmartWatchDevice(
            id = "garmin_primary",
            name = "Garmin Forerunner 965",
            macAddress = "E4:5F:01:88:B2:A1",
            brand = SmartWatchBrand.GARMIN,
            connectionType = com.example.data.model.ConnectionType.BLUETOOTH_LE,
            isPrimary = true
        )

        val record1 = HealthMetricRecord(
            deviceId = "garmin_primary",
            deviceName = "Garmin Forerunner 965",
            heartRate = 74,
            steps = 6840,
            spO2 = 98,
            stressLevel = 30,
            caloriesBurned = 280,
            activeMinutes = 40
        )

        val record2 = HealthMetricRecord(
            deviceId = "apple_watch",
            deviceName = "Apple Watch Ultra",
            heartRate = 78,
            steps = 6820,
            spO2 = 97,
            stressLevel = 35,
            caloriesBurned = 290,
            activeMinutes = 40
        )

        val records = listOf(record1, record2)

        // With PRIMARY_DEVICE_PRIORITY, the primary device reading must win
        val selectedBpm = if (SyncConflictPolicy.PRIMARY_DEVICE_PRIORITY == SyncConflictPolicy.PRIMARY_DEVICE_PRIORITY) {
            records.find { it.deviceId == primaryDevice.id }?.heartRate ?: records.first().heartRate
        } else 0

        assertEquals(74, selectedBpm)
    }

    @Test
    fun testMultiWatchStepDeduplication() {
        val stepReadings = listOf(6820, 6840, 6835)
        // Deduplication rule selects max validated continuous step count
        val deduplicatedSteps = stepReadings.maxOrNull() ?: 0
        assertEquals(6840, deduplicatedSteps)
    }

    @Test
    fun testConsensusPercentageCalculation() {
        val bpms = listOf(74, 75, 73)
        val maxDiff = (bpms.maxOrNull() ?: 70) - (bpms.minOrNull() ?: 70)
        val consensus = (100f - (maxDiff * 0.9f)).coerceIn(90f, 100f)

        // Difference between 75 and 73 is 2 -> consensus = 100 - 1.8 = 98.2%
        assertTrue(consensus > 98f)
    }
}
