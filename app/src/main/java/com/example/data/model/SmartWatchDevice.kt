package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SmartWatchBrand(val displayName: String, val brandColorHex: Long) {
    GARMIN("Garmin", 0xFF007CC3),
    APPLE("Apple Watch", 0xFFE2E8F0),
    SAMSUNG("Samsung Galaxy Watch", 0xFF1428A0),
    XIAOMI_AMAZFIT("Xiaomi / Amazfit", 0xFFFF6900),
    FITBIT("Fitbit / Google", 0xFF00B0B9),
    POLAR("Polar", 0xFFD81E05),
    HUAWEI("Huawei Watch", 0xFFCF0A2C),
    GENERIC_BLE("Smart Band / BLE", 0xFF00E5FF)
}

enum class ConnectionType(val label: String) {
    BLUETOOTH_LE("Bluetooth LE"),
    THIRD_PARTY_APP("Ứng dụng bên thứ 3")
}

enum class ConnectionStatus(val label: String) {
    CONNECTED("Đã kết nối"),
    CONNECTING("Đang kết nối…"),
    DISCONNECTED("Ngắt kết nối"),
    ERROR("Lỗi kết nối")
}

@Entity(tableName = "smartwatch_devices")
data class SmartWatchDevice(
    @PrimaryKey
    val id: String,
    val name: String,
    val macAddress: String,
    val brand: SmartWatchBrand,
    val connectionType: ConnectionType,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val batteryLevel: Int = 100, // 0..100%
    val rssi: Int = -60, // dBm
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isPrimary: Boolean = false,
    val syncIntervalSeconds: Int = 10,
    val firmwareVersion: String = "v2.4.1",
    val accuracyRating: Float = 98.8f, // accuracy percentage
    val sensorsSupported: String = "HEART_RATE,SPO2,STEPS,SLEEP,STRESS,CALORIES"
) {
    fun isConnected(): Boolean = status == ConnectionStatus.CONNECTED
}
