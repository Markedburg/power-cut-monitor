package com.powercuts.powercutmonitor.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.powercuts.powercutmonitor.MainActivity
import com.powercuts.powercutmonitor.R
import com.powercuts.powercutmonitor.data.db.AppDatabase
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.data.repository.EventRepository
import com.powercuts.powercutmonitor.util.Constants
import com.powercuts.powercutmonitor.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service for monitoring power connect/disconnect events
 * Runs continuously while monitoring is enabled
 */
class PowerMonitorService : Service() {
    
    companion object {
        const val ACTION_POWER_EVENT = "com.powercuts.powercutmonitor.ACTION_POWER_EVENT"
        const val ACTION_START_MONITORING = "com.powercuts.powercutmonitor.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.powercuts.powercutmonitor.ACTION_STOP_MONITORING"
        const val ACTION_TOGGLE_MONITORING = "com.powercuts.powercutmonitor.ACTION_TOGGLE_MONITORING"
        
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_EVENT_TIME_MS = "event_time_ms"
        const val EXTRA_BOOT_RESUME = "boot_resume"
        const val EXTRA_BOOT_TIME_MS = "boot_time_ms"
    }
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var prefsManager: PrefsManager
    private lateinit var eventRepository: EventRepository
    private var wakeLock: PowerManager.WakeLock? = null
    private var powerReceiver: PowerReceiver? = null
    
    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Service state
    private var isMonitoring = false
    private var lastEventTimeMs: Long = 0L
    private var lastEventType: String? = null
    private var lastEventDuration: String? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(Constants.TAG_POWER_SERVICE, "PowerMonitorService created")
        
        notificationManager = NotificationManager(this)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        prefsManager = PrefsManager(this)
        
        // Initialize database and repository
        val database = AppDatabase.getDatabase(this)
        eventRepository = EventRepository(database)
        
        // Wake lock will be created when monitoring starts
        
