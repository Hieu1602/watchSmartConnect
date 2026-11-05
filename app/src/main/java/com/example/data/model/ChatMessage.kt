package com.example.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    AI,
    SYSTEM
}

enum class CareUrgency(val label: String, val colorHex: Long) {
    SAFE("Theo dõi tại nhà", 0xFF00E676),
    ATTENTION("Cần khám sớm", 0xFFFFB300),
    EMERGENCY("Báo động đỏ / Cấp cứu", 0xFFFF2A55)
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vitalContext: String? = null,
    val isStreaming: Boolean = false,
    val urgency: CareUrgency = CareUrgency.SAFE,
    val topicTag: String? = null
)

data class QuickPrompt(
    val id: String,
    val title: String,
    val prompt: String,
    val category: String,
    val iconName: String = "medical"
)
