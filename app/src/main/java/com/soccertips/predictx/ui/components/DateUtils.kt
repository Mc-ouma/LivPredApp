package com.soccertips.predictx.ui.components

import android.content.Context
import android.content.res.Resources
import com.soccertips.predictx.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    fun formatRelativeDate(context: Context, dateString: String?): String {
        if (dateString == null) return context.getString(R.string.unknown_date)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateString, formatter)
        val today = LocalDate.now()

        return when (date) {
            today -> context.getString(R.string.today)
            today.plusDays(1) -> context.getString(R.string.tomorrow)
            today.minusDays(1) -> context.getString(R.string.yesterday)
            else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }
}