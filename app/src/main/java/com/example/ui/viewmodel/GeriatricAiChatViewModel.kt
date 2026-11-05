package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CareUrgency
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.QuickPrompt
import com.example.engine.GeriatricClinicalKnowledgeEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.appcheck.appCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

private const val TAG = "GeriatricAiChatVM"

sealed interface ChatAiState {
    data object Idle : ChatAiState
    data object Generating : ChatAiState
    data class Error(val message: String) : ChatAiState
}

class GeriatricAiChatViewModel(application: Application) : AndroidViewModel(application) {

    // Lazy initialization of Gemini 3.8 Flash model via Firebase AI Logic
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    Bạn là Bác sĩ Trợ lý AI Lão khoa (Geriatric Care AI), chuyên gia y tế chuyên biệt về sức khỏe người cao tuổi.
                    Đối tượng người dùng trò chuyện với bạn là NGƯỜI TRẺ (con cái, cháu, người chăm sóc trong gia đình) đang chăm sóc cho ông bà hoặc cha mẹ già.

                    QUY TẮC BẮT BUỘC SỐ 1 TỪ NGƯỜI DÙNG:
                    "TRẢ LỜI ĐÚNG TRỌNG TÂM CÂU HỎI. GIẢI THÍCH CỰC KỲ GỌN, ĐÚNG MỤC TIÊU CÂU HỎI, KHÔNG DÀI DÒNG LAN MAN."
                    - Người dùng hỏi về vấn đề gì (ví dụ: đau răng, đau mắt, ho sốt, đau lưng, mất ngủ, té ngã, đau dạ dày, táo bón, thuốc men, dinh dưỡng...), BẠN PHẢI TRẢ LỜI TRỰC TIẾP VÀ DUY NHẤT VỀ VẤN ĐỀ ĐÓ.
                    - TUYỆT ĐỐI KHÔNG tự động nói sang bệnh khác (không tự ý nói về huyết áp, tim mạch hay bệnh khác nếu người dùng không hỏi).
                    - Dữ liệu sinh tồn từ đồng hồ đeo tay (nếu có kèm) chỉ là thông tin tham khảo bổ trợ, tuyệt đối KHÔNG để các chỉ số này làm chệch trọng tâm câu hỏi nếu người dùng không hỏi về chúng.

                    HÃY TRÌNH BÀY MỌI CÂU TRẢ LỜI THEO CẤU TRÚC 3 PHẦN:
                    1. 🎯 **KẾT LUẬN NHANH** (1-2 câu ngắn): Trả lời trực diện vào câu hỏi, chỉ rõ nguyên nhân hay gặp ở người già và mức độ nguy cơ.
                    2. 📋 **HÀNH ĐỘNG CỤ THỂ (Cho người trẻ làm ngay)**: 3-4 gạch đầu dòng thực tế, súc tích (hướng dẫn cụ thể con cháu cần làm gì cho cụ: chăm sóc, dinh dưỡng, tư thế, cách dùng thuốc an toàn...).
                    3. 🚨 **CẢNH BÁO ĐỎ (Cần đi viện/gọi 115 ngay)**: 2-3 dấu hiệu báo động nguy hiểm của chính triệu chứng đang hỏi.
                    """.trimIndent()
                )
            }
        )
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _aiState = MutableStateFlow<ChatAiState>(ChatAiState.Idle)
    val aiState: StateFlow<ChatAiState> = _aiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isVitalsAttached = MutableStateFlow(false)
    val isVitalsAttached: StateFlow<Boolean> = _isVitalsAttached.asStateFlow()

    private val _selectedTopicFilter = MutableStateFlow<String?>("Tất cả")
    val selectedTopicFilter: StateFlow<String?> = _selectedTopicFilter.asStateFlow()

    // Text To Speech
    private var tts: TextToSpeech? = null
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    val quickPrompts = listOf(
        QuickPrompt(
            id = "bp_high",
            title = "Huyết áp 150/95 mmHg",
            prompt = "Bà em 72 tuổi, vừa đo huyết áp thấy 150/95 mmHg, nhịp tim 84. Như vậy có nguy hiểm không và bây giờ em cần làm gì cho bà?",
            category = "Tim mạch & Huyết áp"
        ),
        QuickPrompt(
            id = "stroke_signs",
            title = "Dấu hiệu đột quỵ (FAST)",
            prompt = "Dấu hiệu nhận biết sớm cơn đột quỵ hoặc thiếu máu não thoáng qua ở người cao tuổi là gì? Cách sơ cứu nhanh trong thời gian vàng?",
            category = "Cấp cứu khẩn"
        ),
        QuickPrompt(
            id = "memory_loss",
            title = "Hay quên vs Alzheimer",
            prompt = "Ông dạo này hay quên chìa khóa và hỏi đi hỏi lại một câu. Làm sao phân biệt đãng trí tuổi già thông thường với bệnh Alzheimer sa sút trí tuệ?",
            category = "Trí nhớ & Thần kinh"
        ),
        QuickPrompt(
            id = "fall_prevention",
            title = "Phòng tránh té ngã",
            prompt = "Người lớn tuổi rất sợ ngã gãy xương hông. Hãy hướng dẫn nhanh các việc cần làm để chống trượt ngã trong nhà tắm và phòng ngủ cho người già?",
            category = "Phòng té ngã"
        ),
        QuickPrompt(
            id = "insomnia_elderly",
            title = "Mất ngủ nửa đêm",
            prompt = "Cụ trằn trọc khó vào giấc, hay tỉnh lúc 2-3 giờ sáng rồi thức trắng. Có nên cho uống thuốc ngủ không và giải pháp khắc phục an toàn?",
            category = "Giấc ngủ & Sinh hoạt"
        ),
        QuickPrompt(
            id = "postural_dizziness",
            title = "Chóng mặt khi đứng dậy",
            prompt = "Mỗi lần cụ từ trên giường ngồi dậy bước đi là bị hoa mắt chóng mặt suýt ngã. Hiện tượng này là gì và người nhà cần hỗ trợ thế nào?",
            category = "Tim mạch & Huyết áp"
        ),
        QuickPrompt(
            id = "medication_safe",
            title = "Uống nhiều thuốc cùng lúc",
            prompt = "Bố em phải uống 5 loại thuốc (huyết áp, tiểu đường, mỡ máu, dạ dày). Quy tắc uống thế nào để tránh tương tác thuốc và bảo vệ dạ dày?",
            category = "Thuốc & Liều dùng"
        ),
        QuickPrompt(
            id = "nutrition_soft",
            title = "Dinh dưỡng người già biếng ăn",
            prompt = "Bà dạo này ăn uống kém, chán ăn, răng yếu. Cần bổ sung dinh dưỡng gì để tránh suy dinh dưỡng và sụt cơ ở người lớn tuổi?",
            category = "Dinh dưỡng"
        )
    )

    init {
        initTts()
        addWelcomeMessage()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("vi", "VN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS Init exception", e)
        }
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            id = "welcome_msg",
            sender = MessageSender.AI,
            text = """
            Chào bạn! Tôi là **Bác sĩ Trợ lý AI Lão khoa**.
            
