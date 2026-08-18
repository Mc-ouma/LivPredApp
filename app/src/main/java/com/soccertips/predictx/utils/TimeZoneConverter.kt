package com.soccertips.predictx.utils

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import timber.log.Timber

object TimeZoneConverter {

    private val TIME_FORMATTER_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val UTC_ZONE = ZoneId.of("UTC")

    /**
     * Converts UTC time string to user's local timezone
     * @param utcTime Time string in UTC (e.g., "14:30", "14:30:00")
     * @param utcDate Date string in format "yyyy-MM-dd"
     * @return Formatted time string in user's local timezone (e.g., "14:30")
     */
    fun convertUtcToLocal(utcTime: String?, utcDate: String?): String {
        if (utcTime.isNullOrBlank() || utcDate.isNullOrBlank()) {
            return utcTime ?: "TBD"
        }

        try {
            // Parse the time (supports both HH:mm and HH:mm:ss formats)
            val timeFormatter = if (utcTime.count { it == ':' } == 2) {
                TIME_FORMATTER_SECONDS
            } else {
                TIME_FORMATTER
            }

            val localTime = LocalTime.parse(utcTime, timeFormatter)
            val localDate = java.time.LocalDate.parse(utcDate, DATE_FORMATTER)

            // Combine date and time in UTC timezone
            val utcDateTime = ZonedDateTime.of(localDate, localTime, UTC_ZONE)

            // Convert to user's local timezone
            val localDateTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault())

            // Format the output time
            return localDateTime.format(TIME_FORMATTER)

        } catch (e: DateTimeParseException) {
            Timber.e(e, "Error parsing UTC time: $utcTime or date: $utcDate")
            return utcTime
        } catch (e: Exception) {
            Timber.e(e, "Error converting UTC time to local: $utcTime")
            return utcTime
        }
    }

    /**
     * Converts UTC time string to user's local timezone with date
     * @param utcTime Time string in UTC (e.g., "14:30", "14:30:00")
     * @param utcDate Date string in format "yyyy-MM-dd"
     * @return Pair of (localDate: String, localTime: String) in user's timezone
     */
    fun convertUtcToLocalWithDate(utcTime: String?, utcDate: String?): Pair<String, String> {
        if (utcTime.isNullOrBlank() || utcDate.isNullOrBlank()) {
            return Pair(utcDate ?: "Unknown", utcTime ?: "TBD")
        }

        try {
            // Parse the time
            val timeFormatter = if (utcTime.count { it == ':' } == 2) {
                TIME_FORMATTER_SECONDS
            } else {
                TIME_FORMATTER
            }

            val localTime = LocalTime.parse(utcTime, timeFormatter)
            val localDate = java.time.LocalDate.parse(utcDate, DATE_FORMATTER)

            // Combine date and time in UTC timezone
            val utcDateTime = ZonedDateTime.of(localDate, localTime, UTC_ZONE)

            // Convert to user's local timezone
            val localDateTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault())

            return Pair(
                localDateTime.format(DATE_FORMATTER),
                localDateTime.format(TIME_FORMATTER)
            )

        } catch (e: Exception) {
            Timber.e(e, "Error converting UTC to local with date: $utcTime, $utcDate")
            return Pair(utcDate, utcTime)
        }
    }

    /**
     * Get timezone offset string (e.g., "GMT+3", "GMT-5")
     */
    fun getTimezoneOffset(): String {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val offset = now.offset

        val hours = offset.totalSeconds / 3600
        return if (hours >= 0) {
            "GMT+$hours"
        } else {
            "GMT$hours"
        }
    }
}

