package com.powercuts.powercutmonitor.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.powercuts.powercutmonitor.data.db.AppDatabase
import com.powercuts.powercutmonitor.data.db.entities.DailyTotal
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import com.powercuts.powercutmonitor.data.db.entities.TodayTotals
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.data.repository.EventRepository
import com.powercuts.powercutmonitor.domain.ExportManager
import com.powercuts.powercutmonitor.service.PowerMonitorService
import com.powercuts.powercutmonitor.util.Constants
import com.powercuts.powercutmonitor.util.DateTimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import java.time.LocalDate

/**
 * Data class for grouping events by date
 */
data class GroupedEvents(
    val date: LocalDate,
    val events: List<PowerEvent>
)

/**
 * ViewModel for the main screen
 * Manages UI state and coordinates between UI and data layer
 */
class MainViewModel(
    private val context: android.content.Context
) : ViewModel() {
    
    // Data layer components
    private val database = AppDatabase.getDatabase(context)
    private val eventRepository = EventRepository(database)
    private val prefsManager = PrefsManager(context)
    private val exportManager = ExportManager(context, eventRepository, prefsManager)
    
    // UI State - using reactive data sources
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    // Reactive data from database
    val todayTotals: StateFlow<TodayTotals> = eventRepository.getTodayTotalsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayTotals(0, 0, "0s")
    )
    
    val recentEvents: StateFlow<List<PowerEvent>> = eventRepository.getAllEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Grouped events by date
    val groupedEvents: StateFlow<List<GroupedEvents>> = recentEvents.map { events ->
        events.groupBy { event ->
            DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
        }.map { (date, eventsForDate) ->
            GroupedEvents(
                date = date,
                events = eventsForDate.sortedByDescending { it.startEpochMs }
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val dailyTotals: StateFlow<List<DailyTotal>> = eventRepository.getDailyTotals().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _exportFile = MutableStateFlow<java.io.File?>(null)
    val exportFile: StateFlow<java.io.File?> = _exportFile.asStateFlow()
    
    init {
        // Initialize monitoring state safely
        viewModelScope.launch {
            try {
                val enabled = prefsManager.getMonitoringEnabled()
                _isMonitoring.value = enabled
            } catch (e: Exception) {
                // If there's an error, default to false
                _isMonitoring.value = false
            }
        }
        // No need to manually load data - reactive flows will handle it automatically
    }
    
    /**
     * Toggles monitoring on/off
     */
    fun toggleMonitoring() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentState = _isMonitoring.value
                val newState = !currentState
                
                if (newState) {
                    startMonitoring()
                } else {
                    stopMonitoring()
                }
                
                // Update preferences
                prefsManager.setMonitoringEnabled(newState)
                _isMonitoring.value = newState
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle monitoring: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Starts monitoring by starting the service
     */
    private fun startMonitoring() {
        val intent = android.content.Intent(context, PowerMonitorService::class.java).apply {
            action = Constants.ACTION_START_MONITORING
        }
        context.startForegroundService(intent)
    }
    
    /**
     * Stops monitoring by stopping the service
     */
    private fun stopMonitoring() {
        val intent = android.content.Intent(context, PowerMonitorService::class.java).apply {
            action = Constants.ACTION_STOP_MONITORING
        }
        context.startForegroundService(intent)
    }
    
    /**
     * Loads today's totals (count and duration)
     */
    fun loadTodayTotals() {
        viewModelScope.launch {
            try {
                // Data is now handled by reactive flows
                // val totals = eventRepository.getTodayTotals()
                // _todayTotals.value = totals
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load today's totals: ${e.message}"
            }
        }
    }
    
    /**
     * Loads recent events for display
     */
    fun loadRecentEvents() {
        viewModelScope.launch {
            try {
                // Data is now handled by reactive flows
                // val events = eventRepository.getTodayEvents()
                // _recentEvents.value = events
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load recent events: ${e.message}"
            }
        }
    }
    
    /**
     * Loads daily totals for all days
     */
    fun loadDailyTotals() {
        viewModelScope.launch {
            try {
                // Data is now handled by reactive flows
                // eventRepository.getDailyTotals().collect { totals ->
                //     _dailyTotals.value = totals
                // }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load daily totals: ${e.message}"
            }
        }
    }
    
    /**
     * Deletes all events for today
     */
    fun deleteTodayEvents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val deletedCount = eventRepository.deleteTodayEvents()
                
                // Refresh data after deletion
                loadTodayTotals()
                loadRecentEvents()
                loadDailyTotals()
                
                _errorMessage.value = "Deleted $deletedCount events for today"
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete today's events: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Deletes all events (with confirmation)
     */
    fun deleteAllEvents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val deletedCount = eventRepository.deleteAllEvents()
                
                // Refresh data after deletion
                loadTodayTotals()
                loadRecentEvents()
                loadDailyTotals()
                
                _errorMessage.value = "Deleted $deletedCount events total"
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete all events: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Deletes a specific event
     */
    fun deleteEvent(event: PowerEvent) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                eventRepository.deleteEvent(event)
                
                // Refresh data after deletion
                loadTodayTotals()
                loadRecentEvents()
                loadDailyTotals()
                
                _errorMessage.value = "Event deleted successfully"
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete event: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refreshes all data
     */
    fun refreshData() {
        loadTodayTotals()
        loadRecentEvents()
        loadDailyTotals()
    }
    
    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Gets formatted duration for display
     */
    fun getFormattedDuration(durationSec: Long): String {
        return DateTimeUtils.formatDuration(durationSec)
    }
    
    /**
     * Gets formatted time for display
     */
    fun getFormattedTime(epochMs: Long): String {
        return DateTimeUtils.formatEventTime(epochMs)
    }
    
    /**
     * Gets formatted date for display
     */
    fun getFormattedDate(epochMs: Long): String {
        return DateTimeUtils.formatEventDate(epochMs)
    }
    
    /**
     * Gets display name for a date
     */
    fun getDateDisplayName(date: java.time.LocalDate): String {
        return DateTimeUtils.getDateDisplayName(date)
    }
    
    /**
     * Exports data and returns the ZIP file for sharing
     */
    suspend fun exportData(allDays: Boolean = true): java.io.File {
        return if (allDays) {
            exportManager.buildZipForAllDays()
        } else {
            exportManager.buildZipForToday()
        }
    }
    
    /**
     * Triggers export and sharing (non-suspend version for UI)
     */
    fun exportAndShare(allDays: Boolean = true) {
        viewModelScope.launch {
            try {
                val exportFile = exportData(allDays)
                _exportFile.value = exportFile
                _errorMessage.value = "Data exported to: ${exportFile.name}"
            } catch (e: Exception) {
                _errorMessage.value = "Export failed: ${e.message}"
                _exportFile.value = null
            }
        }
    }
    
    /**
     * Clears the export file after sharing
     */
    fun clearExportFile() {
        _exportFile.value = null
    }
    
    /**
     * Checks if export cache is valid
     */
    suspend fun isExportCacheValid(): Boolean {
        return exportManager.isCacheValid()
    }
    
    /**
     * Clears export cache
     */
    suspend fun clearExportCache() {
        exportManager.clearCache()
    }
    
    /**
     * Gets export cache size
     */
    suspend fun getExportCacheSize(): Long {
        return exportManager.getCacheSize()
    }
    
    /**
     * Gets export cache file count
     */
    suspend fun getExportCacheFileCount(): Int {
        return exportManager.getCacheFileCount()
    }
    
    /**
     * Test method to simulate a power event (for debugging)
     */
    fun testPowerEvent() {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                // Simulate a power disconnect event
                eventRepository.handlePowerDisconnected(currentTime)
                // Simulate a power connect event after 5 seconds
                kotlinx.coroutines.delay(5000)
                eventRepository.handlePowerConnected(currentTime + 5000)
                
                // Refresh data to show the test event
                // Data is automatically refreshed by reactive flows
                // loadTodayTotals()
                // loadRecentEvents()
            } catch (e: Exception) {
                _errorMessage.value = "Test failed: ${e.message}"
            }
        }
    }
}

/**
 * Factory for creating MainViewModel with Context dependency
 */
class MainViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
