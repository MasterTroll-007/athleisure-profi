package com.fitness.app.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateUtils {
    /**
     * Safely parse an ISO date-time string, handling Z suffix and parse errors.
     * Returns null if parsing fails.
     */
    fun parseDateTime(dateString: String?): LocalDateTime? {
        if (dateString.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(dateString.removeSuffix("Z"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format a LocalDateTime using locale-aware formatting.
     */
    fun formatDateTime(
        dateTime: LocalDateTime?,
        pattern: String = "EEE, d MMM • HH:mm",
        locale: Locale = Locale.getDefault()
    ): String {
        if (dateTime == null) return ""
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            dateTime.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Format date only.
     */
    fun formatDate(
        dateTime: LocalDateTime?,
        pattern: String = "EEE, d MMM yyyy",
        locale: Locale = Locale.getDefault()
    ): String {
        if (dateTime == null) return ""
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            dateTime.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Format time only.
     */
    fun formatTime(
        dateTime: LocalDateTime?,
        pattern: String = "HH:mm",
        locale: Locale = Locale.getDefault()
    ): String {
        if (dateTime == null) return ""
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            dateTime.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }
}
