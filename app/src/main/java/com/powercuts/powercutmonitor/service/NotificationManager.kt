package com.powercuts.powercutmonitor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.powercuts.powercutmonitor.R
import com.powercuts.powercutmonitor.util.Constants

/**
 * Manages notification channels and creates notifications for the power monitoring service
 */
class NotificationManager(private val context: Context) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Creates notification channels for monitoring and alerts
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Monitoring channel - persistent, high importance to prevent battery optimization
            val monitoringChannel = NotificationChannel(
                Constants.MONITORING_CHANNEL_ID,
                "Power Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent notification while power monitoring is active - prevents battery optimization"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setBypassDnd(true) // Bypass Do Not Disturb
            }
            
            // Alerts channel - default importance, optional sound
            val alertsChannel = NotificationChannel(
                Constants.ALERTS_CHANNEL_ID,
                "Power Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for power outage start and end events"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                // Sound will be set per notification to respect DND
            }
            
            systemNotificationManager.createNotificationChannel(monitoringChannel)
            systemNotificationManager.createNotificationChannel(alertsChannel)
        }
    }
    
    /**
     * Creates the persistent monitoring notification
     */
    fun createMonitoringNotification(
        isMonitoring: Boolean,
        lastEventTime: String? = null,
        lastEventDuration: String? = null
    ): NotificationCompat.Builder {
        val title = if (isMonitoring) "Power monitor: ON" else "Power monitor: OFF"
        val contentText = when {
            isMonitoring && lastEventTime != null && lastEventDuration != null -> 
                "last event $lastEventTime ($lastEventDuration)"
            isMonitoring -> "Monitoring active"
            else -> "Monitoring stopped"
        }
        
        return NotificationCompat.Builder(context, Constants.MONITORING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Will be updated with proper icon
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(isMonitoring) // Can't be dismissed while monitoring
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to prevent battery optimization
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Immediate foreground service
    }
    
    /**
     * Creates an alert notification for power events
     */
    fun createAlertNotification(
        isPowerConnected: Boolean,
        eventTime: String
    ): NotificationCompat.Builder {
        val title = if (isPowerConnected) "Power Restored" else "Power Outage Started"
        val icon = if (isPowerConnected) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground
        
        return NotificationCompat.Builder(context, Constants.ALERTS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("Power event at $eventTime")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Respects DND
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
    }
    
    /**
     * Adds action buttons to a notification
     */
    fun addActionButtons(
        builder: NotificationCompat.Builder,
        stopPendingIntent: android.app.PendingIntent?,
        sharePendingIntent: android.app.PendingIntent?
    ): NotificationCompat.Builder {
        stopPendingIntent?.let {
            builder.addAction(
                R.drawable.ic_launcher_foreground, // Will be updated with proper icon
                "Stop",
                it
            )
        }
        
        sharePendingIntent?.let {
            builder.addAction(
                R.drawable.ic_launcher_foreground, // Will be updated with proper icon
                "Share",
                it
            )
        }
        
        return builder
    }
    
    /**
     * Checks if notifications are enabled for the app
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Checks if a specific notification channel is enabled
     */
    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }
}