        // Set first seen timestamp if not already set
        serviceScope.launch {
            prefsManager.setFirstSeenMsIfNotSet()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Constants.TAG_POWER_SERVICE, "onStartCommand called with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring(intent.getBooleanExtra(EXTRA_BOOT_RESUME, false))
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
            ACTION_TOGGLE_MONITORING -> {
                toggleMonitoring()
            }
            ACTION_POWER_EVENT -> {
                handlePowerEvent(
                    intent.getStringExtra(EXTRA_EVENT_TYPE) ?: "",
                    intent.getLongExtra(EXTRA_EVENT_TIME_MS, System.currentTimeMillis())
                )
            }
            null -> {
                // Service was killed and restarted by system
                Log.d(Constants.TAG_POWER_SERVICE, "Service restarted by system, resuming monitoring")
                resumeMonitoring()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        // Service is not bound, only started
        return null
    }
    
    override fun onDestroy() {
        Log.d(Constants.TAG_POWER_SERVICE, "PowerMonitorService destroyed")
        releaseWakeLock()
        super.onDestroy()
    }
    
    /**
     * Starts monitoring and posts foreground notification
     */
    private fun startMonitoring(isBootResume: Boolean = false) {
        if (isMonitoring) {
            Log.d(Constants.TAG_POWER_SERVICE, "Monitoring already active")
            return
        }
        
        Log.d(Constants.TAG_POWER_SERVICE, "Starting power monitoring${if (isBootResume) " (boot resume)" else ""}")
        
        isMonitoring = true
        acquireWakeLock()
        
        // Save monitoring state to preferences
        serviceScope.launch {
            prefsManager.setMonitoringEnabled(true)
        }
        
        // Start foreground service with notification
        val notification = createMonitoringNotification()
        startForeground(Constants.MONITORING_NOTIFICATION_ID, notification)
        
        // Register power receiver programmatically
        registerPowerReceiver()
        
        // Start heartbeat to keep service alive and detect if it's been killed
        startHeartbeat()
        
        Log.d(Constants.TAG_POWER_SERVICE, "Power monitoring started successfully - notification posted")
    }
    
    /**
     * Stops monitoring and removes foreground notification
     */
    private fun stopMonitoring() {
        if (!isMonitoring) {
            Log.d(Constants.TAG_POWER_SERVICE, "Monitoring already stopped")
            return
        }
        
        Log.d(Constants.TAG_POWER_SERVICE, "Stopping power monitoring")
        
        isMonitoring = false
        releaseWakeLock()
        
        // Save monitoring state to preferences
        serviceScope.launch {
            prefsManager.setMonitoringEnabled(false)
        }
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Unregister power receiver
        unregisterPowerReceiver()
        
        // Stop heartbeat
        stopHeartbeat()
        
        stopSelf()
        
        Log.d(Constants.TAG_POWER_SERVICE, "Power monitoring stopped successfully")
    }
    
    /**
     * Toggles monitoring state
     */
    private fun toggleMonitoring() {
        if (isMonitoring) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }
    
    /**
     * Resumes monitoring after service restart
     */
    private fun resumeMonitoring() {
        serviceScope.launch {
            val wasMonitoring = prefsManager.getMonitoringEnabled()
            if (wasMonitoring) {
                Log.d(Constants.TAG_POWER_SERVICE, "Resuming monitoring after service restart")
                startMonitoring()
            } else {
                Log.d(Constants.TAG_POWER_SERVICE, "Monitoring was disabled, stopping service")
                stopSelf()
            }
        }
    }
    
    /**
     * Handles power connect/disconnect events
     */
    private fun handlePowerEvent(eventType: String, eventTimeMs: Long) {
        Log.d(Constants.TAG_POWER_SERVICE, "handlePowerEvent called: $eventType at $eventTimeMs, isMonitoring: $isMonitoring")
        
        if (!isMonitoring) {
            Log.d(Constants.TAG_POWER_SERVICE, "Received power event but monitoring is not active")
            return
        }
        
        Log.d(Constants.TAG_POWER_SERVICE, "Processing power event: $eventType")
        
        serviceScope.launch {
            try {
                // Apply debounce logic
                if (shouldIgnoreEvent(eventTimeMs)) {
                    Log.d(Constants.TAG_POWER_SERVICE, "Ignoring event due to debounce (${eventTimeMs - lastEventTimeMs}ms since last event)")
                    return@launch
                }
                
                // Update last event info
                lastEventTimeMs = eventTimeMs
                lastEventType = eventType
                
                // Save event to database
                val eventId = when (eventType) {
                    Constants.POWER_DISCONNECTED -> {
                        eventRepository.handlePowerDisconnected(eventTimeMs)
                    }
                    Constants.POWER_CONNECTED -> {
                        eventRepository.handlePowerConnected(eventTimeMs)
                    }
                    else -> {
                        Log.w(Constants.TAG_POWER_SERVICE, "Unknown event type: $eventType")
                        return@launch
                    }
                }
                
                Log.d(Constants.TAG_POWER_SERVICE, "Event saved to database with ID: $eventId")
                
                // Calculate duration if this is a power restore event
                if (eventType == Constants.POWER_CONNECTED && lastEventType == Constants.POWER_DISCONNECTED) {
                    val durationMs = eventTimeMs - lastEventTimeMs
                    lastEventDuration = DateTimeUtils.formatDurationMs(durationMs)
                    Log.d(Constants.TAG_POWER_SERVICE, "Power restored after ${lastEventDuration}")
                }
                
                // Update notification with latest event info
                updateMonitoringNotification()
                
                // TODO: In Phase 5, show alert notification if enabled
                
                Log.d(Constants.TAG_POWER_SERVICE, "Power event processed successfully")
                
            } catch (e: Exception) {
                Log.e(Constants.TAG_POWER_SERVICE, "Error handling power event", e)
            }
        }
    }
    
    /**
     * Checks if event should be ignored due to debounce
     */
    private suspend fun shouldIgnoreEvent(eventTimeMs: Long): Boolean {
        if (lastEventTimeMs == 0L) return false
        
        val debounceMs = prefsManager.getDebounceMs()
        val timeSinceLastEvent = eventTimeMs - lastEventTimeMs
        
        return timeSinceLastEvent < debounceMs
    }
    
    /**
     * Creates the monitoring notification
     */
    private fun createMonitoringNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, PowerMonitorService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val shareIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.powercuts.powercutmonitor.ACTION_SHARE"
        }
        val sharePendingIntent = PendingIntent.getActivity(
            this, 2, shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = notificationManager.createMonitoringNotification(
            isMonitoring = isMonitoring,
            lastEventTime = DateTimeUtils.formatEventTime(lastEventTimeMs),
            lastEventDuration = lastEventDuration
        )
        
        return notificationManager.addActionButtons(
            builder,
            stopPendingIntent,
            sharePendingIntent
        )
            .setContentIntent(mainPendingIntent)
            .build()
    }
    
