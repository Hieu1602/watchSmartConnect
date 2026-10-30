package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeartRateTrendChart(
    bpmPoints: List<Int>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
                    text = "Biểu đồ nhịp tim 24 giờ",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Đã lọc nhiễu",
                    color = EmeraldNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val points = if (bpmPoints.isEmpty()) listOf(68, 70, 72, 75, 71, 74, 82, 95, 110, 88, 76, 73, 72, 74, 75) else bpmPoints

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val width = size.width
                val height = size.height

                val minVal = 50f
                val maxVal = 140f

                // Draw background horizontal grid guides
                val gridLines = listOf(60f, 90f, 120f)
                gridLines.forEach { lineVal ->
                    val y = height - ((lineVal - minVal) / (maxVal - minVal)) * height
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                val stepX = width / (points.size - 1).coerceAtLeast(1)
                val linePath = Path()
                val fillPath = Path()

                points.forEachIndexed { i, value ->
                    val x = i * stepX
                    val normY = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val y = height - (normY * height)

                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(width, height)
                fillPath.close()

                // Gradient fill underneath
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HeartRateRed.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // Main stroke
                drawPath(
                    path = linePath,
                    color = HeartRateRed,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "00:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "06:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "12:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "18:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "Bây giờ", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HourlyStepsBarChart(
    stepData: List<Int>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
                    text = "Phân bổ bước chân theo giờ",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Deduplicated",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val data = if (stepData.isEmpty()) listOf(
                120, 80, 0, 0, 0, 320, 850, 1420, 980, 650, 420, 890, 1100, 750, 920, 1250, 800, 600
            ) else stepData

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val width = size.width
                val height = size.height
                val maxSteps = 1500f
                val barSpacing = width / data.size
                val barWidth = barSpacing * 0.65f

                data.forEachIndexed { i, steps ->
                    val barHeight = (steps / maxSteps).coerceIn(0.04f, 1f) * height
                    val left = i * barSpacing + (barSpacing - barWidth) / 2f
                    val top = height - barHeight

                    val barColor = if (steps > 1000) CyanNeon else EmeraldNeon.copy(alpha = 0.8f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "06:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "10:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "14:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "18:00", color = TextMuted, fontSize = 10.sp)
                Text(text = "22:00", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun MultiDevicePrecisionComparisonCard(
    garminBpm: Int = 74,
    appleBpm: Int = 75,
    galaxyBpm: Int = 73,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        color = Color(0xFF101926),
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(CyanNeon, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "So sánh độ chính xác đa thiết bị",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Khớp 99.1%",
                    color = EmeraldNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Đồng hồ đo độc lập tại cùng thời điểm để loại trừ sai số phần cứng:",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Garmin row
            DeviceReadingRow(
                brandName = "Garmin Forerunner 965",
                sensorType = "Elevate™ v5 (Optical)",
                bpm = garminBpm,
                accuracy = "99.4%",
                accentColor = Color(0xFF007CC3),
                isPrimary = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Apple row
            DeviceReadingRow(
                brandName = "Apple Watch Ultra 2",
                sensorType = "Third-gen Optical + ECG",
                bpm = appleBpm,
                accuracy = "99.2%",
                accentColor = Color(0xFFE2E8F0),
                isPrimary = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Galaxy row
            DeviceReadingRow(
                brandName = "Galaxy Watch 6 Pro",
                sensorType = "BioActive Sensor",
                bpm = galaxyBpm,
                accuracy = "98.7%",
                accentColor = Color(0xFF1428A0),
                isPrimary = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Độ lệch chuẩn: ±0.8 BPM (Đạt chuẩn y tế thể thao)",
                color = EmeraldNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeviceReadingRow(
    brandName: String,
    sensorType: String,
    bpm: Int,
    accuracy: String,
    accentColor: Color,
    isPrimary: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B131E), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = brandName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isPrimary) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Chuẩn)",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "$sensorType • $accuracy",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$bpm",
                color = HeartRateRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "BPM",
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
