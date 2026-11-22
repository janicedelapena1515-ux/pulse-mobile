package com.example.data.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.domain.model.InstalledAppCandidate
import java.io.File

object SystemIntentsHelper {

    fun openStorageSettings(context: Context): Boolean {
        val manageIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Intent(StorageManager.ACTION_MANAGE_STORAGE)
        } else {
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        }
        return launchSafely(context, manageIntent)
            || launchSafely(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    }

    fun openAppManagementSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            || launchSafely(context, Intent(Settings.ACTION_APPLICATION_SETTINGS))
    }

    fun openBatterySaverSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            || launchSafely(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            || openBatterySaverSettings(context)
    }

    fun openDisplaySettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    fun openDeveloperAnimationSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            || launchSafely(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    }

    fun openNetworkSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_WIFI_SETTINGS))
            || launchSafely(context, Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openDndPolicyAccessSettings(context: Context): Boolean {
        return launchSafely(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun openWriteSettingsAccess(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        return launchSafely(context, intent)
    }

    fun openAppDetails(context: Context, packageName: String = context.packageName): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        return launchSafely(context, intent)
    }

    fun applyDoNotDisturbIfPermitted(context: Context, enable: Boolean): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        return runCatching {
            val filter = if (enable) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            nm.setInterruptionFilter(filter)
            true
        }.getOrDefault(false)
    }

    fun applyBrightnessIfPermitted(context: Context, brightness0To255: Int): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness0To255.coerceIn(15, 255)
            )
            true
        }.getOrDefault(false)
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun openUriWithExternalViewer(context: Context, uriString: String, mimeType: String): Boolean {
        return runCatching {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun queryLaunchableApps(context: Context): List<InstalledAppCandidate> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        val selfPkg = context.packageName
        return resolved
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (appInfo.packageName == selfPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }
                InstalledAppCandidate(
                    packageName = appInfo.packageName,
                    appName = label,
                    isCategoryGame = isGame
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<InstalledAppCandidate> { it.isCategoryGame }.thenBy { it.appName.lowercase() })
    }

    private fun launchSafely(context: Context, intent: Intent): Boolean {
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
