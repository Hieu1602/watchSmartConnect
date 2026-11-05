package com.example

import com.example.data.model.CareUrgency
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.QuickPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeriatricAiUnitTest {

    @Test
    fun testChatMessageCreationAndUrgency() {
        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = "Huyết áp 160/100 ở cụ 75 tuổi có nguy hiểm?",
            vitalContext = "HR: 88 BPM, SpO2: 96%",
            urgency = CareUrgency.ATTENTION,
            topicTag = "Tim mạch & Huyết áp"
        )

        assertEquals(MessageSender.USER, userMessage.sender)
        assertNotNull(userMessage.vitalContext)
        assertEquals(CareUrgency.ATTENTION, userMessage.urgency)
        assertTrue(userMessage.text.contains("160/100"))
    }

    @Test
    fun testCareUrgencyClassification() {
        fun classifyUrgency(text: String): CareUrgency {
            val lower = text.lowercase()
            return when {
                lower.contains("115") || lower.contains("cấp cứu") || lower.contains("đột quỵ") -> CareUrgency.EMERGENCY
                lower.contains("cần đi khám") || lower.contains("theo dõi sát") -> CareUrgency.ATTENTION
                else -> CareUrgency.SAFE
            }
        }

        assertEquals(CareUrgency.EMERGENCY, classifyUrgency("Cần gọi cấp cứu 115 ngay lập tức"))
        assertEquals(CareUrgency.ATTENTION, classifyUrgency("Cụ cần đi khám chuyên khoa tim mạch sớm"))
        assertEquals(CareUrgency.SAFE, classifyUrgency("Chỉ số huyết áp ổn định, theo dõi tại nhà"))
    }

    @Test
    fun testQuickPromptsCoverageForElderlyCare() {
        val categories = listOf(
            "Tim mạch & Huyết áp",
            "Cấp cứu khẩn",
            "Trí nhớ & Thần kinh",
            "Phòng té ngã",
            "Giấc ngủ & Sinh hoạt",
            "Thuốc & Liều dùng",
            "Dinh dưỡng"
        )

        val samplePrompt = QuickPrompt(
            id = "test_bp",
            title = "Huyết áp người già",
            prompt = "Chỉ số huyết áp mục tiêu ở người trên 70 tuổi là bao nhiêu?",
            category = "Tim mạch & Huyết áp"
        )

        assertTrue(categories.contains(samplePrompt.category))
        assertTrue(samplePrompt.prompt.isNotEmpty())
    }

    @Test
    fun testGeriatricClinicalKnowledgeEngine_ToothacheQuestion_ReturnsDentalAdviceNotBloodPressure() {
        val result = com.example.engine.GeriatricClinicalKnowledgeEngine.generateResponse(
            "Bà tôi 75 tuổi bị đau răng sưng nướu không nhai được thì làm thế nào?"
        )
        // Must address teeth/gums, must NOT be off-topic
        assertTrue("Must address dental/gums", result.text.contains("răng") || result.text.contains("nướu"))
        assertTrue("Must have Conclusion", result.text.contains("🎯 **KẾT LUẬN NHANH**"))
        assertTrue("Must have Action", result.text.contains("📋 **HÀNH ĐỘNG CỤ THỂ"))
        assertTrue("Must have Red Flags", result.text.contains("🚨 **CẢNH BÁO ĐỎ"))
        // Must NOT output hypertension advice for a toothache question!
        org.junit.Assert.assertFalse("Must not confuse with hypertension", result.text.contains("TĂNG HUYẾT ÁP ĐỘ 1"))
    }

    @Test
    fun testGeriatricClinicalKnowledgeEngine_WordTimDoesNotTriggerHypertension() {
        val result = com.example.engine.GeriatricClinicalKnowledgeEngine.generateResponse(
            "Tìm cách giúp cụ ngủ ngon giấc về đêm"
        )
        // Must address sleep/insomnia
        assertTrue("Must address sleep", result.text.contains("ngủ") || result.text.contains("Melatonin"))
        org.junit.Assert.assertFalse("Must not trigger hypertension on word 'tìm'", result.text.contains("TĂNG HUYẾT ÁP ĐỘ 1"))
    }

    @Test
    fun testGeriatricClinicalKnowledgeEngine_StrokeQuestion_ReturnsEmergencyUrgency() {
        val result = com.example.engine.GeriatricClinicalKnowledgeEngine.generateResponse(
            "Ông bị méo miệng và đớ lưỡi khi nói chuyện, tay phải yếu"
        )
        assertEquals(CareUrgency.EMERGENCY, result.urgency)
        assertTrue(result.text.contains("FAST"))
        assertTrue(result.text.contains("115"))
    }

    @Test
    fun testGeriatricClinicalKnowledgeEngine_FoodQuestion_ReturnsNutritionAdvice() {
        val result = com.example.engine.GeriatricClinicalKnowledgeEngine.generateResponse(
            "Nấu món gì dễ ăn bồi bổ cho cụ vừa ốm dậy?"
        )
        assertTrue(result.text.contains("dinh dưỡng") || result.text.contains("món ăn") || result.text.contains("cháo"))
        assertTrue(result.text.contains("🎯 **KẾT LUẬN NHANH**"))
        assertTrue(result.text.contains("📋 **HÀNH ĐỘNG CỤ THỂ"))
        assertTrue(result.text.contains("🚨 **CẢNH BÁO ĐỎ"))
    }
}
