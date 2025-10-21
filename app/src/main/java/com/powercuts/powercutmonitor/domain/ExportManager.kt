package com.powercuts.powercutmonitor.domain

import android.content.Context
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.data.repository.EventRepository
import com.powercuts.powercutmonitor.util.Constants
import com.powercuts.powercutmonitor.util.DateTimeUtils
import com.powercuts.powercutmonitor.util.FormatUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Manager for exporting power event data to CSV and ZIP formats
 * Handles CSV generation, ZIP bundling, and cache management
 */
class ExportManager(
    private val context: Context,
    private val eventRepository: EventRepository,
    private val prefsManager: PrefsManager
) {
    
    private val cacheDir = File(context.cacheDir, Constants.EXPORTS_CACHE_DIR)
    
    init {
        // Ensure cache directory exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * Generates CSV content for a specific date
     */
    suspend fun generateCSVForDay(date: LocalDate): String = withContext(Dispatchers.IO) {
        val events = eventRepository.getEventsForDate(date)
        
        val csv = StringBuilder()
        
        // CSV header
        csv.appendLine("date,start_time,end_time,duration_seconds,duration_hms")
        
        // CSV rows
        events.forEach { event ->
            val dateStr = FormatUtils.formatDateForExport(date)
            val startTime = FormatUtils.formatTimeForExport(event.startEpochMs)
            val endTime = if (event.endEpochMs != null) {
                FormatUtils.formatTimeForExport(event.endEpochMs)
            } else {
                "?"
            }
            val durationSec = event.durationSec ?: 0
            val durationHms = FormatUtils.formatDurationHMSForExport(durationSec)
            
            csv.appendLine("$dateStr,$startTime,$endTime,$durationSec,$durationHms")
        }
        
        csv.toString()
    }
    
    /**
     * Generates CSV content for all events
     */
    suspend fun generateCSVForAllEvents(): String = withContext(Dispatchers.IO) {
        val allEvents = eventRepository.getAllEvents().first()
        
        val csv = StringBuilder()
        
        // CSV header
        csv.appendLine("date,start_time,end_time,duration_seconds,duration_hms")
        
        // Group events by date
        val eventsByDate = allEvents.groupBy { event ->
            DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
        }
        
        // Sort dates in descending order (newest first)
        val sortedDates = eventsByDate.keys.sortedDescending()
        
        // CSV rows
        sortedDates.forEach { date ->
            val events = eventsByDate[date] ?: emptyList()
            events.forEach { event ->
                val dateStr = FormatUtils.formatDateForExport(date)
                val startTime = FormatUtils.formatTimeForExport(event.startEpochMs)
                val endTime = if (event.endEpochMs != null) {
                    FormatUtils.formatTimeForExport(event.endEpochMs)
                } else {
                    "?"
                }
                val durationSec = event.durationSec ?: 0
                val durationHms = FormatUtils.formatDurationHMSForExport(durationSec)
                
                csv.appendLine("$dateStr,$startTime,$endTime,$durationSec,$durationHms")
            }
        }
        
        csv.toString()
    }
    
    /**
     * Builds a ZIP file containing CSV files for all days
     */
    suspend fun buildZipForAllDays(): File = withContext(Dispatchers.IO) {
        val timestamp = DateTimeUtils.getCurrentEpochMs()
        val zipFileName = "${Constants.ZIP_FILE_PREFIX}-${DateTimeUtils.formatTimestampForDebug(timestamp).replace(":", "").replace(" ", "-")}.zip"
        val zipFile = File(cacheDir, zipFileName)
        
        // Check if cache is valid
        if (isCacheValid() && zipFile.exists()) {
            return@withContext zipFile
        }
        
        // Generate ZIP file
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // Get all events grouped by date
            val allEvents = eventRepository.getAllEvents().first()
            val eventsByDate = allEvents.groupBy { event ->
                DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
            }
            
            // Sort dates in descending order (newest first)
            val sortedDates = eventsByDate.keys.sortedDescending()
            
            // Add CSV file for each date
            sortedDates.forEach { date ->
                val csvContent = generateCSVForDay(date)
                val csvFileName = "${Constants.CSV_FILE_PREFIX}-${FormatUtils.formatDateForExport(date)}.csv"
                
                val entry = ZipEntry(csvFileName)
                zipOut.putNextEntry(entry)
                zipOut.write(csvContent.toByteArray())
                zipOut.closeEntry()
            }
            
            // Add summary CSV with all events
            val summaryCsv = generateCSVForAllEvents()
            val summaryEntry = ZipEntry("${Constants.CSV_FILE_PREFIX}-all-events.csv")
            zipOut.putNextEntry(summaryEntry)
            zipOut.write(summaryCsv.toByteArray())
            zipOut.closeEntry()
        }
        
        // Update cache info
        val zipHash = calculateFileHash(zipFile)
        prefsManager.setLastExportInfo(timestamp, zipHash)
        
        zipFile
    }
    
    /**
     * Builds a ZIP file containing CSV file for today only
     */
    suspend fun buildZipForToday(): File = withContext(Dispatchers.IO) {
        val today = LocalDate.now(DateTimeUtils.getCurrentTimezone())
        val timestamp = DateTimeUtils.getCurrentEpochMs()
        val zipFileName = "${Constants.ZIP_FILE_PREFIX}-today-${DateTimeUtils.formatTimestampForDebug(timestamp).replace(":", "").replace(" ", "-")}.zip"
        val zipFile = File(cacheDir, zipFileName)
        
        // Generate ZIP file
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            val csvContent = generateCSVForDay(today)
            val csvFileName = "${Constants.CSV_FILE_PREFIX}-${FormatUtils.formatDateForExport(today)}.csv"
            
            val entry = ZipEntry(csvFileName)
            zipOut.putNextEntry(entry)
            zipOut.write(csvContent.toByteArray())
            zipOut.closeEntry()
        }
        
        zipFile
    }
    
    /**
     * Builds a ZIP file containing CSV files for the last N days
     */
    suspend fun buildZipForLastNDays(days: Int): File = withContext(Dispatchers.IO) {
        val today = LocalDate.now(DateTimeUtils.getCurrentTimezone())
        val startDate = today.minusDays(days.toLong() - 1) // -1 because today counts as day 1
        val timestamp = DateTimeUtils.getCurrentEpochMs()
        val zipFileName = "${Constants.ZIP_FILE_PREFIX}-last-${days}days-${DateTimeUtils.formatTimestampForDebug(timestamp).replace(":", "").replace(" ", "-")}.zip"
        val zipFile = File(cacheDir, zipFileName)
        
        // Generate ZIP file
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // Get all events and filter by date range
            val allEvents = eventRepository.getAllEvents().first()
            val filteredEvents = allEvents.filter { event ->
                val eventDate = DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
                !eventDate.isBefore(startDate) && !eventDate.isAfter(today)
            }
            
            // Group by date
            val eventsByDate = filteredEvents.groupBy { event ->
                DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
            }
            
            // Sort dates in descending order (newest first)
            val sortedDates = eventsByDate.keys.sortedDescending()
            
            // Add CSV file for each date
            sortedDates.forEach { date ->
                val events = eventsByDate[date] ?: emptyList()
                val csvContent = generateCSVForDateWithEvents(date, events)
                val csvFileName = "${Constants.CSV_FILE_PREFIX}-${FormatUtils.formatDateForExport(date)}.csv"
                
                val entry = ZipEntry(csvFileName)
                zipOut.putNextEntry(entry)
                zipOut.write(csvContent.toByteArray())
                zipOut.closeEntry()
            }
            
            // Add summary CSV with filtered events
            val summaryCsv = generateCSVForEvents(filteredEvents)
            val summaryEntry = ZipEntry("${Constants.CSV_FILE_PREFIX}-last-${days}days-summary.csv")
            zipOut.putNextEntry(summaryEntry)
            zipOut.write(summaryCsv.toByteArray())
            zipOut.closeEntry()
        }
        
        zipFile
    }
    
    /**
     * Checks if the cached ZIP is still valid
     */
    suspend fun isCacheValid(): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastExportHash = prefsManager.lastExportZipHash.first()
            val lastExportTime = prefsManager.lastExportEpochMs.first()
            
            if (lastExportHash == null || lastExportTime == null) {
                return@withContext false
            }
            
            // Get current database hash
            val allEvents = eventRepository.getAllEvents().first()
            val currentDbHash = calculateEventsHash(allEvents)
            
            // Check if database has changed
            currentDbHash == lastExportHash
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates CSV content for a list of events
     */
    private suspend fun generateCSVForEvents(events: List<PowerEvent>): String = withContext(Dispatchers.IO) {
        val csv = StringBuilder()
        
        // CSV header
        csv.appendLine("date,start_time,end_time,duration_seconds,duration_hms")
        
        // Group events by date
        val eventsByDate = events.groupBy { event ->
            DateTimeUtils.epochMsToLocalDate(event.startEpochMs)
        }
        
        // Sort dates in descending order (newest first)
        val sortedDates = eventsByDate.keys.sortedDescending()
        
        // CSV rows
        sortedDates.forEach { date ->
            val eventsForDate = eventsByDate[date] ?: emptyList()
            eventsForDate.forEach { event ->
                val dateStr = FormatUtils.formatDateForExport(date)
                val startTime = FormatUtils.formatTimeForExport(event.startEpochMs)
                val endTime = if (event.endEpochMs != null) {
                    FormatUtils.formatTimeForExport(event.endEpochMs)
                } else {
                    "?"
                }
                val durationSec = event.durationSec ?: 0
                val durationHms = FormatUtils.formatDurationHMSForExport(durationSec)
                
                csv.appendLine("$dateStr,$startTime,$endTime,$durationSec,$durationHms")
            }
        }
        
        csv.toString()
    }
    
    /**
     * Generates CSV content for a specific date with provided events
     */
    private suspend fun generateCSVForDateWithEvents(date: LocalDate, events: List<PowerEvent>): String = withContext(Dispatchers.IO) {
        val csv = StringBuilder()
        
        // CSV header
        csv.appendLine("date,start_time,end_time,duration_seconds,duration_hms")
        
        // CSV rows
        events.forEach { event ->
            val dateStr = FormatUtils.formatDateForExport(date)
            val startTime = FormatUtils.formatTimeForExport(event.startEpochMs)
            val endTime = if (event.endEpochMs != null) {
                FormatUtils.formatTimeForExport(event.endEpochMs)
            } else {
                "?"
            }
            val durationSec = event.durationSec ?: 0
            val durationHms = FormatUtils.formatDurationHMSForExport(durationSec)
            
            csv.appendLine("$dateStr,$startTime,$endTime,$durationSec,$durationHms")
        }
        
        csv.toString()
    }
    
    /**
     * Calculates hash of all events for cache validation
     */
    private fun calculateEventsHash(events: List<PowerEvent>): String {
        val eventsString = events.joinToString("|") { event ->
            "${event.id}-${event.startEpochMs}-${event.endEpochMs}-${event.durationSec}"
        }
        return calculateStringHash(eventsString)
    }
    
    /**
     * Calculates hash of a file
     */
    private fun calculateFileHash(file: File): String {
        return file.readBytes().let { bytes ->
            calculateByteArrayHash(bytes)
        }
    }
    
    /**
     * Calculates hash of a string
     */
    private fun calculateStringHash(input: String): String {
        return calculateByteArrayHash(input.toByteArray())
    }
    
    /**
     * Calculates SHA-256 hash of byte array
     */
    private fun calculateByteArrayHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Clears the export cache
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            prefsManager.clearLastExportInfo()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
    
    /**
     * Gets the size of the cache directory
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.sumOf { file ->
                if (file.isFile) file.length() else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Gets the number of files in cache
     */
    suspend fun getCacheFileCount(): Int = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.count { it.isFile } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
