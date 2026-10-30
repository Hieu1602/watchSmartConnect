package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.ConnectionType
import com.example.data.model.HealthMetricRecord
import com.example.data.model.SmartWatchBrand
import com.example.data.model.SmartWatchDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThirdPartyPlatform(
    val id: String,
    val name: String,
    val brand: SmartWatchBrand,
    val packageName: String,
    val description: String,
    val isLinked: Boolean,
    val lastSyncFormatted: String,
    val syncFeatures: List<String>
)

class ThirdPartySyncBridge(private val context: Context) {

    private val _platforms = MutableStateFlow<List<ThirdPartyPlatform>>(
        listOf(
            ThirdPartyPlatform(
                id = "health_connect",
                name = "Android Health Connect",
                brand = SmartWatchBrand.GENERIC_BLE,
                packageName = "com.google.android.apps.healthdata",
                description = "Nền tảng dữ liệu sức khỏe hợp nhất của Google và Android. Đồng bộ hai chiều giữa các ứng dụng.",
                isLinked = true,
                lastSyncFormatted = "1 phút trước",
                syncFeatures = listOf("Nhịp tim", "SpO2", "Bước chân", "Giấc ngủ", "Calo")
            ),
            ThirdPartyPlatform(
                id = "garmin_connect",
                name = "Garmin Connect™",
                brand = SmartWatchBrand.GARMIN,
                packageName = "com.garmin.android.apps.connectmobile",
                description = "Đồng bộ hóa dữ liệu từ hệ sinh thái đồng hồ Garmin qua Garmin Health API.",
                isLinked = true,
                lastSyncFormatted = "Vừa xong",
                syncFeatures = listOf("HRV", "Body Battery", "VO2 Max", "Nhịp tim thời gian thực")
            ),
            ThirdPartyPlatform(
                id = "samsung_health",
                name = "Samsung Health",
                brand = SmartWatchBrand.SAMSUNG,
                packageName = "com.sec.android.app.shealth",
                description = "Đồng bộ hóa Galaxy Watch (Wear OS) qua Samsung Health Data SDK.",
                isLinked = false,
                lastSyncFormatted = "Chưa kết nối",
                syncFeatures = listOf("ECG", "Huyết áp", "SpO2", "Bước chân")
            ),
            ThirdPartyPlatform(
                id = "strava",
                name = "Strava Fitness",
                brand = SmartWatchBrand.GENERIC_BLE,
                packageName = "com.strava",
                description = "Đồng bộ hóa các buổi tập luyện GPS và nhịp tim từ Strava.",
                isLinked = false,
                lastSyncFormatted = "Chưa kết nối",
                syncFeatures = listOf("Quãng đường GPS", "Tốc độ", "Calo", "Nhịp tim")
            ),
            ThirdPartyPlatform(
                id = "apple_health_bridge",
                name = "Apple Health Sync Bridge",
                brand = SmartWatchBrand.APPLE,
                packageName = "com.apple.health.bridge",
                description = "Cầu nối đồng bộ dữ liệu xuất XML / BLE Companion từ Apple Watch.",
                isLinked = true,
                lastSyncFormatted = "5 phút trước",
                syncFeatures = listOf("Nhịp tim liên tục", "Vòng vận động", "SpO2")
            )
        )
    )

    val platforms: StateFlow<List<ThirdPartyPlatform>> = _platforms.asStateFlow()

    fun togglePlatformLink(platformId: String): Boolean {
        val currentList = _platforms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == platformId }
        if (index >= 0) {
            val item = currentList[index]
            val newState = !item.isLinked
            currentList[index] = item.copy(
                isLinked = newState,
                lastSyncFormatted = if (newState) "Vừa kết nối" else "Chưa kết nối"
            )
            _platforms.value = currentList
            return newState
        }
        return false
    }

    fun openOrInstallApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            // Open Play Store
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    fun createDeviceFromPlatform(platform: ThirdPartyPlatform): SmartWatchDevice {
        return SmartWatchDevice(
            id = "ext_${platform.id}",
            name = platform.name,
            macAddress = "APP:${platform.id.uppercase()}",
            brand = platform.brand,
            connectionType = ConnectionType.THIRD_PARTY_APP,
            status = com.example.data.model.ConnectionStatus.CONNECTED,
            batteryLevel = 100,
            rssi = -30,
            lastSyncTimestamp = System.currentTimeMillis(),
            isPrimary = false,
            syncIntervalSeconds = 15,
            firmwareVersion = "API v3.2",
            accuracyRating = 99.5f,
            sensorsSupported = platform.syncFeatures.joinToString(",")
        )
    }
}
