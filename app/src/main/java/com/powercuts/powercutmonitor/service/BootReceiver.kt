package com.powercuts.powercutmonitor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for boot completion events
 * Auto-resumes power monitoring if it was enabled before reboot
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(Constants.TAG_BOOT_RECEIVER, "Boot completed broadcast received")
                handleBootCompleted(context)
            }
        }
    }
    
    /**
     * Handles boot completion by checking if monitoring should be resumed
     */
    private fun handleBootCompleted(context: Context) {
        val bootScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        bootScope.launch {
            try {
                val bootTimeMs = System.currentTimeMillis()
                Log.d(Constants.TAG_BOOT_RECEIVER, "Boot completed at $bootTimeMs")
                
                // Check if monitoring was enabled before reboot using DataStore
                val prefsManager = PrefsManager(context)
                val wasMonitoringEnabled = prefsManager.getMonitoringEnabled()
                
                if (wasMonitoringEnabled) {
                    Log.d(Constants.TAG_BOOT_RECEIVER, "Auto-resuming power monitoring after boot")
                    
                    // Start the PowerMonitorService
                    val serviceIntent = Intent(context, PowerMonitorService::class.java).apply {
                        action = Constants.ACTION_START_MONITORING
                        putExtra(PowerMonitorService.EXTRA_BOOT_RESUME, true)
                        putExtra(PowerMonitorService.EXTRA_BOOT_TIME_MS, bootTimeMs)
                    }
                    
                    context.startForegroundService(serviceIntent)
                    
                    Log.d(Constants.TAG_BOOT_RECEIVER, "PowerMonitorService started for boot resume")
                } else {
                    Log.d(Constants.TAG_BOOT_RECEIVER, "Monitoring was not enabled before boot, skipping auto-resume")
                }
                
            } catch (e: Exception) {
                Log.e(Constants.TAG_BOOT_RECEIVER, "Error handling boot completion", e)
            }
        }
    }
}
