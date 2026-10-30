package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConnectionStatus
import com.example.data.model.SmartWatchDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM smartwatch_devices ORDER BY isPrimary DESC, name ASC")
    fun getAllDevicesFlow(): Flow<List<SmartWatchDevice>>

    @Query("SELECT * FROM smartwatch_devices")
    suspend fun getAllDevices(): List<SmartWatchDevice>

    @Query("SELECT * FROM smartwatch_devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): SmartWatchDevice?

    @Query("SELECT * FROM smartwatch_devices WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryDevice(): SmartWatchDevice?

    @Query("SELECT * FROM smartwatch_devices WHERE status = 'CONNECTED'")
    suspend fun getConnectedDevices(): List<SmartWatchDevice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: SmartWatchDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<SmartWatchDevice>)

    @Update
    suspend fun update(device: SmartWatchDevice)

    @Delete
    suspend fun delete(device: SmartWatchDevice)

    @Query("UPDATE smartwatch_devices SET isPrimary = 0")
    suspend fun clearPrimaryFlag()

    @Query("UPDATE smartwatch_devices SET isPrimary = 1 WHERE id = :deviceId")
    suspend fun setPrimary(deviceId: String)

    @Query("UPDATE smartwatch_devices SET status = :status, lastSyncTimestamp = :timestamp WHERE id = :deviceId")
    suspend fun updateStatus(deviceId: String, status: ConnectionStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE smartwatch_devices SET batteryLevel = :battery, rssi = :rssi, lastSyncTimestamp = :timestamp WHERE id = :deviceId")
    suspend fun updateTelemetry(deviceId: String, battery: Int, rssi: Int, timestamp: Long = System.currentTimeMillis())
}
