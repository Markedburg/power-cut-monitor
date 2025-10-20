package com.powercuts.powercutmonitor.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.ZoneId

/**
 * Entity representing a power outage event
 */
@Entity(tableName = "power_events")
data class PowerEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * When the power outage started (UTC epoch milliseconds)
     */
    val startEpochMs: Long,
    
    /**
     * When the power outage ended (UTC epoch milliseconds)
     * null if outage is still ongoing
     */
    val endEpochMs: Long? = null,
    
    /**
     * Duration of the outage in seconds
     * null if outage is still ongoing
     */
    val durationSec: Long? = null,
    
    /**
     * Source of the event (CONNECTED or DISCONNECTED)
     */
    val source: String,
    
    /**
     * Optional notes about the event
     */
    val notes: String? = null,
    
    /**
     * When this record was created (UTC epoch milliseconds)
     */
    val createdMs: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this event is still ongoing (no end time)
     */
    fun isOngoing(): Boolean = endEpochMs == null
    
    /**
     * Gets the local date when this event started
     */
    fun getStartLocalDate(): LocalDate {
        return java.time.Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    
    /**
     * Gets the local date when this event ended (or null if ongoing)
     */
    fun getEndLocalDate(): LocalDate? {
        return endEpochMs?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }
    
    /**
     * Checks if this event spans multiple days
     */
    fun spansMultipleDays(): Boolean {
        val startDate = getStartLocalDate()
        val endDate = getEndLocalDate()
        return endDate != null && startDate != endDate
    }
}
