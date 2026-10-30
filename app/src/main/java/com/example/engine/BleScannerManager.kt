package com.example.engine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.data.model.ConnectionType
import com.example.data.model.SmartWatchBrand
import com.example.data.model.SmartWatchDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Standard Bluetooth SIG Health Profiles
val UUID_HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
val UUID_BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
val UUID_HEALTH_THERMOMETER: UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
val UUID_PULSE_OXIMETER: UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")

data class DiscoveredBleDevice(
    val address: String,
    val name: String,
    val brand: SmartWatchBrand,
    val rssi: Int,
    val isConnectable: Boolean = true,
    val services: List<String> = emptyList(),
    val rawBluetoothDevice: BluetoothDevice? = null
)

class BleScannerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var scanCallback: ScanCallback? = null

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val connect = ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            return scan && connect
        } else {
            val location = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            return location
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        _scanError.value = null

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = "Bluetooth chưa được bật trên thiết bị."
            populateDefaultWearableCatalog()
            return
        }

        if (!hasPermissions()) {
            _scanError.value = "Chưa cấp quyền Bluetooth/Vị trí để quét thiết bị."
            populateDefaultWearableCatalog()
            return
        }

        try {
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                _scanError.value = "Không tìm thấy bộ quét Bluetooth LE."
                populateDefaultWearableCatalog()
                return
            }

            _discoveredDevices.value = emptyList()
            _isScanning.value = true

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.let { handleScanResult(it) }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    results?.forEach { handleScanResult(it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    _isScanning.value = false
                    _scanError.value = "Quét BLE thất bại (mã: $errorCode). Đã chuyển sang danh mục đề xuất."
                    populateDefaultWearableCatalog()
                }
            }

            scanner.startScan(null, settings, scanCallback)

            // Auto timeout scan after 15 seconds
            scope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(15000)
                stopScan()
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
            _scanError.value = "Quyền quét Bluetooth bị từ chối."
            populateDefaultWearableCatalog()
        } catch (e: Exception) {
            _isScanning.value = false
            _scanError.value = "Lỗi khi quét: ${e.localizedMessage}"
            populateDefaultWearableCatalog()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore
        } finally {
            _isScanning.value = false
            scanCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val deviceName = try { device.name } catch (e: SecurityException) { null } ?: "Thiết bị BLE (${device.address.takeLast(5)})"
        val rssi = result.rssi
        val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

        val brand = detectBrand(deviceName)

        val discovered = DiscoveredBleDevice(
            address = device.address,
            name = deviceName,
            brand = brand,
            rssi = rssi,
            services = serviceUuids,
            rawBluetoothDevice = device
        )

        val currentList = _discoveredDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            currentList[index] = discovered
        } else {
            currentList.add(discovered)
        }
        _discoveredDevices.value = currentList
    }

    private fun detectBrand(name: String): SmartWatchBrand {
        val lower = name.lowercase()
        return when {
            lower.contains("garmin") || lower.contains("forerunner") || lower.contains("fenix") || lower.contains("venu") -> SmartWatchBrand.GARMIN
            lower.contains("apple") || lower.contains("watch") && (lower.contains("series") || lower.contains("ultra")) -> SmartWatchBrand.APPLE
            lower.contains("galaxy") || lower.contains("samsung") || lower.contains("gear") -> SmartWatchBrand.SAMSUNG
            lower.contains("mi band") || lower.contains("amazfit") || lower.contains("xiaomi") -> SmartWatchBrand.XIAOMI_AMAZFIT
            lower.contains("fitbit") || lower.contains("sense") || lower.contains("versa") || lower.contains("charge") -> SmartWatchBrand.FITBIT
            lower.contains("polar") -> SmartWatchBrand.POLAR
            lower.contains("huawei") || lower.contains("honor") -> SmartWatchBrand.HUAWEI
            else -> SmartWatchBrand.GENERIC_BLE
        }
    }

    fun populateDefaultWearableCatalog() {
        val catalog = listOf(
            DiscoveredBleDevice("E4:5F:01:88:B2:A1", "Garmin Forerunner 965", SmartWatchBrand.GARMIN, -52),
            DiscoveredBleDevice("F2:2C:99:34:10:8B", "Apple Watch Ultra 2 (BLE Bridge)", SmartWatchBrand.APPLE, -58),
            DiscoveredBleDevice("C8:02:8D:11:43:5F", "Samsung Galaxy Watch 6 Pro", SmartWatchBrand.SAMSUNG, -61),
            DiscoveredBleDevice("D1:9A:33:F2:70:12", "Xiaomi Smart Band 8 Pro", SmartWatchBrand.XIAOMI_AMAZFIT, -65),
            DiscoveredBleDevice("A0:B1:C2:D3:E4:F5", "Fitbit Sense 2 Health", SmartWatchBrand.FITBIT, -68),
            DiscoveredBleDevice("74:D2:1D:90:3E:44", "Polar H10 Heart Rate Strap", SmartWatchBrand.POLAR, -49)
        )
        val existing = _discoveredDevices.value.toMutableList()
        catalog.forEach { sample ->
            if (existing.none { it.address == sample.address }) {
                existing.add(sample)
            }
        }
        _discoveredDevices.value = existing
    }
}
