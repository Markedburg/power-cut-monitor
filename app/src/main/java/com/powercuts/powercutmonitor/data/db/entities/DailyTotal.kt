package com.powercuts.powercutmonitor.data.db.entities

import java.time.LocalDate

/**
 * Data class representing daily aggregated totals
 * Used for efficient querying of daily statistics
 */
data class DailyTotal(
    /**
     * The local date for this total
     */
    val date: LocalDate,
    
    /**
     * Number of power cuts on this date
     */
    val eventCount: Int,
    
    /**
     * Total duration of power outages on this date (in seconds)
     */
    val totalDurationSec: Long,
    
    /**
     * The actual PowerEvent data for this day
     */
    val events: List<PowerEvent> = emptyList()
)

/**
 * Data class for today's totals specifically
 */
data class TodayTotals(
    /**
     * Number of power cuts today
     */
    val count: Int,
    
    /**
     * Total duration of power outages today (in seconds)
     */
    val totalDurationSec: Long,
    
    /**
     * Formatted duration string (e.g., "2h 15m 30s")
     */
    val formattedDuration: String
)
