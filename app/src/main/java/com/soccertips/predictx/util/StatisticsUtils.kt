package com.soccertips.predictx.util

import android.content.Context
import com.soccertips.predictx.R

/**
 * Maps API statistic type strings to localized resource IDs
 */
object StatisticsUtils {
    /**
     * Get the appropriate string resource ID for a statistic type from the API
     */
    fun getLocalizedStatTypeResourceId(statType: String): Int {
        return when (statType) {
            "Shots on Goal" -> R.string.shots_on_goal
            "Shots off Goal" -> R.string.shots_off_goal
            "Total Shots" -> R.string.total_shots
            "Blocked Shots" -> R.string.blocked_shots
            "Shots insidebox" -> R.string.shots_insidebox
            "Shots outsidebox" -> R.string.shots_outsidebox
            "Fouls" -> R.string.fouls
            "Corner Kicks" -> R.string.corner_kicks
            "Offsides" -> R.string.offsides
            "Ball Possession" -> R.string.ball_possession
            "Yellow Cards" -> R.string.yellow_cards
            "Red Cards" -> R.string.red_cards
            "Goalkeeper Saves" -> R.string.goalkeeper_saves
            "Total passes" -> R.string.total_passes
            "Passes accurate" -> R.string.passes_accurate
            "Passes %" -> R.string.passes_percentage
            "expected_goals" -> R.string.expected_goals
            "goals_prevented" -> R.string.goals_prevented
            else -> R.string.na
        }
    }

    /**
     * Extract numeric value from a string, handling percentage values and null cases
     */
    fun extractNumericValue(value: String?): Double? {
        if (value == null || value == "null" || value.isEmpty() || value == "N/A") return null

        return try {
            // If the value is a percentage (e.g., "60%"), extract the number
            if (value.endsWith("%")) {
                value.substringBefore("%").toDouble()
            } else {
                value.toDouble()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Format the value for display, handling null cases appropriately
     */
    fun formatValue(value: String?, context: Context): String {
        if (value == null || value == "null" || value.isEmpty()) {
            return context.getString(R.string.na)
        }
        return value
    }
}
