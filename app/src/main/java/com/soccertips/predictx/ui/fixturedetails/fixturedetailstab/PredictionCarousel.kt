package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.prediction.Biggest
import com.soccertips.predictx.data.model.prediction.BiggestGoals
import com.soccertips.predictx.data.model.prediction.CleanSheet
import com.soccertips.predictx.data.model.prediction.Comparison
import com.soccertips.predictx.data.model.prediction.FailedToScore
import com.soccertips.predictx.data.model.prediction.FixtureDetail
import com.soccertips.predictx.data.model.prediction.Fixtures
import com.soccertips.predictx.data.model.prediction.GoalAverage
import com.soccertips.predictx.data.model.prediction.GoalData
import com.soccertips.predictx.data.model.prediction.GoalStats
import com.soccertips.predictx.data.model.prediction.GoalTotal
import com.soccertips.predictx.data.model.prediction.H2H
import com.soccertips.predictx.data.model.prediction.HomeAway
import com.soccertips.predictx.data.model.prediction.Last5
import com.soccertips.predictx.data.model.prediction.Last5Goals
import com.soccertips.predictx.data.model.prediction.Predictions
import com.soccertips.predictx.data.model.prediction.Streak
import com.soccertips.predictx.data.model.prediction.Team
import com.soccertips.predictx.data.model.prediction.TeamGoals
import com.soccertips.predictx.data.model.prediction.TeamLeague
import com.soccertips.predictx.data.model.prediction.Teams
import com.soccertips.predictx.data.model.prediction.WinLoss
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PredictionCarousel(
    predictions: Predictions,
    comparison: Comparison,
    teams: Teams,
    h2h: List<H2H>
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val cardHeight = 480.dp // Adjust height as needed

    Column {

        // Horizontal pager for cards
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            beyondViewportPageCount = 1
        ) { page ->

            when (page) {
                0 -> PredictionOverviewCard(predictions)
                1 -> TeamFormComparisonCard(comparison, teams)
                2 -> GoalsAnalysisCard(teams)
                3 -> HeadToHeadCard(h2h)
            }

        }

        // Page indicator
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { page ->
                Box(
                    Modifier
                        .size(if (pagerState.currentPage == page) 20.dp else 14.dp)
                        .padding(4.dp)
                        .background(
                            if (pagerState.currentPage == page)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun GoalsAnalysisCard(teams: Teams) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Goals Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Team headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamHeader(
                    team = teams.home,
                    modifier = Modifier.weight(1f)
                )
                TeamHeader(
                    team = teams.away,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Last 5 matches stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Last 5 Matches Goals",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Goals For
                    GoalsRow(
                        homeTeamsName = teams.home.name,
                        awayTeamsName = teams.away.name,
                        title = "Goals For:",
                        homeGoals = teams.home.last_5.goals.`for`,
                        awayGoals = teams.away.last_5.goals.`for`,
                        homeColor = MaterialTheme.colorScheme.primary,
                        awayColor = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Goals Against
                    GoalsRow(
                        awayTeamsName = teams.away.name,
                        homeTeamsName = teams.home.name,
                        title = "Goals Against:",
                        homeGoals = teams.home.last_5.goals.against,
                        awayGoals = teams.away.last_5.goals.against,
                        homeColor = Color.Red.copy(alpha = 0.7f),
                        awayColor = Color.Red.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Season stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Season Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SeasonStatsRow(
                        homeTeam = teams.home,
                        awayTeam = teams.away
                    )
                }
            }
        }
    }
}

@Composable
fun GoalsRow(
    homeTeamsName: String,
    awayTeamsName: String,
    title: String,
    homeGoals: GoalData,
    awayGoals: GoalData,
    homeColor: Color,
    awayColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(120.dp)
        )

        GoalBar(
            teamName = homeTeamsName,
            value = homeGoals.total.toFloat(),
            maxValue = maxOf(homeGoals.total, awayGoals.total).toFloat(),
            color = homeColor,
            text = "${homeGoals.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    homeGoals.average
                )
            })",
            modifier = Modifier.weight(2f)
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        GoalBar(
            teamName = awayTeamsName,
            value = awayGoals.total.toFloat(),
            maxValue = maxOf(homeGoals.total, awayGoals.total).toFloat(),
            color = awayColor,
            text = "${awayGoals.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    awayGoals.average
                )
            })",
            modifier = Modifier.weight(3f)
        )
    }
}

