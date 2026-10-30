package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.ConnectionStatus
import com.example.ui.components.ContinuousSyncStatusBanner
import com.example.ui.components.HealthStatCompactCard
import com.example.ui.components.LiveHeartRateCard
import com.example.ui.components.MultiDevicePrecisionComparisonCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HealthSyncViewModel

@Composable
fun OverviewScreen(
    viewModel: HealthSyncViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.healthSummary.collectAsState()
    val isContinuousRunning by viewModel.isContinuousSyncRunning.collectAsState()
    val syncInterval by viewModel.syncIntervalSeconds.collectAsState()
    val isSyncingNow by viewModel.isSyncingNow.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val connectedDevices = devices.filter { it.status == ConnectionStatus.CONNECTED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("overview_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Continuous Sync Banner
        item {
            ContinuousSyncStatusBanner(
                isRunning = isContinuousRunning,
                intervalSeconds = syncInterval,
                consensusPercent = summary.syncConsensusPercent,
                isSyncingNow = isSyncingNow,
                onManualSync = { viewModel.triggerManualSync() }
            )
        }

        // Active Alerts if any
        val unreadAlerts = alerts.filter { !it.isRead }
        if (unreadAlerts.isNotEmpty()) {
            item {
                val alert = unreadAlerts.first()
                val alertColor = if (alert.severity == AlertSeverity.CRITICAL) HeartRateRed else AmberWarning
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = alertColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, alertColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Cảnh báo",
                            tint = alertColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = alert.message,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { viewModel.dismissAlert(alert.id) }) {
                            Text(text = "Đóng", color = alertColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Heart Rate Card
        item {
            LiveHeartRateCard(
                bpm = summary.liveHeartRate,
                minBpm = summary.minHeartRate,
                maxBpm = summary.maxHeartRate,
                waveData = summary.recentBpmWave
            )
        }

        // Active watches horizontal strip
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đồng hồ đang đồng bộ (${connectedDevices.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Luôn kết nối",
                        color = EmeraldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (connectedDevices.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF101926),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22354E))
                    ) {
                        Text(
                            text = "Chưa có đồng hồ nào kết nối. Vào tab 'Thiết bị' để ghép nối.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(connectedDevices) { dev ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF101926),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (dev.isPrimary) CyanNeon.copy(alpha = 0.5f) else Color(0xFF22354E)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EmeraldNeon, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = dev.name,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (dev.isPrimary) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "★", color = CyanNeon, fontSize = 10.sp)
                                            }
                                        }
                                        Text(
                                            text = "Pin ${dev.batteryLevel}% • ${dev.rssi} dBm",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid (2 columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HealthStatCompactCard(
                        title = "Oxy trong máu (SpO2)",
                        value = "${summary.currentSpO2}",
                        unit = "%",
                        subtext = if (summary.currentSpO2 >= 95) "Mức an toàn tối ưu" else "Cần chú ý",
                        icon = Icons.Default.WaterDrop,
                        accentColor = CyanNeon,
                        progressFraction = summary.currentSpO2 / 100f,
                        modifier = Modifier.weight(1f),
                        testTag = "spo2_card"
                    )

                    HealthStatCompactCard(
                        title = "Số bước chân",
                        value = "%,d".format(summary.totalSteps),
                        unit = "bước",
                        subtext = "Mục tiêu: 10,000",
                        icon = Icons.Default.DirectionsWalk,
                        accentColor = EmeraldNeon,
                        progressFraction = summary.totalSteps / 10000f,
                        modifier = Modifier.weight(1f),
                        testTag = "steps_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HealthStatCompactCard(
                        title = "Giấc ngủ đêm qua",
                        value = "${summary.sleepHours}",
                        unit = "giờ",
                        subtext = "Điểm chất lượng: ${summary.sleepQualityScore}/100",
                        icon = Icons.Default.NightsStay,
                        accentColor = PurpleAccent,
                        progressFraction = summary.sleepQualityScore / 100f,
                        modifier = Modifier.weight(1f),
                        testTag = "sleep_card"
                    )

                    HealthStatCompactCard(
                        title = "Calo tiêu thụ",
                        value = "${summary.caloriesBurned}",
                        unit = "kcal",
                        subtext = "${summary.activeMinutes} phút hoạt động",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = Color(0xFFFF9100),
                        progressFraction = summary.caloriesBurned / 750f,
                        modifier = Modifier.weight(1f),
                        testTag = "calories_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HealthStatCompactCard(
                        title = "Body Battery",
                        value = "${summary.bodyBattery}",
                        unit = "%",
                        subtext = "Mức nạp năng lượng cao",
                        icon = Icons.Default.Bolt,
                        accentColor = EmeraldNeon,
                        progressFraction = summary.bodyBattery / 100f,
                        modifier = Modifier.weight(1f),
                        testTag = "body_battery_card"
                    )

                    HealthStatCompactCard(
                        title = "Mức độ căng thẳng",
                        value = "${summary.stressScore}",
                        unit = "/100",
                        subtext = "Thư thái • HRV ổn định",
                        icon = Icons.Default.SelfImprovement,
                        accentColor = BlueAccent,
                        progressFraction = summary.stressScore / 100f,
                        modifier = Modifier.weight(1f),
                        testTag = "stress_card"
                    )
                }
            }
        }

        // Multi-device precision comparison
        item {
            MultiDevicePrecisionComparisonCard(
                garminBpm = summary.liveHeartRate,
                appleBpm = summary.liveHeartRate + 1,
                galaxyBpm = summary.liveHeartRate - 1
            )
        }
    }
}
