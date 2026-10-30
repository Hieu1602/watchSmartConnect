package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceName: String,
    val deviceBrand: String,
    val metricTypes: String, // "HeartRate, SpO2, Steps"
    val durationMs: Long,
    val latencyMs: Long,
    val packetCount: Int,
    val accuracy: Float,
    val status: String, // "SUCCESS", "RETRY_RESOLVED", "WARNING", "FAILED"
    val message: String
)

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Entity(tableName = "health_alerts")
data class HealthAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val metricType: String = "HEART_RATE"
)
