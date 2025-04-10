package com.soccertips.predictx.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    fun formatRelativeDate(dateString: String?): String {
        if (dateString == null) return "Unknown Date"

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateString, formatter)
        val today = LocalDate.now()

        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }
}