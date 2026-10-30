package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HealthMetricRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {
    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT 100")
    fun getRecentMetricsFlow(): Flow<List<HealthMetricRecord>>

    @Query("SELECT * FROM health_metrics WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 60")
    fun getMetricsForDevice(deviceId: String): Flow<List<HealthMetricRecord>>

    @Query("SELECT * FROM health_metrics WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getMetricsSince(sinceTimestamp: Long): List<HealthMetricRecord>

    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetric(): HealthMetricRecord?

    @Query("SELECT AVG(heartRate) FROM health_metrics WHERE timestamp >= :sinceTimestamp")
    suspend fun getAverageHeartRateSince(sinceTimestamp: Long): Double?

    @Query("SELECT MAX(heartRate) FROM health_metrics WHERE timestamp >= :sinceTimestamp")
    suspend fun getMaxHeartRateSince(sinceTimestamp: Long): Int?

    @Query("SELECT MIN(heartRate) FROM health_metrics WHERE timestamp >= :sinceTimestamp AND heartRate > 0")
    suspend fun getMinHeartRateSince(sinceTimestamp: Long): Int?

    @Query("SELECT MAX(steps) FROM health_metrics WHERE timestamp >= :sinceTimestamp")
    suspend fun getMaxStepsToday(sinceTimestamp: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HealthMetricRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthMetricRecord>)

    @Query("DELETE FROM health_metrics WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOldMetrics(olderThanTimestamp: Long)
}
