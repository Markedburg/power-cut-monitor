package com.powercuts.powercutmonitor.data.db.dao

import androidx.room.*
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for PowerEvent operations
 */
@Dao
interface EventDao {
    
    /**
     * Insert a new power event
     */
    @Insert
    suspend fun insertEvent(event: PowerEvent): Long
    
    /**
     * Update an existing power event
     */
    @Update
    suspend fun updateEvent(event: PowerEvent)
    
    /**
     * Delete a power event
     */
    @Delete
    suspend fun deleteEvent(event: PowerEvent)
    
    /**
     * Get all events, ordered by start time (newest first)
     */
    @Query("SELECT * FROM power_events ORDER BY startEpochMs DESC")
    fun getAllEvents(): Flow<List<PowerEvent>>
    
    /**
     * Get events for today (based on local timezone)
     */
    @Query("""
        SELECT * FROM power_events 
        WHERE startEpochMs >= :startOfDayMs 
        AND startEpochMs < :endOfDayMs
        ORDER BY startEpochMs DESC
    """)
    suspend fun getTodayEvents(startOfDayMs: Long, endOfDayMs: Long): List<PowerEvent>
    
    /**
     * Get events for a specific date (based on local timezone)
     */
    @Query("""
        SELECT * FROM power_events 
        WHERE startEpochMs >= :startOfDayMs 
        AND startEpochMs < :endOfDayMs
        ORDER BY startEpochMs DESC
    """)
    suspend fun getEventsForDate(startOfDayMs: Long, endOfDayMs: Long): List<PowerEvent>
    
    /**
     * Get events that span across the given date range
     * This handles cases where an event starts on one day and ends on another
     */
    @Query("""
        SELECT * FROM power_events 
        WHERE (startEpochMs >= :startOfDayMs AND startEpochMs < :endOfDayMs)
        OR (endEpochMs IS NOT NULL AND endEpochMs >= :startOfDayMs AND endEpochMs < :endOfDayMs)
        OR (startEpochMs < :startOfDayMs AND (endEpochMs IS NULL OR endEpochMs >= :endOfDayMs))
        ORDER BY startEpochMs DESC
    """)
    suspend fun getEventsSpanningDateRange(startOfDayMs: Long, endOfDayMs: Long): List<PowerEvent>
    
    /**
     * Get today's totals (count and duration)
     */
    @Query("""
        SELECT 
            COUNT(*) as eventCount,
            COALESCE(SUM(durationSec), 0) as totalDurationSec
        FROM power_events 
        WHERE startEpochMs >= :startOfDayMs 
        AND startEpochMs < :endOfDayMs
    """)
    suspend fun getTodayTotals(startOfDayMs: Long, endOfDayMs: Long): TodayTotalsResult
    
    /**
     * Get daily totals for all days with events
     */
    @Query("""
        SELECT 
            DATE(startEpochMs / 1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as eventCount,
            COALESCE(SUM(durationSec), 0) as totalDurationSec
        FROM power_events 
        GROUP BY DATE(startEpochMs / 1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
    """)
    fun getDailyTotals(): Flow<List<DailyTotalResult>>
    
    /**
     * Delete all events for today
     */
    @Query("""
        DELETE FROM power_events 
        WHERE startEpochMs >= :startOfDayMs 
        AND startEpochMs < :endOfDayMs
    """)
    suspend fun deleteTodayEvents(startOfDayMs: Long, endOfDayMs: Long): Int
    
    /**
     * Delete all events
     */
    @Query("DELETE FROM power_events")
    suspend fun deleteAllEvents(): Int
    
    /**
     * Get the most recent event
     */
    @Query("SELECT * FROM power_events ORDER BY startEpochMs DESC LIMIT 1")
    suspend fun getMostRecentEvent(): PowerEvent?
    
    /**
     * Get ongoing events (events with no end time)
     */
    @Query("SELECT * FROM power_events WHERE endEpochMs IS NULL ORDER BY startEpochMs DESC")
    suspend fun getOngoingEvents(): List<PowerEvent>
    
    /**
     * Update an ongoing event with its end time and duration
     */
    @Query("""
        UPDATE power_events 
        SET endEpochMs = :endEpochMs, durationSec = :durationSec 
        WHERE id = :eventId AND endEpochMs IS NULL
    """)
    suspend fun updateOngoingEvent(eventId: Long, endEpochMs: Long, durationSec: Long): Int
}

/**
 * Result class for today's totals query
 */
data class TodayTotalsResult(
    val eventCount: Int,
    val totalDurationSec: Long
)

/**
 * Result class for daily totals query
 */
data class DailyTotalResult(
    val date: String, // Will be converted to LocalDate
    val eventCount: Int,
    val totalDurationSec: Long
)
