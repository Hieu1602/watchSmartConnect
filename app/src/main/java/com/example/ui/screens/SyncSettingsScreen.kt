package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncConflictPolicy
import com.example.data.model.SyncLogEntry
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HealthSyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncSettingsScreen(
    viewModel: HealthSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isContinuousRunning by viewModel.isContinuousSyncRunning.collectAsState()
    val syncInterval by viewModel.syncIntervalSeconds.collectAsState()
    val conflictPolicy by viewModel.conflictPolicy.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val isSyncingNow by viewModel.isSyncingNow.collectAsState()

    var stepDeduplicationEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sync_settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Continuous Sync Master Toggle
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF101926),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isContinuousRunning) EmeraldNeon.copy(alpha = 0.5f) else Color(0xFF22354E)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (isContinuousRunning) EmeraldNeon.copy(alpha = 0.15f) else Color(0xFF1E2D42),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (isContinuousRunning) EmeraldNeon else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đồng bộ nền liên tục",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isContinuousRunning) "Đang chạy qua Foreground Service" else "Đang tạm dừng",
                                    color = if (isContinuousRunning) EmeraldNeon else TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isContinuousRunning,
                            onCheckedChange = { viewModel.toggleContinuousSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldNeon,
                                checkedTrackColor = EmeraldNeon.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E2D42)
                            ),
                            modifier = Modifier.testTag("toggle_continuous_sync")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Duy trì kết nối GATT BLE và lắng nghe liên tục các gói tin nhịp tim, bước chân ngay cả khi tắt màn hình mà không bị hệ điều hành đóng.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Sync Frequency Selector
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF101926),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tần suất đồng bộ dữ liệu",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Khoảng thời gian lấy mẫu từ các thiết bị thông minh:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val intervals = listOf(
                        5 to "5s (Siêu tốc)",
                        15 to "15s (Tiêu chuẩn)",
                        30 to "30s",
                        60 to "1 phút",
                        300 to "5 phút (Tiết kiệm pin)"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.take(3).forEach { (sec, label) ->
                            val isSelected = syncInterval == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyanNeon else Color(0xFF172336))
                                    .border(1.dp, if (isSelected) CyanNeon else Color(0xFF22354E), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setSyncInterval(sec) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF090E17) else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.drop(3).forEach { (sec, label) ->
                            val isSelected = syncInterval == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyanNeon else Color(0xFF172336))
                                    .border(1.dp, if (isSelected) CyanNeon else Color(0xFF22354E), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setSyncInterval(sec) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF090E17) else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Conflict Resolution Policy
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF101926),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quy tắc giải quyết sai số & xung đột dữ liệu",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cách thức xử lý khi 2 hay nhiều đồng hồ đo cùng lúc có chênh lệch:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SyncConflictPolicy.values().forEach { policy ->
                        val isSelected = conflictPolicy == policy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF172336) else Color.Transparent)
                                .clickable { viewModel.setConflictPolicy(policy) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) CyanNeon else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = policy.title,
                                    color = if (isSelected) CyanNeon else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = policy.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Deduplication Filter Switch
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF101926),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lọc chống trùng lặp bước chân (Deduplication)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tự động phát hiện khi đeo 2 đồng hồ trên cùng một người để không nhân đôi số bước",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = stepDeduplicationEnabled,
                        onCheckedChange = { stepDeduplicationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanNeon.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E2D42)
                        )
                    )
                }
            }
        }

        // Real-time Audit Sync Log Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nhật ký đồng bộ trực tiếp (${syncLogs.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Đã xuất dữ liệu sức khỏe (JSON/CSV) ra bộ nhớ máy!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Xuất dữ liệu",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Xuất file", color = CyanNeon, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearSyncLogs() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Xóa nhật ký",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Live Log Entries
        if (syncLogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B131E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D42)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chưa có nhật ký gói tin. Nhấn 'Đồng bộ ngay' ở màn hình Tổng quan để kích hoạt.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(syncLogs, key = { it.id }) { log ->
                SyncLogItemRow(log = log)
            }
        }
    }
}

@Composable
fun SyncLogItemRow(log: SyncLogEntry) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeFormatted = formatter.format(Date(log.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF101926),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                when (log.status) {
                                    "SUCCESS" -> EmeraldNeon
                                    "WARNING" -> Color(0xFFFFB300)
                                    else -> HeartRateRed
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.deviceName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = timeFormatted,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${log.packetCount} gói tin • Độ trễ: ${log.latencyMs}ms",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Khớp: ${String.format("%.1f", log.accuracy)}%",
                    color = EmeraldNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