@Composable
fun SeasonStatsRow(homeTeam: Team, awayTeam: Team) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        GoalStatsColumn(
            title = homeTeam.name,
            goalsScored = "${homeTeam.league.goals.`for`.total.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    homeTeam.league.goals.`for`.average.total
                )
            })",
            goalsConceded = "${homeTeam.league.goals.against.total.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    homeTeam.league.goals.against.average.total
                )
            })",
            cleanSheets = homeTeam.league.clean_sheet.total.toString(),
            failedToScore = homeTeam.league.failed_to_score.total.toString()
        )

        GoalStatsColumn(
            title = awayTeam.name,
            goalsScored = "${awayTeam.league.goals.`for`.total.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    awayTeam.league.goals.`for`.average.total
                )
            })",
            goalsConceded = "${awayTeam.league.goals.against.total.total} (${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    awayTeam.league.goals.against.average.total
                )
            })",
            cleanSheets = awayTeam.league.clean_sheet.total.toString(),
            failedToScore = awayTeam.league.failed_to_score.total.toString()
        )
    }
}

@Composable
fun GoalBar(
    teamName: String,
    value: Float,
    maxValue: Float,
    color: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = teamName,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
                .width(60.dp)
        )
        Box(
            modifier = modifier
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.3f))
        ) {
            val fillPercentage = if (maxValue > 0) value / maxValue else 0f
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillPercentage)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(color)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
                .padding(start = 4.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun GoalStatsColumn(
    title: String,
    goalsScored: String,
    goalsConceded: String,
    cleanSheets: String,
    failedToScore: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        StatRow("Scored:", goalsScored)
        StatRow("Conceded:", goalsConceded)
        StatRow("Clean sheets:", cleanSheets)
        StatRow("Failed to score:", failedToScore)
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun HeadToHeadCard(h2h: List<H2H>) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Head-to-Head History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (h2h.isEmpty()) {
                Text(
                    "No head to head history available",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // H2H Summary
                val homeTeam = h2h.first().teams.home.name
                val awayTeam = h2h.first().teams.away.name

                val homeWins = h2h.count { it.teams.home.winner == true }
                val awayWins = h2h.count { it.teams.away.winner == true }
                val draws =
                    h2h.count { it.teams.home.winner == null && it.teams.away.winner == null }

                // Simple win-draw-loss chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(vertical = 4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                ) {
                    val total = h2h.size.toFloat()

                    val homeWeight = maxOf(0.01f, homeWins / total)// to avoid division by zero
                    val drawWeight = maxOf(0.01f, draws / total)
                    val awayWeight = maxOf(0.01f, awayWins / total)

                    Box(
                        modifier = Modifier
                            .weight(homeWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Box(
                        modifier = Modifier
                            .weight(drawWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary)
                    )

                    Box(
                        modifier = Modifier
                            .weight(awayWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$homeTeam: $homeWins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Draws: $draws",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        "$awayTeam: $awayWins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recent matches list
                Text(
                    "Last 5 Matches",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show most recent 5 matches or fewer if less available
                val recentMatches = h2h.take(5)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentMatches.forEach { match ->
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                        val dateTime = LocalDateTime.parse(match.fixture.date, formatter)
                        val formattedDate =
                            dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Date and competition
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = match.league.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Match result
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Home team
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(2f)
                                    ) {
                                        Text(
                                            text = match.teams.home.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Score
                                    Text(
                                        text = "${match.goals.home} - ${match.goals.away}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    // Away team
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(2f)
                                    ) {
                                        Text(
                                            text = match.teams.away.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionOverviewCard(predictions: Predictions) {
    val homePercent = predictions.percent.home.removeSuffix("%").toFloatOrNull() ?: 0f
    val drawPercent = predictions.percent.draw.removeSuffix("%").toFloatOrNull() ?: 0f
    val awayPercent = predictions.percent.away.removeSuffix("%").toFloatOrNull() ?: 0f

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Match Prediction Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Pie Chart Visual Representation using Canvas
            PieChartVisual(
                homePercent = homePercent,
                drawPercent = drawPercent,
                awayPercent = awayPercent,
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    text = "Home: ${predictions.percent.home}"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.secondary,
                    text = "Draw: ${predictions.percent.draw}"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiary,
                    text = "Away: ${predictions.percent.away}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            predictions.advice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (predictions.win_or_draw) {
                Text(
                    text = "Win or Draw: Likely",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }


            predictions.under_over?.let {

                Text(
                    text = "Goals Under/Over: ${predictions.under_over}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PieChartVisual(
    homePercent: Float,
    drawPercent: Float,
    awayPercent: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2

        val total = homePercent + drawPercent + awayPercent
        val homeSweepAngle = 360f * (homePercent / total)
        val drawSweepAngle = 360f * (drawPercent / total)
        val awaySweepAngle = 360f * (awayPercent / total)


        var startAngle = 0f

        // Home section
        drawArc(
            color = primaryColor,
            startAngle = startAngle,
            sweepAngle = homeSweepAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )

        startAngle += homeSweepAngle

        // Draw section
        drawArc(
            color = secondaryColor,
            startAngle = startAngle,
            sweepAngle = drawSweepAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )

        startAngle += drawSweepAngle

        // Away section
        drawArc(
            color = tertiaryColor,
            startAngle = startAngle,
            sweepAngle = awaySweepAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun TeamFormComparisonCard(comparison: Comparison, teams: Teams) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Team Form Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Team headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamHeader(
                    team = teams.home,
                    modifier = Modifier.weight(1f)
                )
                TeamHeader(
                    team = teams.away,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form comparison bars
            ComparisonMetric(
                title = "Form",
                homeValue = comparison.form.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                awayValue = comparison.form.away.removeSuffix("%").toFloatOrNull() ?: 0f
            )

            ComparisonMetric(
                title = "Attack",
                homeValue = comparison.att.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                awayValue = comparison.att.away.removeSuffix("%").toFloatOrNull() ?: 0f
            )

            ComparisonMetric(
                title = "Defense",
                homeValue = comparison.def.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                awayValue = comparison.def.away.removeSuffix("%").toFloatOrNull() ?: 0f
            )

            ComparisonMetric(
                title = "H2H",
                homeValue = comparison.h2h.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                awayValue = comparison.h2h.away.removeSuffix("%").toFloatOrNull() ?: 0f
            )

            ComparisonMetric(
                title = "Overall",
                homeValue = comparison.total.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                awayValue = comparison.total.away.removeSuffix("%").toFloatOrNull() ?: 0f,
                isHighlighted = true
            )
        }
    }
}

@Composable
fun TeamHeader(team: Team, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = team.logo),
            contentDescription = "${team.name} logo",
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = team.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComparisonMetric(
    title: String,
    homeValue: Float,
    awayValue: Float,
    isHighlighted: Boolean = false
) {
    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(backgroundColor, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home value
            Text(
                text = "${homeValue.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(36.dp)
            )

            // Home progress bar
            LinearProgressIndicator(
                progress = { homeValue / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Away progress bar
            LinearProgressIndicator(
                progress = { awayValue / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Away value
            Text(
                text = "${awayValue.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .width(36.dp)
                    .padding(start = 4.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview
@Composable
private fun GoalsAnalysisCardPreview() {
    GoalsAnalysisCard(
        teams = Teams(
            home = Team(
                1,
                "Arsenal",
                "https://media.api-sports.io/football/teams/57.png",
                last_5 = Last5(
                    "60%",
                    "70",
                    "0%",
                    goals = Last5Goals(`for` = GoalData(18, 3.6), against = GoalData(10, 2.0))
                ),
                league = TeamLeague(
                    form = "60%",
                    fixtures = Fixtures(
                        played = FixtureDetail(1, 1, 1),
                        wins = FixtureDetail(1, 1, 1),
                        draws = FixtureDetail(1, 1, 1),
                        loses = FixtureDetail(1, 1, 1),
                    ),
                    goals = TeamGoals(
                        `for` = GoalStats(
                            total = GoalTotal(1, 1, 1),
                            average = GoalAverage(1.0, 1.0, 1.0)
                        ),
                        against = GoalStats(
                            total = GoalTotal(1, 1, 1),
                            average = GoalAverage(1.0, 1.0, 1.0)
                        )
                    ),
                    biggest = Biggest(
                        streak = Streak(1, 1, 1),
                        wins = WinLoss("1", "1"),
                        loses = WinLoss("1", "1"),
                        goals = BiggestGoals(
                            `for` = HomeAway(1, 1), against = HomeAway(1, 1)
                        )
                    ),
                    clean_sheet = CleanSheet(23, 23, 46),
                    failed_to_score = FailedToScore(9, 9, 18)
                )
            ),
            away = Team(
                2, "Chelsea", "https://media.api-sports.io/football/teams/61.png",
                last_5 = Last5(
                    "40%",
                    "50",
                    "0%",
                    goals = Last5Goals(`for` = GoalData(15, 3.0), against = GoalData(5, 1.0))
                ),
                league = TeamLeague(
                    form = "40%",
                    fixtures = Fixtures(
                        played = FixtureDetail(1, 1, 1),
                        wins = FixtureDetail(1, 1, 1),
                        draws = FixtureDetail(1, 1, 1),
                        loses = FixtureDetail(1, 1, 1),
                    ),
                    goals = TeamGoals(
                        `for` = GoalStats(
                            total = GoalTotal(1, 1, 1),
                            average = GoalAverage(1.0, 1.0, 1.0)
                        ),
                        against = GoalStats(
                            total = GoalTotal(1, 1, 1),
                            average = GoalAverage(1.0, 1.0, 1.0)
                        )
                    ),
                    biggest = Biggest(
                        streak = Streak(1, 1, 1),
                        wins = WinLoss("1", "1"),
                        loses = WinLoss("1", "1"),
                        goals = BiggestGoals(
                            `for` = HomeAway(1, 1), against = HomeAway(1, 1)
                        )
                    ),
                    clean_sheet = CleanSheet(23, 23, 46),
                    failed_to_score = FailedToScore(9, 9, 18)
                )
            )
        )
    )

}