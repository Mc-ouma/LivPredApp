package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.statistics.Response
import com.soccertips.predictx.data.model.statistics.Team
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation

@Composable
fun FixtureStatisticsScreen(statistics: List<Response>) {
    if (statistics.size < 2) return // Ensure there are at least two teams

    val team1 = statistics[0]
    val team2 = statistics[1]

    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .wrapContentHeight()
                .fillMaxWidth(),
        ) {
            // Display team headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamHeader(team1.team)
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                TeamHeader(team2.team)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display shared statistics in rows
            team1.statistics.forEachIndexed { index, stat ->
                val team1Value = stat.value?.toString() ?: "N/A"
                val statType = stat.type
                val team2Value = team2.statistics.getOrNull(index)?.value?.toString() ?: "N/A"

                SharedStatisticRow(
                    team1Value = team1Value,
                    statType = statType,
                    team2Value = team2Value
                )
            }
        }
    }
}

@Composable
fun TeamHeader(team: Team) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = team.logo),
            contentDescription = "${team.name} logo",
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
fun SharedStatisticRow(
    team1Value: String,
    statType: String,
    team2Value: String,
) {
    // Get formatted values
    val formattedTeam1Value = formatValue(team1Value)
    val formattedTeam2Value = formatValue(team2Value)

    // Get styles for each team based on comparison
    val team1Style = getTextStyle(formattedTeam1Value, formattedTeam2Value)
    val team2Style = getTextStyle(formattedTeam2Value, formattedTeam1Value)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formattedTeam1Value,
            textAlign = TextAlign.Center,
            style = team1Style
        )
        Text(
            text = statType,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formattedTeam2Value,
            textAlign = TextAlign.Center,
            style = team2Style
        )
    }
}


@Composable
fun getTextStyle(value1: String, value2: String): TextStyle {
    val numericValue1 = extractNumericValue(value1)
    val numericValue2 = extractNumericValue(value2)

    return when {
        numericValue1 != null && numericValue2 != null -> {
            when {
                numericValue1 > numericValue2 -> {
                    // If value1 is greater, highlight value1
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                numericValue2 > numericValue1 -> {
                    // If value2 is greater, highlight value2
                    MaterialTheme.typography.bodyMedium
                }

                else -> {
                    // If both are equal, no highlighting
                    MaterialTheme.typography.bodyMedium
                }
            }
        }

        else -> {
            // For non-numeric values, apply default style (no highlight)
            MaterialTheme.typography.bodyMedium
        }
    }
}


fun formatValue(value: String): String {
    val numericValue = extractNumericValue(value)
    return if (numericValue != null) {
        // If the value is a whole number, format it as an integer
        if (numericValue == numericValue.toInt().toDouble()) {
            "${numericValue.toInt()}"
        } else {
            value
        }
    } else {
        value // If not numeric, return the original value
    }
}

fun extractNumericValue(value: String): Double? {
    // Check if it's a percentage (contains '%')
    return when {
        value.contains("%") -> {
            // Remove the '%' sign and convert to Double
            val percentageValue = value.removeSuffix("%")
            percentageValue.toDoubleOrNull()
                ?.div(100) // Return value as fraction (e.g., 54% becomes 0.54 for comparison)
        }

        else -> {
            value.toDoubleOrNull() // Return the numeric value, or null if it's not a number
        }
    }
}


