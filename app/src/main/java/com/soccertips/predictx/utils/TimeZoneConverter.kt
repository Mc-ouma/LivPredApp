package com.soccertips.predictx.utils

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import timber.log.Timber

object TimeZoneConverter {

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
                DateTimeFormatter.ofPattern("HH:mm:ss")
            } else {
                DateTimeFormatter.ofPattern("HH:mm")
            }

            val localTime = LocalTime.parse(utcTime, timeFormatter)

            // Parse the date
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = java.time.LocalDate.parse(utcDate, dateFormatter)

            // Combine date and time in UTC timezone
            val utcDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC"))

            // Convert to user's local timezone
            val localDateTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault())

            // Format the output time
            val outputFormatter = DateTimeFormatter.ofPattern("HH:mm")
            return localDateTime.format(outputFormatter)

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
                DateTimeFormatter.ofPattern("HH:mm:ss")
            } else {
                DateTimeFormatter.ofPattern("HH:mm")
            }

            val localTime = LocalTime.parse(utcTime, timeFormatter)

            // Parse the date
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = java.time.LocalDate.parse(utcDate, dateFormatter)

            // Combine date and time in UTC timezone
            val utcDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC"))

            // Convert to user's local timezone
            val localDateTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault())

            // Format the output
            val outputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val outputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            return Pair(
                localDateTime.format(outputDateFormatter),
                localDateTime.format(outputTimeFormatter)
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

