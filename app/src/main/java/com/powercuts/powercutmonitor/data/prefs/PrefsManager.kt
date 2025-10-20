package com.powercuts.powercutmonitor.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.powercuts.powercutmonitor.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Manager for app preferences using DataStore
 * Replaces SharedPreferences with modern, type-safe preference storage
 */
class PrefsManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "powercuts_prefs")
    }
    
    private val dataStore: DataStore<Preferences> = context.dataStore
    
    // Preference keys
    private val monitoringEnabledKey = booleanPreferencesKey(Constants.PREF_MONITORING_ENABLED)
    private val lastStateKey = stringPreferencesKey(Constants.PREF_LAST_STATE)
    private val firstSeenMsKey = longPreferencesKey(Constants.PREF_FIRST_SEEN_MS)
    private val debounceMsKey = longPreferencesKey(Constants.PREF_DEBOUNCE_MS)
    private val lastExportEpochMsKey = longPreferencesKey(Constants.PREF_LAST_EXPORT_EPOCH_MS)
    private val lastExportZipHashKey = stringPreferencesKey(Constants.PREF_LAST_EXPORT_ZIP_HASH)
    private val lastHeartbeatMsKey = longPreferencesKey(Constants.PREF_LAST_HEARTBEAT_MS)
    
    /**
     * Monitoring enabled state
     */
    val monitoringEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[monitoringEnabledKey] ?: Constants.DEFAULT_MONITORING_ENABLED
    }
    
    /**
     * Last monitoring state (ON/OFF)
     */
    val lastState: Flow<String> = dataStore.data.map { preferences ->
        preferences[lastStateKey] ?: Constants.DEFAULT_LAST_STATE
    }
    
    /**
     * First seen timestamp
     */
    val firstSeenMs: Flow<Long> = dataStore.data.map { preferences ->
        preferences[firstSeenMsKey] ?: System.currentTimeMillis()
    }
    
    /**
     * Debounce setting in milliseconds
     */
    val debounceMs: Flow<Long> = dataStore.data.map { preferences ->
        preferences[debounceMsKey] ?: Constants.DEFAULT_DEBOUNCE_MS_VALUE
    }
    
    /**
     * Last export timestamp
     */
    val lastExportEpochMs: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[lastExportEpochMsKey]
    }
    
    /**
     * Last export ZIP hash
     */
    val lastExportZipHash: Flow<String?> = dataStore.data.map { preferences ->
        preferences[lastExportZipHashKey]
    }
    
    /**
     * Last heartbeat timestamp
     */
    val lastHeartbeatMs: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[lastHeartbeatMsKey]
    }
    
    /**
     * Set monitoring enabled state
     */
    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[monitoringEnabledKey] = enabled
            preferences[lastStateKey] = if (enabled) "ON" else "OFF"
        }
    }
    
    /**
     * Set debounce setting
     */
    suspend fun setDebounceMs(debounceMs: Long) {
        dataStore.edit { preferences ->
            preferences[debounceMsKey] = debounceMs
        }
    }
    
    /**
     * Set first seen timestamp (only if not already set)
     */
    suspend fun setFirstSeenMsIfNotSet() {
        dataStore.edit { preferences ->
            if (preferences[firstSeenMsKey] == null) {
                preferences[firstSeenMsKey] = System.currentTimeMillis()
            }
        }
    }
    
    /**
     * Set last export information
     */
    suspend fun setLastExportInfo(epochMs: Long, zipHash: String) {
        dataStore.edit { preferences ->
            preferences[lastExportEpochMsKey] = epochMs
            preferences[lastExportZipHashKey] = zipHash
        }
    }
    
    /**
     * Clear last export information
     */
    suspend fun clearLastExportInfo() {
        dataStore.edit { preferences ->
            preferences.remove(lastExportEpochMsKey)
            preferences.remove(lastExportZipHashKey)
        }
    }
    
    /**
     * Set last heartbeat timestamp
     */
    suspend fun setLastHeartbeatMs(heartbeatMs: Long) {
        dataStore.edit { preferences ->
            preferences[lastHeartbeatMsKey] = heartbeatMs
        }
    }
    
    /**
     * Get current monitoring enabled state (synchronous)
     */
    suspend fun getMonitoringEnabled(): Boolean {
        return monitoringEnabled.first()
    }
    
    /**
     * Get current debounce setting (synchronous)
     */
    suspend fun getDebounceMs(): Long {
        return debounceMs.first()
    }
    
    /**
     * Get current last state (synchronous)
     */
    suspend fun getLastState(): String {
        return lastState.first()
    }
    
    /**
     * Clear all preferences (for testing or reset)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
