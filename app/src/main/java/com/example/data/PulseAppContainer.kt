package com.example.data

import android.content.Context
import com.example.data.local.PreferencesRepository
import com.example.data.local.PulseDatabase
import com.example.data.local.PulseLocalRepository
import com.example.data.service.DeviceTelemetryService
import com.example.data.service.OptimizationEngine
import com.example.data.service.StorageScannerService

class PulseAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: PulseDatabase by lazy { PulseDatabase.getInstance(appContext) }
    val localRepository: PulseLocalRepository by lazy { PulseLocalRepository(database) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(appContext) }
    val telemetryService: DeviceTelemetryService by lazy { DeviceTelemetryService(appContext) }
    val storageScannerService: StorageScannerService by lazy { StorageScannerService(appContext) }
    val optimizationEngine: OptimizationEngine by lazy {
        OptimizationEngine(telemetryService, storageScannerService)
    }

    companion object {
        @Volatile
        private var INSTANCE: PulseAppContainer? = null

        fun get(context: Context): PulseAppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PulseAppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
