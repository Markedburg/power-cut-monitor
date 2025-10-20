package com.powercuts.powercutmonitor.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Utility functions for date and time operations
 * Handles timezone conversion and formatting
 */
object DateTimeUtils {
    
    /**
     * Gets the current timezone
     */
    fun getCurrentTimezone(): ZoneId = ZoneId.systemDefault()
    
    /**
     * Gets today's date boundaries in UTC epoch milliseconds
     * Returns (startOfDay, endOfDay) for the current local date
     */
    fun getTodayBoundaries(): Pair<Long, Long> {
        val today = LocalDate.now(getCurrentTimezone())
        return getDateBoundaries(today)
    }
    
    /**
     * Gets date boundaries for a specific date in UTC epoch milliseconds
     * Returns (startOfDay, endOfDay) for the given local date
     */
    fun getDateBoundaries(date: LocalDate): Pair<Long, Long> {
        val timezone = getCurrentTimezone()
        val startOfDay = date.atStartOfDay(timezone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(timezone).toInstant().toEpochMilli()
        return Pair(startOfDay, endOfDay)
    }
    
    /**
     * Converts UTC epoch milliseconds to local date
     */
    fun epochMsToLocalDate(epochMs: Long): LocalDate {
        return java.time.Instant.ofEpochMilli(epochMs)
            .atZone(getCurrentTimezone())
            .toLocalDate()
    }
    
    /**
     * Converts UTC epoch milliseconds to local date time
     */
    fun epochMsToLocalDateTime(epochMs: Long): LocalDateTime {
        return java.time.Instant.ofEpochMilli(epochMs)
            .atZone(getCurrentTimezone())
            .toLocalDateTime()
    }
    
    /**
     * Formats a LocalDate to a date header string (e.g., "Sun, 12 Oct 2025")
     */
    fun formatDateHeader(date: java.time.LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("E, d MMM yyyy"))
    }
    
    /**
     * Formats a UTC epoch milliseconds timestamp to local date string
     */
    fun formatEventDate(epochMs: Long): String {
        val localDate = epochMsToLocalDate(epochMs)
        return localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    
    /**
     * Formats a UTC epoch milliseconds timestamp to local time string (time only)
     */
    fun formatEventTime(epochMs: Long): String {
        val localDateTime = epochMsToLocalDateTime(epochMs)
        return localDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    /**
     * Formats a UTC epoch milliseconds timestamp to local date and time string
     */
    fun formatEventDateTime(epochMs: Long): String {
        val localDateTime = epochMsToLocalDateTime(epochMs)
        return localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
    }
    
    /**
     * Formats duration in seconds to human-readable string
     * Format: "Xh Ym Zs" or "Ym Zs" or "Zs"
     */
    fun formatDuration(durationSec: Long): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Formats duration in milliseconds to human-readable string
     */
    fun formatDurationMs(durationMs: Long): String {
        return formatDuration(durationMs / 1000)
    }
    
    /**
     * Gets a display name for a date (Today, Yesterday, or formatted date)
     */
    fun getDateDisplayName(date: LocalDate): String {
        val today = LocalDate.now(getCurrentTimezone())
        val yesterday = today.minusDays(1)
        
        return when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        }
    }
    
    /**
     * Parses a date string from database query result
     * Expected format: "YYYY-MM-DD"
     */
    fun parseDateString(dateString: String): LocalDate {
        return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Gets the current UTC epoch milliseconds
     */
    fun getCurrentEpochMs(): Long = System.currentTimeMillis()
    
    /**
     * Checks if two dates are the same day
     */
    fun isSameDay(date1: LocalDate, date2: LocalDate): Boolean {
        return date1 == date2
    }
    
    /**
     * Gets the number of days between two dates
     */
    fun getDaysBetween(date1: LocalDate, date2: LocalDate): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(date1, date2)
    }
    
    /**
     * Gets the start of day for a given date in UTC epoch milliseconds
     */
    fun getStartOfDayEpochMs(date: LocalDate): Long {
        return date.atStartOfDay(getCurrentTimezone()).toInstant().toEpochMilli()
    }
    
    /**
     * Gets the end of day for a given date in UTC epoch milliseconds
     */
    fun getEndOfDayEpochMs(date: LocalDate): Long {
        return date.plusDays(1).atStartOfDay(getCurrentTimezone()).toInstant().toEpochMilli()
    }
    
    /**
     * Formats timestamp for debug/filename purposes
     * Format: "YYYY-MM-DD_HH-MM-SS"
     */
    fun formatTimestampForDebug(epochMs: Long): String {
        val localDateTime = epochMsToLocalDateTime(epochMs)
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    }
}
