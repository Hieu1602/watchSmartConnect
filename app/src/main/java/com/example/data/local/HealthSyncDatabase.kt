package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.HealthAlert
import com.example.data.model.HealthMetricRecord
import com.example.data.model.SmartWatchDevice
import com.example.data.model.SyncLogEntry

@Database(
    entities = [
        SmartWatchDevice::class,
        HealthMetricRecord::class,
        SyncLogEntry::class,
        HealthAlert::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(HealthSyncConverters::class)
abstract class HealthSyncDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun healthAlertDao(): HealthAlertDao

    companion object {
        @Volatile
        private var INSTANCE: HealthSyncDatabase? = null

        fun getDatabase(context: Context): HealthSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthSyncDatabase::class.java,
                    "health_sync.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
