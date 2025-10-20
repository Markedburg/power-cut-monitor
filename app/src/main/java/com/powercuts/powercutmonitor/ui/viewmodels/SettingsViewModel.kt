package com.powercuts.powercutmonitor.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.powercuts.powercutmonitor.data.db.AppDatabase
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.data.repository.EventRepository
import com.powercuts.powercutmonitor.domain.ExportManager
import com.powercuts.powercutmonitor.util.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen
 * Manages settings state and preferences
 */
class SettingsViewModel(
    private val context: android.content.Context
) : ViewModel() {
    
    // Data layer components
    private val database = AppDatabase.getDatabase(context)
    private val eventRepository = EventRepository(database)
    private val prefsManager = PrefsManager(context)
    private val exportManager = ExportManager(context, eventRepository, prefsManager)
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Settings
    val debounceMs: StateFlow<Long> = prefsManager.debounceMs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.DEFAULT_DEBOUNCE_MS_VALUE
    )
    
    /**
     * Sets the debounce setting
     */
    fun setDebounceMs(debounceMs: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                prefsManager.setDebounceMs(debounceMs)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update debounce setting: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clears export cache
     */
    fun clearExportCache() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                exportManager.clearCache()
                _errorMessage.value = "Export cache cleared"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear cache: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refreshes data (placeholder for future functionality)
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Future: refresh data from repository
                _errorMessage.value = "Data refreshed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
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
}

/**
 * Factory for creating SettingsViewModel with Context dependency
 */
class SettingsViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
