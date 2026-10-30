package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AlertSeverity
import com.example.data.model.ConnectionStatus
import com.example.data.model.ConnectionType
import com.example.data.model.SmartWatchBrand

class HealthSyncConverters {
    @TypeConverter
    fun fromBrand(brand: SmartWatchBrand): String = brand.name

    @TypeConverter
    fun toBrand(value: String): SmartWatchBrand =
        try { SmartWatchBrand.valueOf(value) } catch (e: Exception) { SmartWatchBrand.GENERIC_BLE }

    @TypeConverter
    fun fromConnectionType(type: ConnectionType): String = type.name

    @TypeConverter
    fun toConnectionType(value: String): ConnectionType =
        try { ConnectionType.valueOf(value) } catch (e: Exception) { ConnectionType.BLUETOOTH_LE }

    @TypeConverter
    fun fromStatus(status: ConnectionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ConnectionStatus =
        try { ConnectionStatus.valueOf(value) } catch (e: Exception) { ConnectionStatus.DISCONNECTED }

    @TypeConverter
    fun fromSeverity(severity: AlertSeverity): String = severity.name

    @TypeConverter
    fun toSeverity(value: String): AlertSeverity =
        try { AlertSeverity.valueOf(value) } catch (e: Exception) { AlertSeverity.INFO }
}
