package com.powercuts.powercutmonitor.util

/**
 * App-wide constants for Power Cuts Log
 */
object Constants {
    
    // Notification channels
    const val MONITORING_CHANNEL_ID = "monitoring"
    
    // Notification IDs
    const val MONITORING_NOTIFICATION_ID = 1001
    
    // Service actions
    const val ACTION_START_MONITORING = "com.powercuts.powercutmonitor.ACTION_START_MONITORING"
    const val ACTION_STOP_MONITORING = "com.powercuts.powercutmonitor.ACTION_STOP_MONITORING"
    const val ACTION_TOGGLE_MONITORING = "com.powercuts.powercutmonitor.ACTION_TOGGLE_MONITORING"
    
    // Power event types
    const val POWER_CONNECTED = "CONNECTED"
    const val POWER_DISCONNECTED = "DISCONNECTED"
    
    // Debounce settings (in milliseconds)
    const val DEBOUNCE_100MS = 100L
    const val DEBOUNCE_300MS = 300L
    const val DEBOUNCE_500MS = 500L
    const val DEBOUNCE_1S = 1000L
    const val DEBOUNCE_2S = 2000L
    
    // Default debounce
    const val DEFAULT_DEBOUNCE_MS = DEBOUNCE_500MS
    
    // Database
    const val DATABASE_NAME = "powercuts_database"
    const val DATABASE_VERSION = 1
    
    // Preferences keys
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_LAST_STATE = "last_state"
    const val PREF_FIRST_SEEN_MS = "first_seen_ms"
    const val PREF_DEBOUNCE_MS = "debounce_ms"
    const val PREF_LAST_EXPORT_EPOCH_MS = "last_export_epoch_ms"
    const val PREF_LAST_EXPORT_ZIP_HASH = "last_export_zip_hash"
    const val PREF_LAST_HEARTBEAT_MS = "last_heartbeat_ms"
    const val PREF_THEME_MODE = "theme_mode"
    
    // Default preference values
    const val DEFAULT_MONITORING_ENABLED = false
    const val DEFAULT_LAST_STATE = "OFF"
    const val DEFAULT_DEBOUNCE_MS_VALUE = DEFAULT_DEBOUNCE_MS
    
    // Theme preferences
    const val THEME_MODE_SYSTEM = "system"
    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"
    const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM
    
    // File paths
    const val EXPORTS_CACHE_DIR = "exports"
    const val CSV_FILE_PREFIX = "powercuts"
    const val ZIP_FILE_PREFIX = "powercuts-all"
    
    // Logging tags
    const val TAG_POWER_SERVICE = "PowerMonitorService"
    const val TAG_POWER_RECEIVER = "PowerReceiver"
    const val TAG_BOOT_RECEIVER = "BootReceiver"
    const val TAG_NOTIFICATION_MANAGER = "NotificationManager"
    
    // Timeouts and intervals
    const val SERVICE_START_TIMEOUT_MS = 5000L
    const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L
    
    // Wake lock timeout (short duration for event timestamping)
    const val WAKE_LOCK_TIMEOUT_MS = 1000L
}
