package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HealthSyncApp
import com.example.data.model.AlertSeverity
import com.example.data.model.ConnectionStatus
import com.example.data.model.ConnectionType
import com.example.data.model.HealthAlert
import com.example.data.model.HealthMetricRecord
import com.example.data.model.SmartWatchBrand
import com.example.data.model.SmartWatchDevice
import com.example.data.model.SyncConflictPolicy
import com.example.data.model.SyncLogEntry
import com.example.engine.DiscoveredBleDevice
import com.example.engine.ThirdPartyPlatform
import com.example.service.ContinuousSyncService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HealthSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HealthSyncApp
    private val repository = app.repository
    private val syncEngine = app.syncEngine
    private val bleScanner = app.bleScanner
    private val thirdPartyBridge = app.thirdPartyBridge

    val devices: StateFlow<List<SmartWatchDevice>> = repository.allDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMetrics: StateFlow<List<HealthMetricRecord>> = repository.recentMetricsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntry>> = repository.syncLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<HealthAlert>> = repository.alertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthSummary = syncEngine.healthSummary
    val isSyncingNow = syncEngine.isSyncingNow
    val isContinuousSyncRunning = syncEngine.isContinuousSyncRunning
    val syncIntervalSeconds = syncEngine.syncIntervalSeconds
    val conflictPolicy = syncEngine.conflictPolicy
    val liveBpmWave = syncEngine.liveBpmWave

    val discoveredBleDevices: StateFlow<List<DiscoveredBleDevice>> = bleScanner.discoveredDevices
    val isBleScanning: StateFlow<Boolean> = bleScanner.isScanning
    val scanError: StateFlow<String?> = bleScanner.scanError

    val thirdPartyPlatforms: StateFlow<List<ThirdPartyPlatform>> = thirdPartyBridge.platforms

    fun triggerManualSync() {
        syncEngine.triggerManualSyncNow()
    }

    fun toggleContinuousSync(enable: Boolean) {
        syncEngine.toggleContinuousSync(enable)
        if (enable) {
            ContinuousSyncService.start(getApplication())
        } else {
            ContinuousSyncService.stop(getApplication())
        }
    }

    fun setSyncInterval(seconds: Int) {
        syncEngine.setSyncInterval(seconds)
    }

    fun setConflictPolicy(policy: SyncConflictPolicy) {
        syncEngine.setConflictPolicy(policy)
    }

    fun toggleDeviceConnection(device: SmartWatchDevice) {
        viewModelScope.launch {
            val nextStatus = when (device.status) {
                ConnectionStatus.CONNECTED -> ConnectionStatus.DISCONNECTED
                ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> ConnectionStatus.CONNECTED
                ConnectionStatus.CONNECTING -> ConnectionStatus.DISCONNECTED
            }
            repository.updateDeviceStatus(device.id, nextStatus)
            syncEngine.triggerManualSyncNow()
        }
    }

    fun setPrimaryDevice(deviceId: String) {
        viewModelScope.launch {
            repository.setPrimaryDevice(deviceId)
            syncEngine.triggerManualSyncNow()
        }
    }

    fun removeDevice(device: SmartWatchDevice) {
        viewModelScope.launch {
            repository.removeDevice(device)
        }
    }

    fun pairDiscoveredDevice(discovered: DiscoveredBleDevice) {
        viewModelScope.launch {
            val newDevice = SmartWatchDevice(
                id = "ble_${discovered.address.replace(":", "")}",
                name = discovered.name,
                macAddress = discovered.address,
                brand = discovered.brand,
                connectionType = ConnectionType.BLUETOOTH_LE,
                status = ConnectionStatus.CONNECTED,
                batteryLevel = (75..98).random(),
                rssi = discovered.rssi,
                lastSyncTimestamp = System.currentTimeMillis(),
                isPrimary = false,
                syncIntervalSeconds = 5,
                firmwareVersion = "v3.1.0",
                accuracyRating = when (discovered.brand) {
                    SmartWatchBrand.GARMIN -> 99.4f
                    SmartWatchBrand.APPLE -> 99.2f
                    SmartWatchBrand.POLAR -> 99.6f
                    SmartWatchBrand.SAMSUNG -> 98.9f
                    else -> 98.2f
                }
            )
            repository.addDevice(newDevice)
            syncEngine.triggerManualSyncNow()
        }
    }

    fun addCustomDevice(
        name: String,
        brand: SmartWatchBrand,
        connectionType: ConnectionType
    ) {
        viewModelScope.launch {
            val id = "dev_${UUID.randomUUID().toString().take(8)}"
            val mac = String.format(
                "%02X:%02X:%02X:%02X:%02X:%02X",
                (0..255).random(),
                (0..255).random(),
                (0..255).random(),
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
            val device = SmartWatchDevice(
                id = id,
                name = name.ifBlank { "${brand.displayName} Đồng hồ" },
                macAddress = mac,
                brand = brand,
                connectionType = connectionType,
                status = ConnectionStatus.CONNECTED,
                batteryLevel = (80..100).random(),
                rssi = (-70..-45).random(),
                lastSyncTimestamp = System.currentTimeMillis(),
                isPrimary = false,
                syncIntervalSeconds = 10,
                firmwareVersion = "v2.0",
                accuracyRating = 98.8f
            )
            repository.addDevice(device)
            syncEngine.triggerManualSyncNow()
        }
    }

    fun toggleThirdPartyPlatform(platform: ThirdPartyPlatform) {
        val newState = thirdPartyBridge.togglePlatformLink(platform.id)
        viewModelScope.launch {
            if (newState) {
                val linkedDevice = thirdPartyBridge.createDeviceFromPlatform(platform)
                repository.addDevice(linkedDevice)
            } else {
                val existing = repository.allDevicesFlow
                // Can disconnect linked device
            }
            syncEngine.triggerManualSyncNow()
        }
    }

    fun openThirdPartyApp(platform: ThirdPartyPlatform) {
        thirdPartyBridge.openOrInstallApp(platform.packageName)
    }

    fun startBleScan() {
        bleScanner.startScan()
    }

    fun stopBleScan() {
        bleScanner.stopScan()
    }

    fun dismissAlert(alertId: Long) {
        viewModelScope.launch {
            repository.markAlertRead(alertId)
        }
    }

    fun clearSyncLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
