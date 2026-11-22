package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.GamingProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pulseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulse_preferences"
)

data class UserPreferences(
    val monitorRefreshMs: Long = 1000L,
    val reducedAnimations: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val largeFileThresholdMb: Int = 25,
    val oldDownloadDays: Int = 30,
    val scheduledHealthCheckEnabled: Boolean = false,
    val activeGamingProfile: GamingProfile = GamingProfile.BALANCED
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MONITOR_REFRESH_MS = longPreferencesKey("monitor_refresh_ms")
        val REDUCED_ANIMATIONS = booleanPreferencesKey("reduced_animations")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val LARGE_FILE_THRESHOLD_MB = intPreferencesKey("large_file_threshold_mb")
        val OLD_DOWNLOAD_DAYS = intPreferencesKey("old_download_days")
        val SCHEDULED_HEALTH_CHECK = booleanPreferencesKey("scheduled_health_check")
        val ACTIVE_GAMING_PROFILE = stringPreferencesKey("active_gaming_profile")
    }

    val preferences: Flow<UserPreferences> = context.pulseDataStore.data.map { prefs ->
        val profileName = prefs[Keys.ACTIVE_GAMING_PROFILE] ?: GamingProfile.BALANCED.name
        val parsedProfile = runCatching { GamingProfile.valueOf(profileName) }
            .getOrDefault(GamingProfile.BALANCED)
        UserPreferences(
            monitorRefreshMs = prefs[Keys.MONITOR_REFRESH_MS] ?: 1000L,
            reducedAnimations = prefs[Keys.REDUCED_ANIMATIONS] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            largeFileThresholdMb = prefs[Keys.LARGE_FILE_THRESHOLD_MB] ?: 25,
            oldDownloadDays = prefs[Keys.OLD_DOWNLOAD_DAYS] ?: 30,
            scheduledHealthCheckEnabled = prefs[Keys.SCHEDULED_HEALTH_CHECK] ?: false,
            activeGamingProfile = parsedProfile
        )
    }

    suspend fun setMonitorRefreshMs(intervalMs: Long) {
        context.pulseDataStore.edit { it[Keys.MONITOR_REFRESH_MS] = intervalMs }
    }

    suspend fun setReducedAnimations(enabled: Boolean) {
        context.pulseDataStore.edit { it[Keys.REDUCED_ANIMATIONS] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.pulseDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setLargeFileThresholdMb(mb: Int) {
        context.pulseDataStore.edit { it[Keys.LARGE_FILE_THRESHOLD_MB] = mb }
    }

    suspend fun setOldDownloadDays(days: Int) {
        context.pulseDataStore.edit { it[Keys.OLD_DOWNLOAD_DAYS] = days }
    }

    suspend fun setScheduledHealthCheck(enabled: Boolean) {
        context.pulseDataStore.edit { it[Keys.SCHEDULED_HEALTH_CHECK] = enabled }
    }

    suspend fun setActiveGamingProfile(profile: GamingProfile) {
        context.pulseDataStore.edit { it[Keys.ACTIVE_GAMING_PROFILE] = profile.name }
    }
}
