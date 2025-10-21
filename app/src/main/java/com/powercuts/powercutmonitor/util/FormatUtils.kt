package com.powercuts.powercutmonitor.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Utility functions for formatting data for UI display
 * Extends DateTimeUtils with UI-specific formatting
 */
object FormatUtils {
    
    /**
     * Formats duration for status card display
     * Format: "Xh Ym Zs" with proper spacing
     */
    fun formatDurationForCard(durationSec: Long): String {
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
     * Formats duration for event list display
     * Format: "Xm Ys" or "Xs" for shorter display
     */
    fun formatDurationForList(durationSec: Long): String {
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
     * Formats time for event list display
     * Format: "H:MM AM/PM"
     */
    fun formatTimeForList(epochMs: Long): String {
        return DateTimeUtils.formatEventTime(epochMs)
    }
    
    /**
     * Formats date for section headers
     * Format: "Today", "Yesterday", or "Day, Month D"
     */
    fun formatDateForHeader(date: LocalDate): String {
        return DateTimeUtils.getDateDisplayName(date)
    }
    
    /**
     * Formats event time range for display
     * Format: "H:MM AM → H:MM AM"
     */
    fun formatEventTimeRange(startEpochMs: Long, endEpochMs: Long?): String {
        val startTime = formatTimeForList(startEpochMs)
        val endTime = if (endEpochMs != null) {
            formatTimeForList(endEpochMs)
        } else {
            "…"
        }
        return "$startTime → $endTime"
    }
    
    /**
     * Formats event duration for display
     * Shows "ongoing" for events without end time
     */
    fun formatEventDuration(startEpochMs: Long, endEpochMs: Long?, durationSec: Long?): String {
        return if (endEpochMs == null) {
            // Ongoing event - calculate duration from start to now
            val currentTime = System.currentTimeMillis()
            val elapsedSec = (currentTime - startEpochMs) / 1000
            "${formatDurationForList(elapsedSec)} (ongoing)"
        } else {
            // Completed event
            formatDurationForList(durationSec ?: 0)
        }
    }
    
    /**
     * Formats power cuts count for display
     * Adds proper pluralization
     */
    fun formatPowerCutsCount(count: Int): String {
        return when (count) {
            0 -> "No power cuts"
            1 -> "1 power cut"
            else -> "$count power cuts"
        }
    }
    
    /**
     * Formats power-off time for display
     * Shows "No downtime" for zero duration
     */
    fun formatPowerOffTime(durationSec: Long): String {
        return if (durationSec == 0L) {
            "No downtime"
        } else {
            formatDurationForCard(durationSec)
        }
    }
    
    /**
     * Formats date for CSV export
     * Format: "YYYY-MM-DD"
     */
    fun formatDateForExport(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Formats time for CSV export
     * Format: "HH:MM:SS AM/PM"
     */
    fun formatTimeForExport(epochMs: Long): String {
        val localDateTime = DateTimeUtils.epochMsToLocalDateTime(epochMs)
        return localDateTime.format(DateTimeFormatter.ofPattern("h:mm:ss a"))
    }
    
    /**
     * Formats duration for CSV export
     * Format: "HH:MM:SS"
     */
    fun formatDurationForExport(durationSec: Long): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    /**
     * Formats duration in HMS format for CSV
     * Format: "Xh Ym Zs"
     */
    fun formatDurationHMSForExport(durationSec: Long): String {
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
     * Formats file size for display
     * Format: "X KB", "X MB", etc.
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Formats timestamp for debug logs
     * Format: "YYYY-MM-DD HH:MM:SS.SSS"
     */
    fun formatTimestampForDebug(epochMs: Long): String {
        val localDateTime = DateTimeUtils.epochMsToLocalDateTime(epochMs)
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    }
}
