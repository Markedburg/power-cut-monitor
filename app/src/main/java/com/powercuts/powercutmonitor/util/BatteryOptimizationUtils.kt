package com.powercuts.powercutmonitor.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Utility class for handling battery optimization settings
 * Helps ensure the app can run continuously in the background
 */
object BatteryOptimizationUtils {
    
    private const val TAG = "BatteryOptimizationUtils"
    
    /**
     * Checks if the app is whitelisted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Battery optimization was introduced in API 23 (M)
            true
        }
    }
    
    /**
     * Requests to ignore battery optimization for this app
     * This will open the system settings where user can whitelist the app
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Opened battery optimization settings")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", e)
                // Fallback to general battery settings
                openBatterySettings(context)
            }
        }
    }
    
    /**
     * Opens the general battery settings as a fallback
     */
    private fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened general battery optimization settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery settings", e)
        }
    }
    
    /**
     * Gets a user-friendly message about battery optimization status
     */
    fun getBatteryOptimizationStatusMessage(context: Context): String {
        return if (isIgnoringBatteryOptimizations(context)) {
            "✅ Battery optimization disabled - app can run continuously"
        } else {
            "⚠️ Battery optimization enabled - may stop monitoring after hours"
        }
    }
    
    /**
     * Checks if the app might be affected by unused app management
     * Note: This is an approximation since we can't directly check the setting
     */
    fun mightBeAffectedByUnusedAppManagement(context: Context): Boolean {
        // For Android 12+ (API 31+), unused app management is more aggressive
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * Gets a comprehensive status message about both settings
     */
    fun getComprehensiveStatusMessage(context: Context): String {
        val batteryOptimized = isIgnoringBatteryOptimizations(context)
        val mightBeUnusedManaged = mightBeAffectedByUnusedAppManagement(context)
        
        return when {
            batteryOptimized && !mightBeUnusedManaged -> 
                "✅ All settings optimized for continuous monitoring"
            batteryOptimized && mightBeUnusedManaged -> 
                "⚠️ Battery optimization disabled, but 'Manage app if unused' may still interfere"
            !batteryOptimized && mightBeUnusedManaged -> 
                "⚠️ Both battery optimization and 'Manage app if unused' may stop monitoring"
            else -> 
                "⚠️ Battery optimization may stop monitoring after hours"
        }
    }
    
    /**
     * Gets instructions for disabling battery optimization and unused app management
     */
    fun getBatteryOptimizationInstructions(): String {
        return """
            To ensure continuous monitoring:
            
            Battery Optimization:
            1. Find "Power Cuts Log" in the list
            2. Select "Don't optimize" or "Allow"
            
            Unused App Settings:
            3. Go to "Unused app settings"
            4. Find "Power Cuts Log" and turn OFF "Manage app if unused"
            
            Both settings prevent Android from stopping the app after hours of inactivity.
        """.trimIndent()
    }
    
    /**
     * Opens the unused app settings (Android 12+)
     */
    fun openUnusedAppSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Opened app details settings for unused app management")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open unused app settings", e)
                // Fallback to general app settings
                openBatterySettings(context)
            }
        } else {
            // For older Android versions, just open battery settings
            openBatterySettings(context)
        }
    }
}
