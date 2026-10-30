package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ContinuousSyncStatusBanner(
    isRunning: Boolean,
    intervalSeconds: Int,
    consensusPercent: Float,
    isSyncingNow: Boolean,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isRunning) EmeraldNeon.copy(alpha = 0.4f) else Color(0xFF22354E),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onManualSync)
            .testTag("continuous_sync_banner"),
        color = Color(0xFF101926),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing dot
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .scale(beaconScale)
                                .background(EmeraldNeon.copy(alpha = 0.25f), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isRunning) EmeraldNeon else TextMuted, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isRunning) "ĐỒNG BỘ LIÊN TỤC (${intervalSeconds}s)" else "ĐỒNG BỘ NỀN TẮT",
                            color = if (isRunning) EmeraldNeon else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (isSyncingNow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Đang nhận gói tin…",
                                color = CyanNeon,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "Độ khớp chuẩn đa thiết bị: ${String.format("%.1f", consensusPercent)}%",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Sync Now Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSyncingNow) CyanNeon.copy(alpha = 0.2f) else Color(0xFF172336),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSyncingNow) CyanNeon else Color(0xFF22354E))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Đồng bộ",
                        tint = if (isSyncingNow) CyanNeon else TextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncingNow) "Đang nạp" else "Đồng bộ",
                        color = if (isSyncingNow) CyanNeon else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
