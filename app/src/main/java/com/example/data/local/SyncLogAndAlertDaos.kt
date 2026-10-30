package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HealthAlert
import com.example.data.model.SyncLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsFlow(): Flow<List<SyncLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncLogEntry): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}

@Dao
interface HealthAlertDao {
    @Query("SELECT * FROM health_alerts ORDER BY timestamp DESC LIMIT 20")
    fun getAlertsFlow(): Flow<List<HealthAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: HealthAlert): Long

    @Query("UPDATE health_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: Long)

    @Query("DELETE FROM health_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Long)
}
