package com.example.data.service

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.domain.model.BottleneckSeverity
import com.example.domain.model.DeviceTelemetry
import com.example.domain.model.PerformanceBottleneck
import com.example.domain.model.PulseScoreBreakdown
import com.example.domain.model.TelemetryHistory
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DeviceTelemetryService(private val context: Context) {

    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _history = MutableStateFlow(TelemetryHistory())
    val history: StateFlow<TelemetryHistory> = _history.asStateFlow()

    private var prevRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var prevTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var prevSampleTimeMs: Long = SystemClock.elapsedRealtime()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    @Volatile
    private var latestAmbientTempC: Float? = null
    private var sensorRegistered = false

    private val ambientSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val value = event?.values?.firstOrNull() ?: return
            if (value in -40f..110f) {
                latestAmbientTempC = value
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun startSensorMonitoring() {
        if (sensorRegistered || sensorManager == null) return
        val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (tempSensor != null) {
            sensorRegistered = sensorManager.registerListener(
                ambientSensorListener,
                tempSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stopSensorMonitoring() {
        if (!sensorRegistered || sensorManager == null) return
        sensorManager.unregisterListener(ambientSensorListener)
        sensorRegistered = false
    }

    suspend fun refreshSnapshot(scannedClutterBytes: Long = 0L): DeviceTelemetry =
        withContext(Dispatchers.IO) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val nowWall = System.currentTimeMillis()

            // 1. Device Identity
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }
            val socLabel = readSocLabel()
            val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

            // 2. RAM via ActivityManager
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            val ramTotal = memInfo.totalMem.coerceAtLeast(1L)
            val ramAvail = memInfo.availMem.coerceIn(0L, ramTotal)
            val ramUsed = (ramTotal - ramAvail).coerceAtLeast(0L)
            val ramPercent = ((ramUsed.toDouble() / ramTotal.toDouble()) * 100.0).toFloat()

            // 3. Storage via StatFs & StorageManager
            val statFs = StatFs(Environment.getDataDirectory().path)
            val storageTotal = statFs.totalBytes.coerceAtLeast(1L)
            val storageAvail = statFs.availableBytes.coerceIn(0L, storageTotal)
            val storageUsed = (storageTotal - storageAvail).coerceAtLeast(0L)
            val storagePercent = ((storageUsed.toDouble() / storageTotal.toDouble()) * 100.0).toFloat()
            val appCacheBytes = computeDirSize(context.cacheDir) +
                computeDirSize(context.codeCacheDir) +
                (context.externalCacheDir?.let { computeDirSize(it) } ?: 0L)

            val allocatableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                    sm?.getAllocatableBytes(StorageManager.UUID_DEFAULT)
                }.getOrNull()
            } else null

            // 4. Battery via Sticky Broadcast
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val rawLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val rawScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val batteryLevel = if (rawLevel >= 0 && rawScale > 0) {
                ((rawLevel * 100f) / rawScale).toInt().coerceIn(0, 100)
            } else 0

            val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val batteryTempC = if (rawTemp > 0) rawTemp / 10f else null

            val rawVoltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            val batteryVoltageMv = if (rawVoltage > 0) rawVoltage else null

            val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL
            val batteryStatusLabel = when (statusInt) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            val pluggedInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val batteryPluggedLabel = when (pluggedInt) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }

            val healthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val batteryHealthLabel = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Nominal"
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSaveMode = powerManager?.isPowerSaveMode == true

            // 5. Thermal & Sensors
            val thermalCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { powerManager?.currentThermalStatus }.getOrNull()
            } else null

            val thermalStatusLabel = when (thermalCode) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Heat"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Thermal"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Thermal"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown Risk"
                else -> when {
                    batteryTempC == null -> "Unavailable"
                    batteryTempC >= 43f -> "Warm (${FormatUtils.formatTempC(batteryTempC)})"
                    batteryTempC >= 38f -> "Elevated (${FormatUtils.formatTempC(batteryTempC)})"
                    else -> "Nominal (${FormatUtils.formatTempC(batteryTempC)})"
                }
            }

            val sensorNames = sensorManager?.getSensorList(Sensor.TYPE_ALL)
                ?.map { it.name }
                ?.distinct()
                ?.take(12)
                ?: emptyList()

            // 6. CPU cores & sysfs frequency probing (honest fallback if SELinux blocks)
            val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val curFreqs = List(cpuCores) { idx ->
                readIntFromSysfs("/sys/devices/system/cpu/cpu$idx/cpufreq/scaling_cur_freq")?.div(1000)
            }
            val maxFreqs = List(cpuCores) { idx ->
                readIntFromSysfs("/sys/devices/system/cpu/cpu$idx/cpufreq/cpuinfo_max_freq")?.div(1000)
            }
            val minFreqs = List(cpuCores) { idx ->
                readIntFromSysfs("/sys/devices/system/cpu/cpu$idx/cpufreq/cpuinfo_min_freq")?.div(1000)
            }
            val validCur = curFreqs.filterNotNull()
            val validMax = maxFreqs.filterNotNull()
            val cpuAvgCur = if (validCur.isNotEmpty()) validCur.average().toInt() else null
            val cpuPeakMax = validMax.maxOrNull()

            val normalizedFreqRatios = (0 until cpuCores).mapNotNull { i ->
                val cur = curFreqs[i]
                val max = maxFreqs[i]
                val min = minFreqs[i] ?: 0
                if (cur != null && max != null && max > min) {
                    (((cur - min).toFloat() / (max - min).toFloat()) * 100f).coerceIn(0f, 100f)
                } else null
            }
            val cpuNormalizedLoad = if (normalizedFreqRatios.isNotEmpty()) {
                normalizedFreqRatios.average().toFloat()
            } else null

            val cpuAccessNote = if (validCur.isNotEmpty()) {
                "Live sysfs frequency scaling (${validCur.size}/$cpuCores cores readable)"
            } else {
                "Per-core frequency & /proc/stat restricted by Android SELinux on this OS build"
            }

            // 7. Network (TrafficStats & ConnectivityManager)
            val currRx = TrafficStats.getTotalRxBytes()
            val currTx = TrafficStats.getTotalTxBytes()
            val dtMs = (nowElapsed - prevSampleTimeMs).coerceAtLeast(1L)
            val rxRate = if (currRx != TrafficStats.UNSUPPORTED.toLong() &&
                prevRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
                currRx >= prevRxBytes &&
                nowElapsed > prevSampleTimeMs
            ) {
                ((currRx - prevRxBytes) * 1000L) / dtMs
            } else null
            val txRate = if (currTx != TrafficStats.UNSUPPORTED.toLong() &&
                prevTxBytes != TrafficStats.UNSUPPORTED.toLong() &&
                currTx >= prevTxBytes &&
                nowElapsed > prevSampleTimeMs
            ) {
                ((currTx - prevTxBytes) * 1000L) / dtMs
            } else null

            prevRxBytes = currRx
            prevTxBytes = currTx
            prevSampleTimeMs = nowElapsed

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val networkTransport = when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Connected"
            }
            val isMetered = cm?.isActiveNetworkMetered == true
            val linkDownKbps = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 }
            val linkUpKbps = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }

            // 8. Display & System Settings
            val refreshRate = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display?.refreshRate
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.let { it.refreshRate }
                }
            }.getOrNull() ?: 60f

            val brightness = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            }.getOrNull()
            val isAutoBrightness = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }.getOrDefault(false)

            val winAnim = readGlobalFloat(Settings.Global.WINDOW_ANIMATION_SCALE)
            val transAnim = readGlobalFloat(Settings.Global.TRANSITION_ANIMATION_SCALE)
            val durAnim = readGlobalFloat(Settings.Global.ANIMATOR_DURATION_SCALE)
            val systemReducedAnim = (durAnim != null && durAnim == 0f) || isPowerSaveMode

            // 9. Permissions / Capabilities
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val hasDnd = nm?.isNotificationPolicyAccessGranted == true
            val hasWriteSettings = Settings.System.canWrite(context)
            val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            val baseTelemetry = DeviceTelemetry(
                deviceName = deviceName,
                manufacturer = manufacturer,
                model = model,
                androidVersion = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                socLabel = socLabel,
                primaryAbi = primaryAbi,
                ramTotalBytes = ramTotal,
                ramAvailBytes = ramAvail,
                ramUsedBytes = ramUsed,
                ramPercent = ramPercent,
                isLowMemory = memInfo.lowMemory,
                ramThresholdBytes = memInfo.threshold,
                storageTotalBytes = storageTotal,
                storageAvailBytes = storageAvail,
                storageUsedBytes = storageUsed,
                storagePercent = storagePercent,
                appOwnCacheBytes = appCacheBytes,
                allocatableBytes = allocatableBytes,
                batteryLevel = batteryLevel,
                batteryTempC = batteryTempC,
                batteryStatusLabel = batteryStatusLabel,
                batteryPluggedLabel = batteryPluggedLabel,
                batteryHealthLabel = batteryHealthLabel,
                batteryVoltageMv = batteryVoltageMv,
                isCharging = isCharging,
                isPowerSaveMode = isPowerSaveMode,
                cpuCores = cpuCores,
                cpuFrequenciesMhz = curFreqs,
                cpuMaxFrequenciesMhz = maxFreqs,
                cpuAvgCurrentMhz = cpuAvgCur,
                cpuPeakMaxMhz = cpuPeakMax,
                cpuNormalizedLoadPercent = cpuNormalizedLoad,
                cpuAccessNote = cpuAccessNote,
                thermalStatusLabel = thermalStatusLabel,
                thermalSeverityCode = thermalCode,
                ambientTempC = latestAmbientTempC,
                sensorNames = sensorNames,
                uptimeMillis = nowElapsed,
                awakeMillis = SystemClock.uptimeMillis(),
                networkTransport = networkTransport,
                isNetworkMetered = isMetered,
                rxBytesPerSec = rxRate,
                txBytesPerSec = txRate,
                totalRxBytes = currRx.takeIf { it != TrafficStats.UNSUPPORTED.toLong() },
                totalTxBytes = currTx.takeIf { it != TrafficStats.UNSUPPORTED.toLong() },
                linkDownKbps = linkDownKbps,
                linkUpKbps = linkUpKbps,
                displayRefreshRateHz = refreshRate,
                screenBrightness = brightness,
                isAutoBrightness = isAutoBrightness,
                windowAnimScale = winAnim,
                transitionAnimScale = transAnim,
                animatorDurationScale = durAnim,
                hasDndPolicyAccess = hasDnd,
                hasWriteSettingsAccess = hasWriteSettings,
                hasPostNotificationsAccess = hasNotifications,
                systemReducedAnimations = systemReducedAnim,
                updatedAtMillis = nowWall
            )

            val score = computePulseScore(baseTelemetry, scannedClutterBytes)
            val fullTelemetry = baseTelemetry.copy(pulseScore = score)
            _telemetry.value = fullTelemetry
            appendHistory(fullTelemetry)
            fullTelemetry
        }

    private fun appendHistory(t: DeviceTelemetry) {
        val current = _history.value
        val maxSamples = 36
        _history.value = current.copy(
            ramPercentHistory = (current.ramPercentHistory + t.ramPercent).takeLast(maxSamples),
            cpuFreqNormalizedHistory = if (t.cpuNormalizedLoadPercent != null) {
                (current.cpuFreqNormalizedHistory + t.cpuNormalizedLoadPercent).takeLast(maxSamples)
            } else {
                current.cpuFreqNormalizedHistory
            },
            netRxKbpsHistory = ((current.netRxKbpsHistory + ((t.rxBytesPerSec ?: 0L) / 1024f)))
                .takeLast(maxSamples),
            netTxKbpsHistory = ((current.netTxKbpsHistory + ((t.txBytesPerSec ?: 0L) / 1024f)))
                .takeLast(maxSamples),
            batteryTempHistory = if (t.batteryTempC != null) {
                (current.batteryTempHistory + t.batteryTempC).takeLast(maxSamples)
            } else {
                current.batteryTempHistory
            }
        )
    }

    fun computePulseScore(t: DeviceTelemetry, scannedClutterBytes: Long): PulseScoreBreakdown {
        val bottlenecks = mutableListOf<PerformanceBottleneck>()

        // 1. Storage Headroom (30 pts)
        val freeStoragePct = (100f - t.storagePercent).coerceIn(0f, 100f)
        val storageScore = when {
            freeStoragePct >= 30f -> 30
            freeStoragePct >= 15f -> (20 + ((freeStoragePct - 15f) / 15f) * 10f).toInt()
            else -> ((freeStoragePct / 15f) * 20f).toInt()
        }.coerceIn(0, 30)

        if (freeStoragePct < 15f) {
            bottlenecks += PerformanceBottleneck(
                id = "low_storage",
                title = "Low Storage Headroom (${FormatUtils.formatOneDecimal(freeStoragePct)}% free)",
                detail = "Flash storage slows down significantly when free space falls below 15%.",
                recommendation = "Use Smart Storage Cleaner or System Storage Manager to remove large or duplicate files.",
                severity = BottleneckSeverity.HIGH
            )
        } else if (scannedClutterBytes > 50L * 1024L * 1024L) {
            bottlenecks += PerformanceBottleneck(
                id = "scanned_clutter",
                title = "Reclaimable Files Detected",
                detail = "Your scanned folders contain duplicate, large, or aged download files.",
                recommendation = "Review flagged items in Smart Storage Cleaner.",
                severity = BottleneckSeverity.MODERATE
            )
        }

        // 2. Memory Headroom (25 pts)
        val freeRamPct = (100f - t.ramPercent).coerceIn(0f, 100f)
        val memoryScore = when {
            t.isLowMemory -> 6
            freeRamPct >= 35f -> 25
            freeRamPct >= 18f -> (15 + ((freeRamPct - 18f) / 17f) * 10f).toInt()
            else -> ((freeRamPct / 18f) * 15f).toInt()
        }.coerceIn(0, 25)

        if (t.isLowMemory || freeRamPct < 18f) {
            bottlenecks += PerformanceBottleneck(
                id = "high_ram_pressure",
                title = "High Memory Pressure (${t.ramPercent.toInt()}% used)",
                detail = "Android LMK may reclaim background apps aggressively when free RAM drops low.",
                recommendation = "Open App Management to restrict heavy background apps you rarely use.",
                severity = if (t.isLowMemory) BottleneckSeverity.HIGH else BottleneckSeverity.MODERATE
            )
        }

        // 3. Thermal & Battery Health (25 pts)
        val temp = t.batteryTempC
        val thermalScore = when {
            temp == null -> 20
            temp <= 36f -> 25
            temp <= 40f -> 20
            temp <= 43f -> 13
            else -> 6
        }
        if (temp != null && temp >= 40f) {
            bottlenecks += PerformanceBottleneck(
                id = "elevated_thermal",
                title = "Elevated Battery Temperature (${FormatUtils.formatTempC(temp)})",
                detail = "Thermal throttling reduces peak CPU/GPU sustainability and accelerates battery wear.",
                recommendation = "Lower brightness, pause fast charging while gaming, or enable Battery Saver.",
                severity = if (temp >= 43f) BottleneckSeverity.HIGH else BottleneckSeverity.MODERATE
            )
        }
        if (t.batteryLevel in 1..19 && !t.isCharging) {
            bottlenecks += PerformanceBottleneck(
                id = "low_battery",
                title = "Low Battery (${t.batteryLevel}%)",
                detail = "Low charge levels can trigger OS power-throttling profiles.",
                recommendation = "Enable Battery Saver or connect a charger.",
                severity = BottleneckSeverity.MODERATE
            )
        }

        // 4. System Readiness (20 pts)
        var readiness = 20
        if (t.appOwnCacheBytes > 15L * 1024L * 1024L) readiness -= 3
        if (t.isPowerSaveMode) {
            readiness -= 3
            bottlenecks += PerformanceBottleneck(
                id = "power_save_active",
                title = "System Battery Saver Active",
                detail = "Android Battery Saver limits background sync and reduces peak responsiveness.",
                recommendation = "Disable Battery Saver in Battery settings when maximum responsiveness is needed.",
                severity = BottleneckSeverity.INFO
            )
        }
        if (t.networkTransport == "Offline") readiness -= 2
        readiness = readiness.coerceIn(0, 20)

        val total = (storageScore + memoryScore + thermalScore + readiness).coerceIn(0, 100)
        val rating = when {
            total >= 88 -> "Peak Luxury"
            total >= 72 -> "Optimal"
            total >= 55 -> "Balanced"
            else -> "Needs Attention"
        }

        return PulseScoreBreakdown(
            totalScore = total,
            storageScore = storageScore,
            memoryScore = memoryScore,
            thermalBatteryScore = thermalScore,
            readinessScore = readiness,
            ratingLabel = rating,
            bottlenecks = bottlenecks
        )
    }

    private fun readSocLabel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (!soc.isNullOrBlank() && soc != Build.UNKNOWN) return soc
        }
        return runCatching {
            File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull {
                    it.startsWith("Hardware", ignoreCase = true) ||
                        it.startsWith("model name", ignoreCase = true)
                }?.substringAfter(":")?.trim()?.takeIf { it.isNotBlank() }
            }
        }.getOrNull() ?: Build.HARDWARE.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: "Unavailable"
    }

    private fun readIntFromSysfs(path: String): Int? {
        return runCatching {
            val file = File(path)
            if (file.canRead()) {
                file.readText().trim().toIntOrNull()
            } else null
        }.getOrNull()
    }

    private fun readGlobalFloat(key: String): Float? {
        return runCatching {
            Settings.Global.getFloat(context.contentResolver, key)
        }.getOrNull()
    }

    private fun computeDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var sum = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) sum += file.length()
        }
        return sum
    }
}
