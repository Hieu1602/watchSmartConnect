package com.example.data.repository

import com.example.data.local.HealthSyncDatabase
import com.example.data.model.AlertSeverity
import com.example.data.model.ConnectionStatus
import com.example.data.model.ConnectionType
import com.example.data.model.HealthAlert
import com.example.data.model.HealthMetricRecord
import com.example.data.model.SmartWatchBrand
import com.example.data.model.SmartWatchDevice
import com.example.data.model.SyncLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HealthSyncRepository(private val database: HealthSyncDatabase) {

    val allDevicesFlow: Flow<List<SmartWatchDevice>> = database.deviceDao().getAllDevicesFlow()
    val recentMetricsFlow: Flow<List<HealthMetricRecord>> = database.healthMetricDao().getRecentMetricsFlow()
    val syncLogsFlow: Flow<List<SyncLogEntry>> = database.syncLogDao().getRecentLogsFlow()
    val alertsFlow: Flow<List<HealthAlert>> = database.healthAlertDao().getAlertsFlow()

    suspend fun initializePreloadedDevicesIfEmpty() = withContext(Dispatchers.IO) {
        val existing = database.deviceDao().getAllDevices()
        if (existing.isEmpty()) {
            val initialDevices = listOf(
                SmartWatchDevice(
                    id = "watch_garmin_965",
                    name = "Garmin Forerunner 965",
                    macAddress = "E4:5F:01:88:B2:A1",
                    brand = SmartWatchBrand.GARMIN,
                    connectionType = ConnectionType.BLUETOOTH_LE,
                    status = ConnectionStatus.CONNECTED,
                    batteryLevel = 88,
                    rssi = -52,
                    lastSyncTimestamp = System.currentTimeMillis() - 2000,
                    isPrimary = true,
                    syncIntervalSeconds = 5,
                    firmwareVersion = "v18.27",
                    accuracyRating = 99.4f,
                    sensorsSupported = "HEART_RATE,SPO2,STEPS,SLEEP,STRESS,CALORIES,HRV"
                ),
                SmartWatchDevice(
                    id = "watch_apple_ultra2",
                    name = "Apple Watch Ultra 2",
                    macAddress = "F2:2C:99:34:10:8B",
                    brand = SmartWatchBrand.APPLE,
                    connectionType = ConnectionType.THIRD_PARTY_APP,
                    status = ConnectionStatus.CONNECTED,
                    batteryLevel = 74,
                    rssi = -58,
                    lastSyncTimestamp = System.currentTimeMillis() - 4000,
                    isPrimary = false,
                    syncIntervalSeconds = 10,
                    firmwareVersion = "watchOS 10.5",
                    accuracyRating = 99.2f,
                    sensorsSupported = "HEART_RATE,ECG,SPO2,STEPS,SLEEP,CALORIES"
                ),
                SmartWatchDevice(
                    id = "watch_galaxy_6",
                    name = "Samsung Galaxy Watch 6 Pro",
                    macAddress = "C8:02:8D:11:43:5F",
                    brand = SmartWatchBrand.SAMSUNG,
                    connectionType = ConnectionType.THIRD_PARTY_APP,
                    status = ConnectionStatus.DISCONNECTED,
                    batteryLevel = 92,
                    rssi = -61,
                    lastSyncTimestamp = System.currentTimeMillis() - 3600000,
                    isPrimary = false,
                    syncIntervalSeconds = 15,
                    firmwareVersion = "One UI 5.0",
                    accuracyRating = 98.7f,
                    sensorsSupported = "HEART_RATE,SPO2,STEPS,BLOOD_PRESSURE,SLEEP"
                )
            )
            database.deviceDao().insertAll(initialDevices)

            // Seed initial health alerts and logs
            database.healthAlertDao().insert(
                HealthAlert(
                    title = "Đồng bộ đa thiết bị kích hoạt",
                    message = "Đã kết nối song song Garmin Forerunner 965 & Apple Watch Ultra 2. Chế độ so khớp dữ liệu theo thời gian thực đang chạy.",
                    severity = AlertSeverity.INFO,
                    metricType = "SYSTEM"
                )
            )

            database.syncLogDao().insert(
                SyncLogEntry(
                    timestamp = System.currentTimeMillis() - 5000,
                    deviceName = "Garmin Forerunner 965",
                    deviceBrand = "GARMIN",
                    metricTypes = "Nhịp tim, SpO2, Bước chân, Calo",
                    durationMs = 62,
                    latencyMs = 28,
                    packetCount = 18,
                    accuracy = 99.5f,
                    status = "SUCCESS",
                    message = "Khởi tạo kênh BLE GATT Heart Rate Profile thành công"
                )
            )
        }
    }

    suspend fun addDevice(device: SmartWatchDevice) = withContext(Dispatchers.IO) {
        database.deviceDao().insertOrUpdate(device)
    }

    suspend fun updateDeviceStatus(deviceId: String, status: ConnectionStatus) = withContext(Dispatchers.IO) {
        database.deviceDao().updateStatus(deviceId, status)
    }

    suspend fun setPrimaryDevice(deviceId: String) = withContext(Dispatchers.IO) {
        database.deviceDao().clearPrimaryFlag()
        database.deviceDao().setPrimary(deviceId)
    }

    suspend fun removeDevice(device: SmartWatchDevice) = withContext(Dispatchers.IO) {
        database.deviceDao().delete(device)
    }

    suspend fun markAlertRead(alertId: Long) = withContext(Dispatchers.IO) {
        database.healthAlertDao().markAsRead(alertId)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        database.syncLogDao().clearLogs()
    }
}
