package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HeartRateRed

@Composable
fun HeartRateWaveCanvas(
    bpm: Int,
    waveData: List<Int>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw grid lines (subtle medical ECG grid)
        val gridColor = Color(0x1A00E5FF)
        val stepX = width / 12f
        for (i in 0..12) {
            drawLine(
                color = gridColor,
                start = Offset(i * stepX, 0f),
                end = Offset(i * stepX, height),
                strokeWidth = 1f
            )
        }
        val stepY = height / 4f
        for (i in 0..4) {
            drawLine(
                color = gridColor,
                start = Offset(0f, i * stepY),
                end = Offset(width, i * stepY),
                strokeWidth = 1f
            )
        }

        // Build ECG wave path
        val path = Path()
        val count = 28
        val segmentW = width / count

        for (i in 0..count) {
            val x = i * segmentW
            val normPhase = (i.toFloat() / count + phase) % 1f

            // Shape standard P-Q-R-S-T wave pulse
            val yOffset = when {
                normPhase in 0.40f..0.45f -> -8f // P wave
                normPhase in 0.47f..0.49f -> 6f // Q dip
                normPhase in 0.49f..0.52f -> -26f // R peak
                normPhase in 0.52f..0.55f -> 14f // S dip
                normPhase in 0.58f..0.66f -> -10f // T wave
                else -> 0f
            }

            val y = midY + yOffset

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw glowing gradient wave stroke
        val strokeBrush = Brush.horizontalGradient(
            colors = listOf(
                HeartRateRed.copy(alpha = 0.2f),
                HeartRateRed.copy(alpha = 0.7f),
                HeartRateRed,
                HeartRateRed.copy(alpha = 0.8f),
                HeartRateRed.copy(alpha = 0.3f)
            )
        )

        drawPath(
            path = path,
            brush = strokeBrush,
            style = Stroke(width = 3.5.dp.toPx())
        )

        // Draw leading neon pulse head
        val headX = (phase * width) % width
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(headX, midY)
        )
        drawCircle(
            color = HeartRateRed.copy(alpha = 0.5f),
            radius = 7.dp.toPx(),
            center = Offset(headX, midY)
        )
    }
}
