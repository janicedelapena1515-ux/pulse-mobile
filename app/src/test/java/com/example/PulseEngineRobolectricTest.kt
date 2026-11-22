package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GameAppEntity
import com.example.data.local.OptimizationLogEntity
import com.example.data.local.PulseDatabase
import com.example.data.service.DeviceTelemetryService
import com.example.data.service.OptimizationEngine
import com.example.data.service.StorageScannerService
import com.example.domain.model.BottleneckSeverity
import com.example.domain.model.DeviceTelemetry
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.PulseMobileTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PulseEngineRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: PulseDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `pulse score accurately penalizes low storage high ram and overheating`() {
        val telemetryService = DeviceTelemetryService(context)

        val healthy = DeviceTelemetry(
            ramPercent = 45f,
            storagePercent = 50f,
            batteryTempC = 31f,
            batteryLevel = 85,
            isLowMemory = false,
            networkTransport = "Wi-Fi"
        )
        val healthyScore = telemetryService.computePulseScore(healthy, 0L)
        assertTrue(healthyScore.totalScore >= 88)
        assertTrue(healthyScore.bottlenecks.isEmpty())

        val stressed = DeviceTelemetry(
            ramPercent = 91f,
            storagePercent = 92f,
            batteryTempC = 44.5f,
            batteryLevel = 15,
            isCharging = false,
            isLowMemory = true,
            networkTransport = "Offline"
        )
        val stressedScore = telemetryService.computePulseScore(stressed, 0L)
        assertTrue(stressedScore.totalScore < 50)
        assertTrue(stressedScore.bottlenecks.any { it.severity == BottleneckSeverity.HIGH })
        assertTrue(stressedScore.bottlenecks.any { it.id == "low_storage" })
        assertTrue(stressedScore.bottlenecks.any { it.id == "high_ram_pressure" })
        assertTrue(stressedScore.bottlenecks.any { it.id == "elevated_thermal" })
    }

    @Test
    fun `storage scanner and optimization engine genuinely clear app cache and report exact bytes`() = runTest {
        val tempFile = File(context.cacheDir, "test_cache_blob.bin")
        val payload = ByteArray(4096) { 0x5A }
        tempFile.writeBytes(payload)
        assertTrue(tempFile.exists())

        val telemetryService = DeviceTelemetryService(context)
        val scannerService = StorageScannerService(context)
        val engine = OptimizationEngine(telemetryService, scannerService)

        val scanSummary = scannerService.scanAllSources(
            folders = emptyList(),
            largeFileThresholdMb = 25,
            oldDownloadDays = 30,
            onFolderStatsUpdated = {}
        )
        assertTrue(scanSummary.appCacheBytes >= 4096L)

        val report = engine.runFullDeviceAnalysisAndSafeBoost(
            scanSummary = scanSummary,
            autoClearAppCache = true
        )
        assertTrue(report.bytesRecovered >= 4096L)
        assertTrue(!tempFile.exists())
    }

    @Test
    fun `room database persists optimization logs and game library`() = runTest {
        db.optimizationLogDao().insertLog(
            OptimizationLogEntity(
                timestampMillis = 123456L,
                scoreBefore = 78,
                scoreAfter = 82,
                bytesRecovered = 2048L,
                actionsSummary = "Cleared app cache",
                honestNotes = "Recovered 2.00 KB"
            )
        )
        val logs = db.optimizationLogDao().observeLogs().first()
        assertEquals(1, logs.size)
        assertEquals(2048L, db.optimizationLogDao().observeTotalBytesRecovered().first())

        db.gameLibraryDao().upsertGame(
            GameAppEntity(
                packageName = "com.example.racing",
                appName = "Cherry Drift",
                preferredProfile = "PERFORMANCE"
            )
        )
        val games = db.gameLibraryDao().observeGames().first()
        assertEquals(1, games.size)
        assertEquals("Cherry Drift", games.first().appName)
    }

    @Test
    fun `home screen renders pulse score ring and boost button`() {
        val telemetry = DeviceTelemetryService(context).computePulseScore(
            DeviceTelemetry(deviceName = "Cherry Phone", androidVersion = "15"),
            0L
        ).let {
            DeviceTelemetry(
                deviceName = "Cherry Phone",
                androidVersion = "15",
                ramTotalBytes = 8L * 1024 * 1024 * 1024,
                ramAvailBytes = 4L * 1024 * 1024 * 1024,
                ramUsedBytes = 4L * 1024 * 1024 * 1024,
                ramPercent = 50f,
                storageTotalBytes = 128L * 1024 * 1024 * 1024,
                storageAvailBytes = 64L * 1024 * 1024 * 1024,
                storageUsedBytes = 64L * 1024 * 1024 * 1024,
                storagePercent = 50f,
                batteryLevel = 88,
                batteryTempC = 32.5f,
                pulseScore = it
            )
        }

        composeTestRule.setContent {
            PulseMobileTheme {
                HomeScreen(
                    telemetry = telemetry,
                    isScanning = false,
                    scanProgress = 0f,
                    scanStageMessage = "",
                    lastReport = null,
                    recentLogs = emptyList(),
                    totalBytesRecovered = 0L,
                    onBoostClick = {},
                    onOpenSettings = {},
                    onNavigateToBoost = {},
                    onNavigateToStorageCleaner = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pulse_score_ring").assertIsDisplayed()
        composeTestRule.onNodeWithTag("boost_my_device_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("BOOST MY DEVICE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed()
    }
}
