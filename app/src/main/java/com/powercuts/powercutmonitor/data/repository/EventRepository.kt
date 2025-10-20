package com.powercuts.powercutmonitor.data.repository

import com.powercuts.powercutmonitor.data.db.AppDatabase
import com.powercuts.powercutmonitor.data.db.dao.EventDao
import com.powercuts.powercutmonitor.data.db.entities.DailyTotal
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import com.powercuts.powercutmonitor.data.db.entities.TodayTotals
import com.powercuts.powercutmonitor.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for power event data operations
 * Abstracts database access and provides business logic
 */
class EventRepository(private val database: AppDatabase) {
    
    private val eventDao: EventDao = database.eventDao()
    
    /**
     * Insert a new power event
     */
    suspend fun insertEvent(event: PowerEvent): Long {
        return eventDao.insertEvent(event)
    }
    
    /**
     * Update an existing power event
     */
    suspend fun updateEvent(event: PowerEvent) {
        eventDao.updateEvent(event)
    }
    
    /**
     * Get all events as a Flow
     */
    fun getAllEvents(): Flow<List<PowerEvent>> {
        return eventDao.getAllEvents()
    }
    
    /**
     * Get events for today
     */
    suspend fun getTodayEvents(): List<PowerEvent> {
        val (startOfDay, endOfDay) = DateTimeUtils.getTodayBoundaries()
        return eventDao.getEventsForDate(startOfDay, endOfDay)
    }
    
    /**
     * Get events for a specific date
     */
    suspend fun getEventsForDate(date: LocalDate): List<PowerEvent> {
        val (startOfDay, endOfDay) = DateTimeUtils.getDateBoundaries(date)
        return eventDao.getEventsSpanningDateRange(startOfDay, endOfDay)
    }
    
    /**
     * Get today's totals (count and duration)
     */
    suspend fun getTodayTotals(): TodayTotals {
        val (startOfDay, endOfDay) = DateTimeUtils.getTodayBoundaries()
        val result = eventDao.getTodayTotals(startOfDay, endOfDay)
        
        return TodayTotals(
            count = result.eventCount,
            totalDurationSec = result.totalDurationSec,
            formattedDuration = DateTimeUtils.formatDuration(result.totalDurationSec)
        )
    }
    
    /**
     * Get today's totals as a reactive Flow
     */
    fun getTodayTotalsFlow(): Flow<TodayTotals> {
        return eventDao.getAllEvents().map { events ->
            val (startOfDay, endOfDay) = DateTimeUtils.getTodayBoundaries()
            val todayEvents = events.filter { it.startEpochMs >= startOfDay && it.startEpochMs <= endOfDay }
            
            val count = todayEvents.size
            val totalDurationSec = todayEvents.sumOf { it.durationSec ?: 0L }
            
            TodayTotals(
                count = count,
                totalDurationSec = totalDurationSec,
                formattedDuration = DateTimeUtils.formatDuration(totalDurationSec)
            )
        }
    }
    
    /**
     * Get daily totals for all days with events
     */
    fun getDailyTotals(): Flow<List<DailyTotal>> {
        return eventDao.getDailyTotals().map { results ->
            results.map { result ->
                DailyTotal(
                    date = DateTimeUtils.parseDateString(result.date),
                    eventCount = result.eventCount,
                    totalDurationSec = result.totalDurationSec,
                    events = emptyList() // Will be populated when needed
                )
            }
        }
    }
    
    /**
     * Delete all events for today
     */
    suspend fun deleteTodayEvents(): Int {
        val (startOfDay, endOfDay) = DateTimeUtils.getTodayBoundaries()
        return eventDao.deleteTodayEvents(startOfDay, endOfDay)
    }
    
    /**
     * Delete all events
     */
    suspend fun deleteAllEvents(): Int {
        return eventDao.deleteAllEvents()
    }

    suspend fun deleteEvent(event: PowerEvent) {
        eventDao.deleteEvent(event)
    }
    
    /**
     * Get the most recent event
     */
    suspend fun getMostRecentEvent(): PowerEvent? {
        return eventDao.getMostRecentEvent()
    }
    
    /**
     * Get ongoing events (events with no end time)
     */
    suspend fun getOngoingEvents(): List<PowerEvent> {
        return eventDao.getOngoingEvents()
    }
    
    /**
     * Update an ongoing event with its end time and duration
     */
    suspend fun updateOngoingEvent(eventId: Long, endEpochMs: Long, durationSec: Long): Int {
        return eventDao.updateOngoingEvent(eventId, endEpochMs, durationSec)
    }
    
    /**
     * Handle a power disconnect event
     * Creates a new ongoing event
     */
    suspend fun handlePowerDisconnected(eventTimeMs: Long): Long {
        val event = PowerEvent(
            startEpochMs = eventTimeMs,
            source = "DISCONNECTED"
        )
        return insertEvent(event)
    }
    
    /**
     * Handle a power connect event
     * Updates the most recent ongoing event or creates a new one
     */
    suspend fun handlePowerConnected(eventTimeMs: Long): Long {
        val ongoingEvents = getOngoingEvents()
        
        return if (ongoingEvents.isNotEmpty()) {
            // Update the most recent ongoing event
            val mostRecentEvent = ongoingEvents.first()
            val durationSec = (eventTimeMs - mostRecentEvent.startEpochMs) / 1000
            
            updateOngoingEvent(mostRecentEvent.id, eventTimeMs, durationSec)
            mostRecentEvent.id
        } else {
            // Create a new event (edge case: power was already connected)
            val event = PowerEvent(
                startEpochMs = eventTimeMs,
                endEpochMs = eventTimeMs,
                durationSec = 0,
                source = "CONNECTED"
            )
            insertEvent(event)
        }
    }
}
