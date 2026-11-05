package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CareUrgency
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.QuickPrompt
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HealthDarkBackground
import com.example.ui.theme.HealthDarkCardBorder
import com.example.ui.theme.HealthDarkSurface
import com.example.ui.theme.HealthDarkSurfaceVariant
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ChatAiState
import com.example.ui.viewmodel.GeriatricAiChatViewModel
import com.example.ui.viewmodel.HealthSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    healthViewModel: HealthSyncViewModel,
    chatViewModel: GeriatricAiChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val aiState by chatViewModel.aiState.collectAsState()
    val inputText by chatViewModel.inputText.collectAsState()
    val isVitalsAttached by chatViewModel.isVitalsAttached.collectAsState()
    val isTtsSpeaking by chatViewModel.isTtsSpeaking.collectAsState()
    val selectedFilter by chatViewModel.selectedTopicFilter.collectAsState()

    val healthSummary by healthViewModel.healthSummary.collectAsState()

    // Formatted telemetry string from watch
    val vitalsTelemetryString = remember(healthSummary) {
        "• Nhịp tim đo được: ${healthSummary.liveHeartRate} BPM\n" +
                "• Nồng độ Oxy máu SpO2: ${healthSummary.currentSpO2}%\n" +
                "• Điểm căng thẳng thần kinh: ${healthSummary.stressScore}/100\n" +
                "• Thời lượng ngủ đêm qua: ${healthSummary.sleepHours} giờ\n" +
                "• Vận động trong ngày: ${healthSummary.totalSteps} bước"
    }

    var showEmergencyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val categories = listOf(
        "Tất cả",
        "Tim mạch & Huyết áp",
        "Cấp cứu khẩn",
        "Trí nhớ & Thần kinh",
        "Phòng té ngã",
        "Giấc ngủ & Sinh hoạt",
        "Thuốc & Liều dùng",
        "Dinh dưỡng"
    )

    val filteredQuickPrompts = remember(selectedFilter) {
        if (selectedFilter == null || selectedFilter == "Tất cả") {
            chatViewModel.quickPrompts
        } else {
            chatViewModel.quickPrompts.filter { it.category == selectedFilter }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthDarkBackground)
    ) {
        // 1. Header Banner: Geriatric AI Identity & Emergency Call 115
        Surface(
            color = HealthDarkSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyanNeon, PurpleAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Biểu tượng Bác sĩ Lão khoa",
                                tint = HealthDarkBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bác Sĩ AI Lão Khoa",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldNeon)
                                )
                            }
                            Text(
                                text = "Dành cho người trẻ chăm sóc ông bà, cha mẹ • Trả lời gọn",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Emergency 115 Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HeartRateRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, HeartRateRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .testTag("call_115_btn")
                            .clickable { showEmergencyDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Gọi cấp cứu 115",
                                tint = HeartRateRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "115 Khẩn",
                                color = HeartRateRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Telemetry Sync Status Pill
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HealthDarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Chỉ số từ đồng hồ",
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đồng hồ: HR ${healthSummary.liveHeartRate} BPM • SpO2 ${healthSummary.currentSpO2}% • Căng thẳng ${healthSummary.stressScore}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isVitalsAttached) CyanNeon.copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isVitalsAttached) CyanNeon else TextMuted),
                        modifier = Modifier
                            .testTag("attach_vitals_btn")
                            .clickable { chatViewModel.toggleVitalsAttachment() }
                    ) {
                        Text(
                            text = if (isVitalsAttached) "✓ Đính kèm" else "+ Đính kèm",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isVitalsAttached) CyanNeon else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter Category Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedFilter == category
                FilterChip(
                    selected = isSelected,
                    onClick = { chatViewModel.setTopicFilter(category) },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon.copy(alpha = 0.25f),
                        selectedLabelColor = CyanNeon,
                        containerColor = HealthDarkSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CyanNeon else Color.Transparent
                    )
                )
            }
        }

        // 3. Quick Tap Prompt Suggestions
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredQuickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HealthDarkSurfaceVariant,
                    border = BorderStroke(1.dp, HealthDarkCardBorder),
                    modifier = Modifier
                        .clickable {
                            chatViewModel.selectQuickPrompt(prompt, vitalsTelemetryString)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prompt.title,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubbleItem(
                    message = message,
                    isSpeaking = isTtsSpeaking,
                    onSpeak = { chatViewModel.speakText(it) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Y tế Lão khoa", it))
                        Toast.makeText(context, "Đã sao chép câu trả lời!", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, it)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ hướng dẫn y tế"))
                    }
                )
            }

            if (aiState is ChatAiState.Generating && messages.lastOrNull()?.sender != MessageSender.AI) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyanNeon,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bác sĩ AI đang suy luận phác đồ y tế...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // 5. Input Field & Controls
        Surface(
            color = HealthDarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Clear chat button
                    IconButton(
                        onClick = { chatViewModel.clearChat() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Xóa cuộc trò chuyện",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { chatViewModel.updateInputText(it) },
                        placeholder = {
                            Text(
                                text = "Hỏi về huyết áp, thuốc, té ngã, ăn uống...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                chatViewModel.sendCurrentInput(vitalsTelemetryString)
                            }
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = HealthDarkCardBorder,
                            focusedContainerColor = HealthDarkSurfaceVariant,
                            unfocusedContainerColor = HealthDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    val isGenerating = aiState is ChatAiState.Generating
                    IconButton(
                        onClick = {
                            chatViewModel.sendCurrentInput(vitalsTelemetryString)
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) CyanNeon else HealthDarkSurfaceVariant
                            )
                            .testTag("send_message_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = TextMuted,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi câu hỏi",
                                tint = if (inputText.isNotBlank()) HealthDarkBackground else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Medical Disclaimer Footer
                Text(
                    text = "Lưu ý: Bác sĩ AI Lão khoa cung cấp giải thích y khoa định hướng nhanh cho gia đình. Trong trường hợp nguy kịch, gọi ngay 115.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                )
            }
        }
    }

    // Emergency 115 Confirmation Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = HeartRateRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gọi Cấp Cứu 115", color = HeartRateRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Bạn sắp thực hiện cuộc gọi khẩn cấp đến Trung tâm Cấp cứu Y tế 115. Vui lòng giữ bình tĩnh và chuẩn bị báo địa chỉ chính xác của người lớn tuổi.",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmergencyDialog = false
                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:115")
                        }
                        context.startActivity(callIntent)
                    }
                ) {
                    Text("Gọi Ngay 115", color = HeartRateRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = HealthDarkSurface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            // Header for AI message with urgency badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bác Sĩ AI Lão Khoa",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Urgency indicator badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(message.urgency.colorHex).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(message.urgency.colorHex).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = message.urgency.label,
                        color = Color(message.urgency.colorHex),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Bubble container
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) HealthDarkSurfaceVariant else HealthDarkSurface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    isUser -> CyanNeon.copy(alpha = 0.3f)
                    message.urgency == CareUrgency.EMERGENCY -> HeartRateRed.copy(alpha = 0.6f)
                    message.urgency == CareUrgency.ATTENTION -> AmberWarning.copy(alpha = 0.5f)
                    else -> HealthDarkCardBorder
                }
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.98f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Attached vitals context if present in user message
                if (isUser && !message.vitalContext.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyanNeon.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kèm chỉ số sinh tồn của người lớn tuổi",
                                fontSize = 11.sp,
                                color = CyanNeon,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Render structured medical text
                FormattedMedicalText(rawText = message.text)

                // Footer actions for AI responses
                if (!isUser && !message.isStreaming) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TTS Read Aloud
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Đọc bằng giọng nói",
                                tint = if (isSpeaking) CyanNeon else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy
                        IconButton(
                            onClick = { onCopy(message.text) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Sao chép câu trả lời",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = { onShare(message.text) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Chia sẻ cho người thân",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats markdown-like sections (bold **text**, bullet points, emojis)
 * cleanly for mobile readability.
 */
@Composable
fun FormattedMedicalText(rawText: String) {
    val annotatedString = buildAnnotatedString {
        val lines = rawText.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("🎯") || trimmed.contains("KẾT LUẬN") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CyanNeon, fontSize = 14.5.sp)) {
                        append(line)
                    }
                }
                trimmed.startsWith("📋") || trimmed.contains("HÀNH ĐỘNG") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EmeraldNeon, fontSize = 14.sp)) {
                        append(line)
                    }
                }
                trimmed.startsWith("🚨") || trimmed.contains("CẢNH BÁO ĐỎ") || trimmed.contains("CẤP CỨU") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HeartRateRed, fontSize = 14.sp)) {
                        append(line)
                    }
                }
                trimmed.startsWith("•") || trimmed.startsWith("-") -> {
                    withStyle(SpanStyle(color = TextPrimary, fontSize = 13.5.sp)) {
                        append("  $line")
                    }
                }
                else -> {
                    // Parse embedded **bold** tags
                    var remaining = line
                    while (remaining.contains("**")) {
                        val start = remaining.indexOf("**")
                        val before = remaining.substring(0, start)
                        append(before)
                        val afterFirst = remaining.substring(start + 2)
                        val end = afterFirst.indexOf("**")
                        if (end != -1) {
                            val boldContent = afterFirst.substring(0, end)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                                append(boldContent)
                            }
                            remaining = afterFirst.substring(end + 2)
                        } else {
                            append(afterFirst)
                            remaining = ""
                        }
                    }
                    withStyle(SpanStyle(color = TextPrimary, fontSize = 13.5.sp)) {
                        append(remaining)
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }

    Text(
        text = annotatedString,
        lineHeight = 20.sp
    )
}
