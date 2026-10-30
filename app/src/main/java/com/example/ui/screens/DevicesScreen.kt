package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionType
import com.example.data.model.SmartWatchBrand
import com.example.engine.DiscoveredBleDevice
import com.example.engine.ThirdPartyPlatform
import com.example.ui.components.SmartWatchItemCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HealthSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: HealthSyncViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isBleScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredBleDevices.collectAsState()
    val platforms by viewModel.thirdPartyPlatforms.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("devices_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isScanning) viewModel.stopBleScan() else viewModel.startBleScan()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_scan_ble"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color(0xFF22354E) else CyanNeon,
                        contentColor = if (isScanning) CyanNeon else Color(0xFF090E17)
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.BluetoothSearching,
                        contentDescription = "Quét BLE",
                        modifier = if (isScanning) Modifier.rotate(scanRotation).size(18.dp) else Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Đang tìm BLE…" else "Quét đồng hồ BLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_add_device")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm thiết bị",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Thêm", color = CyanNeon, fontSize = 12.sp)
                }
            }
        }

        // Section: Paired Devices
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đồng hồ đã ghép nối (${devices.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hỗ trợ đa kết nối đồng thời",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (devices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF101926),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có thiết bị nào được ghép nối",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhấn 'Quét đồng hồ BLE' để tìm hoặc thêm từ danh sách",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(devices, key = { it.id }) { device ->
                SmartWatchItemCard(
                    device = device,
                    onToggleConnection = { viewModel.toggleDeviceConnection(device) },
                    onSetPrimary = { viewModel.setPrimaryDevice(device.id) },
                    onRemove = { viewModel.removeDevice(device) }
                )
            }
        }

        // Section: Discovered BLE Devices
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tìm thấy thiết bị xung quanh (${discoveredDevices.size})",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isScanning) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(CyanNeon, CircleShape)
                            )
                        }
                    }
                }

                if (scanError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = scanError ?: "",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (discoveredDevices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B131E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D42)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nhấn 'Quét đồng hồ BLE' để dò tìm đồng hồ Garmin, Apple Watch, Galaxy Watch hoặc Smart Band xung quanh qua Bluetooth LE.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(discoveredDevices) { discovered ->
                DiscoveredDeviceRow(
                    device = discovered,
                    onPair = { viewModel.pairDiscoveredDevice(discovered) }
                )
            }
        }

        // Section: Third-party health platform sync bridges
        item {
            Column {
                Text(
                    text = "Cầu nối ứng dụng sức khỏe bên thứ 3",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Đồng bộ qua API nền tảng để lấy dữ liệu cảm biến chuyên sâu",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        items(platforms, key = { it.id }) { platform ->
            ThirdPartyPlatformCard(
                platform = platform,
                onToggle = { viewModel.toggleThirdPartyPlatform(platform) },
                onOpenApp = { viewModel.openThirdPartyApp(platform) }
            )
        }
    }

    // Add Device Dialog
    if (showAddDialog) {
        AddCustomDeviceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, brand, connType ->
                viewModel.addCustomDevice(name, brand, connType)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DiscoveredDeviceRow(
    device: DiscoveredBleDevice,
    onPair: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF101926),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CyanNeon.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${device.brand.displayName} • ${device.address} • ${device.rssi} dBm",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onPair,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = Color(0xFF090E17)
                )
            ) {
                Text(
                    text = "Ghép nối",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ThirdPartyPlatformCard(
    platform: ThirdPartyPlatform,
    onToggle: () -> Unit,
    onOpenApp: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF101926),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (platform.isLinked) EmeraldNeon.copy(alpha = 0.4f) else Color(0xFF22354E)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(platform.brand.brandColorHex).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = platform.name,
                            tint = if (platform.brand.brandColorHex == 0xFFE2E8F0) CyanNeon else Color(platform.brand.brandColorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = platform.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = platform.lastSyncFormatted,
                            color = if (platform.isLinked) EmeraldNeon else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = platform.isLinked,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldNeon,
                        checkedTrackColor = EmeraldNeon.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF1E2D42)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = platform.description,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dữ liệu: ${platform.syncFeatures.joinToString(" • ")}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onOpenApp) {
                    Text(
                        text = "Mở ứng dụng",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: SmartWatchBrand, connectionType: ConnectionType) -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf(SmartWatchBrand.GARMIN) }
    var selectedType by remember { mutableStateOf(ConnectionType.BLUETOOTH_LE) }
    var brandExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm đồng hồ thông minh",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Tên thiết bị (vd: Garmin Fenix 7)") },
                    placeholder = { Text("Để trống để dùng tên mặc định") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        focusedLabelColor = CyanNeon,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Brand Selector
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBrand.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hãng sản xuất") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        SmartWatchBrand.values().forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand.displayName) },
                                onClick = {
                                    selectedBrand = brand
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                // Connection protocol
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = ConnectionType.BLUETOOTH_LE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == ConnectionType.BLUETOOTH_LE) CyanNeon else Color(0xFF1E2D42),
                            contentColor = if (selectedType == ConnectionType.BLUETOOTH_LE) Color(0xFF090E17) else TextPrimary
                        )
                    ) {
                        Text("Bluetooth LE", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { selectedType = ConnectionType.THIRD_PARTY_APP },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == ConnectionType.THIRD_PARTY_APP) CyanNeon else Color(0xFF1E2D42),
                            contentColor = if (selectedType == ConnectionType.THIRD_PARTY_APP) Color(0xFF090E17) else TextPrimary
                        )
                    ) {
                        Text("App bên thứ 3", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deviceName, selectedBrand, selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF090E17))
            ) {
                Text("Xác nhận ghép nối", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF101926)
    )
}
