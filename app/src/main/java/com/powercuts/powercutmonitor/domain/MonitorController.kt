package com.powercuts.powercutmonitor.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.powercuts.powercutmonitor.service.PowerMonitorService
import com.powercuts.powercutmonitor.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller for managing communication between UI and PowerMonitorService
 * Handles service binding and provides a clean interface for UI components
 */
class MonitorController(private val context: Context) {
    
    private var service: PowerMonitorService? = null
    private var isBound = false
    
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    /**
     * Service connection for binding to PowerMonitorService
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(Constants.TAG_POWER_SERVICE, "Service connected")
            isBound = true
            _isServiceConnected.value = true
            
            // TODO: In future phases, we might get a binder to communicate with service
            // For now, we just track connection state
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(Constants.TAG_POWER_SERVICE, "Service disconnected")
            isBound = false
            _isServiceConnected.value = false
            service = null
        }
    }
    
    /**
     * Binds to the PowerMonitorService
     */
    fun bindService() {
        if (!isBound) {
            val intent = Intent(context, PowerMonitorService::class.java)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (!bound) {
                Log.w(Constants.TAG_POWER_SERVICE, "Failed to bind to service")
            }
        }
    }
    
    /**
     * Unbinds from the PowerMonitorService
     */
    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            _isServiceConnected.value = false
            service = null
        }
    }
    
    /**
     * Starts monitoring by starting the service
     */
    fun startMonitoring() {
        val intent = Intent(context, PowerMonitorService::class.java).apply {
            action = Constants.ACTION_START_MONITORING
        }
        context.startForegroundService(intent)
        _isMonitoring.value = true
    }
    
    /**
     * Stops monitoring by stopping the service
     */
    fun stopMonitoring() {
        val intent = Intent(context, PowerMonitorService::class.java).apply {
            action = Constants.ACTION_STOP_MONITORING
        }
        context.startForegroundService(intent)
        _isMonitoring.value = false
    }
    
    /**
     * Toggles monitoring state
     */
    fun toggleMonitoring() {
        if (_isMonitoring.value) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }
    
    /**
     * Updates monitoring state (called from external sources)
     */
    fun updateMonitoringState(isMonitoring: Boolean) {
        _isMonitoring.value = isMonitoring
    }
    
    /**
     * Checks if the service is currently running
     */
    fun isServiceRunning(): Boolean {
        return isBound && _isServiceConnected.value
    }
    
    /**
     * Gets the current monitoring state
     */
    fun getCurrentMonitoringState(): Boolean {
        return _isMonitoring.value
    }
    
    /**
     * Cleans up resources
     */
    fun cleanup() {
        unbindService()
    }
}
