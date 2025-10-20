package com.powercuts.powercutmonitor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.powercuts.powercutmonitor.util.Constants

/**
 * Broadcast receiver for power connect/disconnect events
 * Captures system broadcasts and forwards them to PowerMonitorService
 */
class PowerReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(Constants.TAG_POWER_RECEIVER, "PowerReceiver.onReceive called with action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(Constants.TAG_POWER_RECEIVER, "Power connected broadcast received")
                handlePowerEvent(context, Constants.POWER_CONNECTED)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(Constants.TAG_POWER_RECEIVER, "Power disconnected broadcast received")
                handlePowerEvent(context, Constants.POWER_DISCONNECTED)
            }
            else -> {
                Log.d(Constants.TAG_POWER_RECEIVER, "Unknown action received: ${intent.action}")
            }
        }
    }
    
    /**
     * Handles power events by acquiring a short wake lock and forwarding to service
     */
    private fun handlePowerEvent(context: Context, eventType: String) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PowerCutsLog:PowerEvent"
        )
        
        try {
            // Acquire wake lock for short duration to ensure event is processed
            wakeLock.acquire(Constants.WAKE_LOCK_TIMEOUT_MS)
            
            val currentTimeMs = System.currentTimeMillis()
            Log.d(Constants.TAG_POWER_RECEIVER, "Power event: $eventType at $currentTimeMs")
            
            // Forward event to PowerMonitorService
            val serviceIntent = Intent(context, PowerMonitorService::class.java).apply {
                action = PowerMonitorService.ACTION_POWER_EVENT
                putExtra(PowerMonitorService.EXTRA_EVENT_TYPE, eventType)
                putExtra(PowerMonitorService.EXTRA_EVENT_TIME_MS, currentTimeMs)
            }
            
            // Start service to handle the event
            context.startForegroundService(serviceIntent)
            
        } catch (e: Exception) {
            Log.e(Constants.TAG_POWER_RECEIVER, "Error handling power event: $eventType", e)
        } finally {
            // Release wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
