package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_metrics",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"])
    ]
)
data class HealthMetricRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int, // BPM (e.g. 72)
    val steps: Int, // Steps accumulated
    val spO2: Int, // % (e.g. 98)
    val stressLevel: Int, // 0..100
    val caloriesBurned: Int, // kcal
    val activeMinutes: Int, // mins
    val temperatureCelsius: Float = 36.6f,
    val sleepStage: String = "AWAKE" // DEEP, LIGHT, REM, AWAKE
)

enum class SyncConflictPolicy(val title: String, val description: String) {
    PRIMARY_DEVICE_PRIORITY(
        "Ưu tiên đồng hồ chính",
        "Dữ liệu từ đồng hồ chính được coi là chuẩn tuyệt đối"
    ),
    WEIGHTED_CONSENSUS(
        "Trọng số độ chính xác",
        "Lấy trung bình có trọng số theo xếp hạng độ chính xác phần cứng của từng hãng"
    ),
    HIGHEST_PRECISION(
        "Giá trị chính xác nhất",
        "Tự động loại bỏ rung nhiễu và chọn cảm biến có chỉ số ổn định nhất"
    )
}

data class AggregatedHealthSummary(
    val liveHeartRate: Int = 74,
    val minHeartRate: Int = 58,
    val maxHeartRate: Int = 138,
    val totalSteps: Int = 6840,
    val stepGoal: Int = 10000,
    val currentSpO2: Int = 98,
    val caloriesBurned: Int = 460,
    val activeMinutes: Int = 42,
    val stressScore: Int = 28, // 0-100 (low is relaxed)
    val bodyBattery: Int = 82, // 0-100%
    val sleepHours: Float = 7.4f,
    val sleepQualityScore: Int = 86,
    val lastSyncTimeEpoch: Long = System.currentTimeMillis(),
    val activeDeviceCount: Int = 0,
    val syncConsensusPercent: Float = 99.1f,
    val recentBpmWave: List<Int> = emptyList()
)
