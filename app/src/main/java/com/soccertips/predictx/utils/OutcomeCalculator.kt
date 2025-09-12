package com.soccertips.predictx.utils

import timber.log.Timber

/** Utility class to calculate the outcome of a prediction based on pick and result */
object OutcomeCalculator {

    /**
     * Calculate the outcome based on pick and result
     * @param pick The prediction made (e.g., "1", "2", "X", "1X", "2X", "Over 2.5", etc.)
     * @param result The match result (e.g., "2-1", "1-0", "3-3", etc.)
     * @return "win" if prediction was correct, "lose" if incorrect, or original outcome if
     * calculation not possible
     */
    fun calculateOutcome(pick: String?, result: String?, originalOutcome: String?): String {
        // Return original outcome if it's already valid
        if (!originalOutcome.isNullOrBlank() &&
                        originalOutcome != "Unknown" &&
                        originalOutcome != "-" &&
                        originalOutcome != ""
        ) {
            return originalOutcome
        }

        // Return original outcome if we don't have pick or result
        if (pick.isNullOrBlank() || result.isNullOrBlank() || result == "-" || result == "Unknown"
        ) {
            return originalOutcome ?: "Unknown"
        }

        try {
            return when {
                // Handle combination predictions first (e.g., "1 and Over 1.5")
                isCombinationPrediction(pick) -> calculateCombinationOutcome(pick, result)

                // Handle basic match outcome predictions (1, X, 2)
                isBasicMatchOutcome(pick) -> calculateBasicMatchOutcome(pick, result)

                // Handle double chance predictions (1X, 2X)
                isDoubleChance(pick) -> calculateDoubleChanceOutcome(pick, result)

                // Handle goal total predictions (Over/Under)
                isGoalTotalPrediction(pick) -> calculateGoalTotalOutcome(pick, result)

                // Handle Both Teams to Score predictions
                isBTTSPrediction(pick) -> calculateBTTSOutcome(pick, result)

                // Handle Half Time / Full Time predictions
                isHTFTPrediction(pick) -> calculateHTFTOutcome(pick, result)
                else -> {
                    Timber.d("Unknown pick format: $pick")
                    originalOutcome ?: "Unknown"
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating outcome for pick: $pick, result: $result")
            return originalOutcome ?: "Unknown"
        }
    }

    private fun isBasicMatchOutcome(pick: String): Boolean {
        return pick in listOf("1", "2", "X")
    }

    private fun isDoubleChance(pick: String): Boolean {
        return pick in listOf("1X", "2X")
    }

    private fun isGoalTotalPrediction(pick: String): Boolean {
        return pick.contains("Over") || pick.contains("Under")
    }

    private fun isCombinationPrediction(pick: String): Boolean {
        return pick.contains(" and ") || pick.contains(" & ")
    }

    private fun isBTTSPrediction(pick: String): Boolean {
        return pick.contains("GG") || pick.contains("BTTS")
    }

    private fun isHTFTPrediction(pick: String): Boolean {
        return pick.contains("HT/FT") || pick.contains("HT --")
    }

    private fun calculateBasicMatchOutcome(pick: String, result: String): String {
        val scores = parseScore(result) ?: return "Unknown"
        val (homeScore, awayScore) = scores

        return when (pick) {
            "1" -> if (homeScore > awayScore) "win" else "lose"
            "2" -> if (awayScore > homeScore) "win" else "lose"
            "X" -> if (homeScore == awayScore) "win" else "lose"
            else -> "Unknown"
        }
    }

    private fun calculateDoubleChanceOutcome(pick: String, result: String): String {
        val scores = parseScore(result) ?: return "Unknown"
        val (homeScore, awayScore) = scores

        return when (pick) {
            "1X" -> if (homeScore >= awayScore) "win" else "lose"
            "2X" -> if (awayScore >= homeScore) "win" else "lose"
            else -> "Unknown"
        }
    }

    private fun calculateGoalTotalOutcome(pick: String, result: String): String {
        val scores = parseScore(result) ?: return "Unknown"
        val (homeScore, awayScore) = scores
        val totalGoals = homeScore + awayScore

        return when {
            pick.contains("Over") -> {
                val threshold = extractGoalThreshold(pick) ?: return "Unknown"
                if (totalGoals > threshold) "win" else "lose"
            }
            pick.contains("Under") -> {
                val threshold = extractGoalThreshold(pick) ?: return "Unknown"
                if (totalGoals < threshold) "win" else "lose"
            }
            else -> "Unknown"
        }
    }

    private fun calculateCombinationOutcome(pick: String, result: String): String {
        val parts =
                when {
                    pick.contains(" and ") -> pick.split(" and ")
                    pick.contains(" & ") -> pick.split(" & ")
                    else -> return "Unknown"
                }

        if (parts.size != 2) return "Unknown"

        val firstPart = parts[0].trim()
        val secondPart = parts[1].trim()

        val firstResult =
                when {
                    isBasicMatchOutcome(firstPart) -> calculateBasicMatchOutcome(firstPart, result)
                    isGoalTotalPrediction(firstPart) -> calculateGoalTotalOutcome(firstPart, result)
                    isBTTSPrediction(firstPart) -> calculateBTTSOutcome(firstPart, result)
                    else -> "Unknown"
                }

        val secondResult =
                when {
                    isBasicMatchOutcome(secondPart) ->
                            calculateBasicMatchOutcome(secondPart, result)
                    isGoalTotalPrediction(secondPart) ->
                            calculateGoalTotalOutcome(secondPart, result)
                    isBTTSPrediction(secondPart) -> calculateBTTSOutcome(secondPart, result)
                    else -> "Unknown"
                }

        return if (firstResult == "win" && secondResult == "win") "win" else "lose"
    }

    private fun calculateBTTSOutcome(pick: String, result: String): String {
        val scores = parseScore(result) ?: return "Unknown"
        val (homeScore, awayScore) = scores

        val bothTeamsScored = homeScore > 0 && awayScore > 0

        return when {
            pick.contains("GG-NO") -> if (!bothTeamsScored) "win" else "lose"
            pick.contains("GG") || pick.contains("BTTS") -> if (bothTeamsScored) "win" else "lose"
            else -> "Unknown"
        }
    }

    private fun calculateHTFTOutcome(pick: String, result: String): String {
        // For now, return Unknown as HT/FT requires half-time score which might not be available
        // This can be enhanced if half-time scores are provided
        return "Unknown"
    }

    private fun parseScore(result: String): Pair<Int, Int>? {
        try {
            // Handle various score formats like "2-1", "1:0", "3-3:(Odds:1.250)", etc.
            val scorePattern = Regex("(\\d+)\\s*[-:]\\s*(\\d+)")
            val matchResult = scorePattern.find(result)

            return if (matchResult != null) {
                val homeScore = matchResult.groupValues[1].toInt()
                val awayScore = matchResult.groupValues[2].toInt()
                Pair(homeScore, awayScore)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing score: $result")
            return null
        }
    }

    private fun extractGoalThreshold(pick: String): Double? {
        try {
            val thresholdPattern = Regex("(\\d+(?:\\.\\d+)?)")
            val matchResult = thresholdPattern.find(pick)
            return matchResult?.value?.toDouble()
        } catch (e: Exception) {
            Timber.e(e, "Error extracting goal threshold from: $pick")
            return null
        }
    }
}
