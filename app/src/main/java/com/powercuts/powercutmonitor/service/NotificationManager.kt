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
            
            systemNotificationManager.createNotificationChannel(monitoringChannel)
        }
    }
    
    /**
     * Creates the persistent monitoring notification
     */
    fun createMonitoringNotification(
        isMonitoring: Boolean
    ): NotificationCompat.Builder {
        val title = if (isMonitoring) "Power monitor: ON" else "Power monitor: OFF"
        val contentText = if (isMonitoring) "Monitoring active" else "Monitoring stopped"
        
        return NotificationCompat.Builder(context, Constants.MONITORING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Use app's foreground icon for notifications
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(isMonitoring) // Can't be dismissed while monitoring
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to prevent battery optimization
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true) // Prevent repeated alerts when notification is updated
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Immediate foreground service
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
                R.mipmap.ic_launcher_foreground, // Use app's foreground icon
                "Stop",
                it
            )
        }
        
        sharePendingIntent?.let {
            builder.addAction(
                R.mipmap.ic_launcher_foreground, // Use app's foreground icon
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
