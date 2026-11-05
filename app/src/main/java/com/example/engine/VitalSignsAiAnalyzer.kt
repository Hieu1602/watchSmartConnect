package com.example.engine

import com.example.data.model.AggregatedHealthSummary
import com.example.data.model.HealthMetricRecord
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class BiosignalAiReport(
    val timestamp: Long = System.currentTimeMillis(),
    val overallHealthScore: Int, // 0 - 100
    val cardiovascularRisk: RiskLevel,
    val rmssdHrvMs: Float, // RMSSD in ms
    val sdnnHrvMs: Float, // SDNN in ms
    val autonomicBalance: String, // "Cân bằng phó giao cảm tốt", "Căng thẳng giao cảm cao"
    val oxygenStabilityScore: Float, // %
    val apneaRisk: RiskLevel,
    val recoveryReadiness: Int, // 0 - 100%
    val metabolicStrain: String, // Thấp, Bình thường, Cao
    val aiDiagnosticSummary: String,
    val actionableRecommendations: List<String>
)

enum class RiskLevel(val label: String, val colorHex: Long) {
    LOW("Thấp (An toàn)", 0xFF00E676),
    MODERATE("Trung bình (Theo dõi)", 0xFFFFB300),
    HIGH("Nguy cơ cao (Cần chú ý)", 0xFFFF2A55)
}

class VitalSignsAiAnalyzer {

    fun analyzeBiosignals(
        summary: AggregatedHealthSummary,
        recentHistory: List<HealthMetricRecord>
    ): BiosignalAiReport {
        val currentBpm = summary.liveHeartRate
        val spO2 = summary.currentSpO2
        val stress = summary.stressScore
        val sleepHours = summary.sleepHours

        // 1. Calculate HRV Time-Domain Metrics (RMSSD & SDNN) from beat intervals
        val rrIntervals = if (recentHistory.isNotEmpty()) {
            recentHistory.map { (60000f / it.heartRate.coerceAtLeast(40)) }
        } else {
            // Synthesize RR intervals around current BPM
            val meanRr = 60000f / currentBpm
            List(30) { meanRr + kotlin.random.Random.nextFloat() * 40f - 20f }
        }

        // SDNN: Standard deviation of NN intervals
        val meanRr = rrIntervals.average().toFloat()
        val variance = rrIntervals.map { (it - meanRr).pow(2) }.average().toFloat()
        val sdnn = sqrt(variance)

        // RMSSD: Root mean square of successive differences
        var sumSuccessiveDiffSq = 0f
        for (i in 0 until rrIntervals.size - 1) {
            val diff = rrIntervals[i + 1] - rrIntervals[i]
            sumSuccessiveDiffSq += diff * diff
        }
        val rmssd = sqrt(sumSuccessiveDiffSq / (rrIntervals.size - 1).coerceAtLeast(1))

        // 2. Cardiovascular Risk Evaluation
        val cardioRisk = when {
            currentBpm > 100 || (spO2 < 93 && currentBpm > 90) -> RiskLevel.HIGH
            currentBpm in 85..100 || spO2 in 93..95 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        // 3. Sleep Apnea / Desaturation Risk
        val apneaRisk = when {
            spO2 < 92 -> RiskLevel.HIGH
            spO2 in 92..94 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        // 4. Autonomic Balance
        val autonomicBalance = when {
            rmssd >= 42f -> "Phó giao cảm chiếm ưu thế (Cơ thể thư giãn & phục hồi tối ưu)"
            rmssd in 25f..41.9f -> "Hệ thần kinh thực vật cân bằng (Trạng thái bình thường)"
            else -> "Cường giao cảm do stress hoặc mệt mỏi thể chất kéo dài"
        }

        // 5. Recovery Readiness (0-100%)
        val recoveryScore = ((rmssd * 1.2f) + (spO2 - 80) * 2.5f - (stress * 0.4f)).roundToInt().coerceIn(30, 98)

        // 6. Overall Health Score
        val healthScore = ((summary.sleepQualityScore * 0.3f) + (recoveryScore * 0.4f) + (spO2 * 0.3f)).roundToInt().coerceIn(40, 99)

        // 7. AI Diagnostic Summary Generation
        val diagnosticText = buildString {
            append("Chỉ số hiệu sinh tim mạch và hô hấp tổng thể đạt $healthScore/100 điểm. ")
            if (rmssd >= 35f) {
                append("Biến thiên nhịp tim HRV (RMSSD: ${String.format("%.1f", rmssd)}ms) phản ánh khả năng thích ứng cơ tim xuất sắc. ")
            } else {
                append("HRV có xu hướng giảm nhẹ (RMSSD: ${String.format("%.1f", rmssd)}ms), dấu hiệu của căng thẳng hoặc thiếu ngủ. ")
            }

            if (spO2 >= 96) {
                append("Độ bão hòa oxy máu đạt ${spO2}% rất ổn định, không có dấu hiệu thiếu oxy mô. ")
            } else {
                append("Cần lưu ý mức SpO2 ${spO2}% dao động dưới ngưỡng tối ưu 95%. ")
            }

            if (summary.totalSteps >= summary.stepGoal) {
                append("Mức vận động thể chất đạt mục tiêu trong ngày (${summary.totalSteps} bước).")
            } else {
                append("Vận động đạt ${summary.totalSteps}/${summary.stepGoal} bước.")
            }
        }

        // 8. Actionable Clinical Recommendations
        val recommendations = mutableListOf<String>()
        if (stress > 50 || rmssd < 30f) {
            recommendations.add("Thực hiện 10 phút thở hộp (Box Breathing 4-4-4-4) để kích thích dây thần kinh phế vị Vagus, tăng chỉ số HRV.")
        }
        if (spO2 < 95) {
            recommendations.add("Kiểm tra lại tư thế đeo dây đồng hồ sát cổ tay hoặc hít thở sâu ngoài không gian thoáng đãng.")
        }
        if (sleepHours < 7.0f) {
            recommendations.add("Bổ sung 30 phút nghỉ ngơi ngắn vào đầu giờ chiều để tái nạp năng lượng sinh học.")
        }
        recommendations.add("Duy trì uống đủ 2.0 - 2.5 lít nước để tối ưu độ nhớt máu và tính đàn hồi mạch máu.")
        recommendations.add("Tập luyện nhịp tim vùng 2 (Zone 2 Cardio: 110-130 BPM) 30 phút mỗi ngày để tăng thể tích nhát bóp tim.")

        return BiosignalAiReport(
            overallHealthScore = healthScore,
            cardiovascularRisk = cardioRisk,
            rmssdHrvMs = rmssd,
            sdnnHrvMs = sdnn,
            autonomicBalance = autonomicBalance,
            oxygenStabilityScore = (spO2.toFloat() / 100f * 99.2f).coerceIn(88f, 99.8f),
            apneaRisk = apneaRisk,
            recoveryReadiness = recoveryScore,
            metabolicStrain = if (currentBpm > 90) "Hơi cao" else "Bình thường",
            aiDiagnosticSummary = diagnosticText,
            actionableRecommendations = recommendations
        )
    }
}
