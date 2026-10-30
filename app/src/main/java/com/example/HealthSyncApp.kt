package com.example

import android.app.Application
import com.example.data.local.HealthSyncDatabase
import com.example.data.repository.HealthSyncRepository
import com.example.engine.BleScannerManager
import com.example.engine.SmartWatchSyncEngine
import com.example.engine.ThirdPartySyncBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthSyncApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: HealthSyncDatabase
        private set

    lateinit var repository: HealthSyncRepository
        private set

    lateinit var syncEngine: SmartWatchSyncEngine
        private set

    lateinit var bleScanner: BleScannerManager
        private set

    lateinit var thirdPartyBridge: ThirdPartySyncBridge
        private set

    override fun onCreate() {
        super.onCreate()
        database = HealthSyncDatabase.getDatabase(this)
        repository = HealthSyncRepository(database)
        syncEngine = SmartWatchSyncEngine(this, database, applicationScope)
        bleScanner = BleScannerManager(this, applicationScope)
        thirdPartyBridge = ThirdPartySyncBridge(this)

        applicationScope.launch {
            repository.initializePreloadedDevicesIfEmpty()
        }
    }
}