    /**
     * Updates the monitoring notification
     */
    private fun updateMonitoringNotification() {
        if (isMonitoring) {
            val notification = createMonitoringNotification()
            val notificationManagerCompat = androidx.core.app.NotificationManagerCompat.from(this)
            notificationManagerCompat.notify(Constants.MONITORING_NOTIFICATION_ID, notification)
        }
    }
    
    /**
     * Acquires wake lock for service operation
     */
    private fun acquireWakeLock() {
        try {
            // Use SCREEN_DIM_WAKE_LOCK to keep CPU running and screen dimmed
            // This prevents the device from going into deep sleep
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "PowerCutsLog:ContinuousMonitoring"
            )
            wakeLock?.acquire() // Acquire indefinitely while monitoring
            Log.d(Constants.TAG_POWER_SERVICE, "Wake lock acquired (SCREEN_DIM_WAKE_LOCK)")
        } catch (e: Exception) {
            Log.e(Constants.TAG_POWER_SERVICE, "Failed to acquire wake lock", e)
            // Fallback to partial wake lock
            try {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PowerCutsLog:Service"
                )
                wakeLock?.acquire()
                Log.d(Constants.TAG_POWER_SERVICE, "Fallback wake lock acquired (PARTIAL_WAKE_LOCK)")
            } catch (e2: Exception) {
                Log.e(Constants.TAG_POWER_SERVICE, "Failed to acquire fallback wake lock", e2)
            }
        }
    }
    
    /**
     * Releases wake lock
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(Constants.TAG_POWER_SERVICE, "Wake lock released")
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_POWER_SERVICE, "Failed to release wake lock", e)
        }
    }
    
    /**
     * Registers the power receiver programmatically
     */
    private fun registerPowerReceiver() {
        try {
            if (powerReceiver == null) {
                powerReceiver = PowerReceiver()
                val filter = android.content.IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                registerReceiver(powerReceiver, filter)
                Log.d(Constants.TAG_POWER_SERVICE, "Power receiver registered programmatically")
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_POWER_SERVICE, "Failed to register power receiver", e)
        }
    }
    
    /**
     * Unregisters the power receiver
     */
    private fun unregisterPowerReceiver() {
        try {
            powerReceiver?.let {
                unregisterReceiver(it)
                powerReceiver = null
                Log.d(Constants.TAG_POWER_SERVICE, "Power receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG_POWER_SERVICE, "Failed to unregister power receiver", e)
        }
    }
    
    /**
     * Starts heartbeat to keep service alive and detect if it's been killed
     */
    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch {
            while (isMonitoring) {
                try {
                    delay(30000) // Check every 30 seconds
                    
                    // Update last heartbeat time in preferences
                    prefsManager.setLastHeartbeatMs(System.currentTimeMillis())
                    
                    // Update notification to show service is alive
                    updateMonitoringNotification()
                    
                    Log.d(Constants.TAG_POWER_SERVICE, "Heartbeat - service is alive")
                    
                } catch (e: Exception) {
                    Log.e(Constants.TAG_POWER_SERVICE, "Heartbeat error", e)
                }
            }
        }
    }
    
    /**
     * Stops the heartbeat
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(Constants.TAG_POWER_SERVICE, "Heartbeat stopped")
    }
    
}