            🎯 Tôi hỗ trợ bạn - người thân trẻ tuổi - theo dõi và chăm sóc sức khỏe cho **ông bà, cha mẹ già**.
            
            📌 **Cam kết phong cách trả lời**: 
            - Giải thích siêu gọn, đúng trọng tâm.
            - Hướng dẫn các bước hành động cụ thể cho bạn làm ngay.
            - Nhận diện sớm các dấu hiệu cảnh báo đỏ (Red Flags).
            
            💡 Bạn có thể chọn câu hỏi mẫu bên dưới hoặc gõ trực tiếp tình huống của người lớn tuổi trong gia đình!
            """.trimIndent(),
            urgency = CareUrgency.SAFE,
            topicTag = "Hướng dẫn"
        )
        _messages.value = listOf(welcome)
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun toggleVitalsAttachment() {
        _isVitalsAttached.value = !_isVitalsAttached.value
    }

    fun setTopicFilter(topic: String?) {
        _selectedTopicFilter.value = topic
    }

    fun selectQuickPrompt(prompt: QuickPrompt, currentVitalsText: String?) {
        _inputText.value = prompt.prompt
        sendMessage(
            promptText = prompt.prompt,
            vitalTelemetry = if (_isVitalsAttached.value) currentVitalsText else null,
            topic = prompt.category
        )
    }

    fun sendCurrentInput(currentVitalsText: String?) {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _aiState.value is ChatAiState.Generating) return
        sendMessage(
            promptText = text,
            vitalTelemetry = if (_isVitalsAttached.value) currentVitalsText else null
        )
        _inputText.value = ""
    }

    private fun sendMessage(
        promptText: String,
        vitalTelemetry: String? = null,
        topic: String? = null
    ) {
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id = userMsgId,
            sender = MessageSender.USER,
            text = promptText,
            vitalContext = vitalTelemetry,
            topicTag = topic
        )

        val aiMsgId = UUID.randomUUID().toString()
        val initialAiMessage = ChatMessage(
            id = aiMsgId,
            sender = MessageSender.AI,
            text = "Đang phân tích dữ liệu y tế lão khoa...",
            isStreaming = true,
            urgency = CareUrgency.SAFE,
            topicTag = topic
        )

        _messages.value = _messages.value + userMessage + initialAiMessage
        _aiState.value = ChatAiState.Generating

        viewModelScope.launch {
            val accumulatedText = StringBuilder()
            var streamedSuccessfully = false

            // Build prompt with telemetry if available
            val fullPrompt = buildString {
                if (!vitalTelemetry.isNullOrBlank()) {
                    append("[CHỈ SỐ SINH TỒN THAM KHẢO THÊM TỪ ĐỒNG HỒ - chỉ tham khảo nếu liên quan đến câu hỏi, không để chỉ số này làm chệch hướng câu hỏi]:\n")
                    append(vitalTelemetry)
                    append("\n\n---\n")
                }
                append("CÂU HỎI TỪ NGƯỜI DÙNG:\n")
                append(promptText)
                append("\n\n(Yêu cầu: Trả lời đúng trọng tâm câu hỏi trên, cực kỳ ngắn gọn, đúng mục tiêu, cấu trúc 3 phần: 🎯 KẾT LUẬN NHANH, 📋 HÀNH ĐỘNG CỤ THỂ, 🚨 CẢNH BÁO ĐỎ)")
            }

            try {
                // Responsive timeout for AI generation (20s)
                withTimeout(20_000L) {
                    model.generateContentStream(fullPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulatedText.append(chunkText)

                            val currentFullText = accumulatedText.toString()
                            val urgency = detectUrgency(currentFullText)

                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == aiMsgId) {
                                    msg.copy(
                                        text = currentFullText,
                                        isStreaming = true,
                                        urgency = urgency
                                    )
                                } else {
                                    msg
                                }
                            }
                        }
                    }
                }

                val finalOutput = accumulatedText.toString()
                if (finalOutput.isNotBlank()) {
                    val finalUrgency = detectUrgency(finalOutput)
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == aiMsgId) {
                            msg.copy(
                                text = finalOutput,
                                isStreaming = false,
                                urgency = finalUrgency
                            )
                        } else {
                            msg
                        }
                    }
                    _aiState.value = ChatAiState.Idle
                    streamedSuccessfully = true
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Streaming timed out after 20s, falling back to specialized clinical knowledge engine", e)
                // Proceed directly to fallback so user receives immediate, accurate guidance instead of an error
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI streaming generation failed: ${e.message}", e)
                
                // If App Check or server error occurs, attempt token refresh for future calls
                if (e.message?.contains("App Check", ignoreCase = true) == true) {
                    try {
                        Firebase.appCheck.getAppCheckToken(true).await()
                        Log.d(TAG, "Forced App Check token refresh successfully.")
                    } catch (appCheckEx: Exception) {
                        Log.w(TAG, "App Check refresh error", appCheckEx)
                    }
                }
            }

            // If streaming didn't complete (network timeout or connection issue), supply immediate expert geriatric clinical advice
            if (!streamedSuccessfully) {
                val clinicalResult = GeriatricClinicalKnowledgeEngine.generateResponse(promptText, vitalTelemetry)
                
                // Stream smoothly to UI for natural UX
                simulateStreamingResponse(aiMsgId, clinicalResult.text, clinicalResult.urgency)
                _aiState.value = ChatAiState.Idle
                streamedSuccessfully = true
            }
        }
    }

    private suspend fun simulateStreamingResponse(
        msgId: String,
        fullContent: String,
        urgency: CareUrgency
    ) {
        val lines = fullContent.lines()
        val current = StringBuilder()
        for (line in lines) {
            current.append(line).append("\n")
            _messages.value = _messages.value.map { msg ->
                if (msg.id == msgId) {
                    msg.copy(
                        text = current.toString().trimEnd(),
                        isStreaming = true,
                        urgency = urgency
                    )
                } else msg
            }
            delay(30)
        }
        _messages.value = _messages.value.map { msg ->
            if (msg.id == msgId) {
                msg.copy(
                    text = fullContent,
                    isStreaming = false,
                    urgency = urgency
                )
            } else msg
        }
    }

    private fun detectUrgency(text: String): CareUrgency {
        val lower = text.lowercase()
        return when {
            lower.contains("115") || lower.contains("cấp cứu") || lower.contains("đột quỵ") ||
                    lower.contains("nguy kịch") || lower.contains("báo động đỏ") || lower.contains("tai biến") -> CareUrgency.EMERGENCY
            lower.contains("cần đi khám") || lower.contains("theo dõi sát") || lower.contains("lưu ý") ||
                    lower.contains("cảnh báo") -> CareUrgency.ATTENTION
            else -> CareUrgency.SAFE
        }
    }

    private fun updateAiMessageOnError(id: String, errorText: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    text = "⚠️ $errorText",
                    isStreaming = false,
                    urgency = CareUrgency.ATTENTION
                )
            } else {
                msg
            }
        }
    }

    fun speakText(text: String) {
        try {
            if (_isTtsSpeaking.value) {
                tts?.stop()
                _isTtsSpeaking.value = false
            } else {
                val cleanText = text.replace("**", "").replace("#", "").replace("-", " ")
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "geriatric_tts")
                _isTtsSpeaking.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error", e)
        }
    }

    fun stopTts() {
        tts?.stop()
        _isTtsSpeaking.value = false
    }

    fun clearChat() {
        _messages.value = emptyList()
        addWelcomeMessage()
        _aiState.value = ChatAiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "TTS shutdown error", e)
        }
    }
}
