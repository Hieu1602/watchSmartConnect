package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HeartRateTrendChart
import com.example.ui.components.HourlyStepsBarChart
import com.example.ui.components.MultiDevicePrecisionComparisonCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HealthSyncViewModel

@Composable
fun AnalyticsScreen(
    viewModel: HealthSyncViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.healthSummary.collectAsState()
    val recentMetrics by viewModel.recentMetrics.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("Hôm nay") }

    val bpmPoints = recentMetrics.map { it.heartRate }.reversed()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe Selector Pill Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101926), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Hôm nay", "7 ngày qua", "30 ngày").forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyanNeon else Color.Transparent)
                            .clickable { selectedTimeframe = timeframe }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeframe,
                            color = if (isSelected) Color(0xFF090E17) else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Multi-Device Precision Card
        item {
            MultiDevicePrecisionComparisonCard(
                garminBpm = summary.liveHeartRate,
                appleBpm = summary.liveHeartRate + 1,
                galaxyBpm = summary.liveHeartRate - 1
            )
        }

        // Heart rate chart
        item {
            HeartRateTrendChart(bpmPoints = bpmPoints)
        }

        // Steps Distribution Chart
        item {
            HourlyStepsBarChart(stepData = emptyList())
        }

        // SpO2 Saturation Analysis Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF22354E), RoundedCornerShape(18.dp)),
                color = Color(0xFF101926),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nồng độ Oxy trong máu (SpO2)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Độ tin cậy: 99.6%",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${summary.currentSpO2}",
                                    color = TextPrimary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%",
                                    color = CyanNeon,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                            Text(
                                text = "Bình thường (Chuẩn y tế: 95 - 100%)",
                                color = EmeraldNeon,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Trung bình: 98%", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Thấp nhất đêm qua: 96%", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Safety bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF1F2B3E), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (summary.currentSpO2 - 85) / 15f)
                                .height(8.dp)
                                .background(CyanNeon, CircleShape)
                        )
                    }
                }
            }
        }

        // Sleep Architecture & Quality Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF22354E), RoundedCornerShape(18.dp)),
                color = Color(0xFF101926),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phân tích cấu trúc giấc ngủ",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${summary.sleepHours} giờ • Điểm ${summary.sleepQualityScore}/100",
                            color = PurpleAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleep Stages Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        Box(modifier = Modifier.weight(0.28f).fillMaxSize().background(Color(0xFF673AB7))) // Deep
                        Box(modifier = Modifier.weight(0.48f).fillMaxSize().background(Color(0xFF9575CD))) // Light
                        Box(modifier = Modifier.weight(0.18f).fillMaxSize().background(Color(0xFF00E5FF))) // REM
                        Box(modifier = Modifier.weight(0.06f).fillMaxSize().background(Color(0xFFFFB300))) // Awake
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SleepStageLegendItem("Sâu (28%)", Color(0xFF673AB7))
                        SleepStageLegendItem("Nông (48%)", Color(0xFF9575CD))
                        SleepStageLegendItem("REM (18%)", Color(0xFF00E5FF))
                        SleepStageLegendItem("Thức (6%)", Color(0xFFFFB300))
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepStageLegendItem(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = name, color = TextSecondary, fontSize = 11.sp)
    }
}
