package com.soccertips.predictx.ui.team


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.data.model.team.teamscreen.TeamStatistics
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import timber.log.Timber


@Composable
fun TeamStatisticsContent(
    statistics: TeamStatistics,
    lazyListState: LazyListState,
) {

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .onGloballyPositioned { layoutCoordinates ->
                Timber.d("TeamStatisticsContent: LazyColumn Size: ${layoutCoordinates.size}")
            }
    ) {
        Timber.d("TeamStatisticsContent: statistics: $statistics")
        // Form Card
        item {
            StatisticCard(title = "Form") {
                Row(horizontalArrangement = Arrangement.Center) {
                    statistics.form.forEach { letter ->
                        val (color, background) = when (letter) {
                            'W' -> Color.Green to Color.Green.copy(alpha = 0.2f)
                            'D' -> Color.Gray to Color.Gray.copy(alpha = 0.2f)
                            'L' -> Color.Red to Color.Red.copy(alpha = 0.2f)
                            else -> Color.Gray to Color.Gray.copy(alpha = 0.2f)
                        }
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    color = background
                                ),
                            textAlign = TextAlign.Center,
                            maxLines = 2

                        )
                    }
                }
            }
        }
        // Fixtures Card
        item {
            ExpandableStatisticCard(title = "Fixtures") {
                TableComposable(
                    headers = listOf("Category", "Home", "Away", "Total"),
                    rows = listOf(
                        listOf(
                            "Played",
                            "${statistics.fixtures.played.home}",
                            "${statistics.fixtures.played.away}",
                            "${statistics.fixtures.played.total}"
                        ),
                        listOf(
                            "Wins",
                            "${statistics.fixtures.wins.home}",
                            "${statistics.fixtures.wins.away}",
                            "${statistics.fixtures.wins.total}"
                        ),
                        listOf(
                            "Draws",
                            "${statistics.fixtures.draws.home}",
                            "${statistics.fixtures.draws.away}",
                            "${statistics.fixtures.draws.total}"
                        ),
                        listOf(
                            "Losses",
                            "${statistics.fixtures.loses.home}",
                            "${statistics.fixtures.loses.away}",
                            "${statistics.fixtures.loses.total}"
                        )
                    )
                )
            }
        }
        // Goals Card
        item {
            ExpandableStatisticCard(title = "Goals") {
                TableComposable(
                    headers = listOf("Type", "Home", "Away", "Total"),
                    rows = listOf(
                        listOf(
                            "For",
                            "${statistics.goals.`for`.total.home}",
                            "${statistics.goals.`for`.total.away}",
                            "${statistics.goals.`for`.total.total}"
                        ),
                        listOf(
                            "Against",
                            "${statistics.goals.against.total.home}",
                            "${statistics.goals.against.total.away}",
                            "${statistics.goals.against.total.total}"
                        )
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Goals For Minute table
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.goals.`for`.minute.map { (minute, goalMinute) ->
                        listOf(minute, "${goalMinute.total ?: "-"}", goalMinute.percentage ?: "N/A")
                    }
                )

            }
        }
        // Penalties Card
        item {
            ExpandableStatisticCard(title = "Penalties") {
                Text(text = "Scored: ${statistics.penalty.scored.total} (${statistics.penalty.scored.percentage})")
                LinearProgressIndicator(
                    progress = { statistics.penalty.scored.total / statistics.penalty.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Missed: ${statistics.penalty.missed.total} (${statistics.penalty.missed.percentage})")
                LinearProgressIndicator(
                    progress = { statistics.penalty.missed.total / statistics.penalty.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Most Used Formation Card
        item {
            ExpandableStatisticCard(title = "Most Used Formation: ${statistics.lineups.maxByOrNull { it.played }?.formation}") {
                statistics.lineups.forEach { lineup ->
                    Text(
                        text = "${lineup.formation}: (${lineup.played} times)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        // Cards Card
        item {
            ExpandableStatisticCard(title = "Cards") {
                // Yellow Cards table
                Row {
                    Text(text = "Yellow Cards")
                    Icon(
                        Icons.Default.Rectangle,
                        contentDescription = "Yellow Cards",
                        tint = Color.Yellow
                    )
                }
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.cards.yellow.map { (minute, cardDetail) ->
                        listOf(minute, "${cardDetail.total ?: "-"}", cardDetail.percentage ?: "N/A")
                    }
                )
                // Red Cards table
                Row {
                    Text(text = "Red Cards")
                    Icon(
                        Icons.Default.Rectangle,
                        contentDescription = "Red Cards",
                        tint = Color.Red
                    )
                }
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.cards.red.map { (minute, cardDetail) ->
                        listOf(minute, "${cardDetail.total ?: "-"}", cardDetail.percentage ?: "N/A")
                    }
                )
            }
        }
        // Biggest Streaks Card
        item {
            ExpandableStatisticCard(title = "Biggest Streaks") {
                Text(text = "Wins: ${statistics.biggest.streak.wins}")
                Text(text = "Draws: ${statistics.biggest.streak.draws}")
                Text(text = "Loses: ${statistics.biggest.streak.loses}")
            }
            ExpandableStatisticCard(title = "Biggest Wins") {
                Text(text = "Home: ${statistics.biggest.wins.home}")
                Text(text = "Away: ${statistics.biggest.wins.away}")
            }
            ExpandableStatisticCard(title = "Biggest Losses") {
                Text(text = "Home: ${statistics.biggest.loses.home}")
                Text(text = "Away: ${statistics.biggest.loses.away}")
            }
            ExpandableStatisticCard(title = "Most Goals For") {
                Text(text = "Home: ${statistics.biggest.goals.`for`.home}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.`for`.home.toFloat() / statistics.goals.`for`.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Away: ${statistics.biggest.goals.`for`.away}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.`for`.away.toFloat() / statistics.goals.`for`.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ExpandableStatisticCard(title = "Most Goals Against") {
                Text(text = "Home: ${statistics.biggest.goals.against.home}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.against.home.toFloat() / statistics.goals.against.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Away: ${statistics.biggest.goals.against.away}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.against.away.toFloat() / statistics.goals.against.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Clean Sheets Card
        item {
            ExpandableStatisticCard(title = "Clean Sheets") {
                Text(text = "Home: ${statistics.clean_sheet.home}")
                Text(text = "Away: ${statistics.clean_sheet.away}")
                Text(text = "Total: ${statistics.clean_sheet.total}")
            }
        }
        // Failed to Score Card
        item {
            ExpandableStatisticCard(title = "Failed to Score") {
                Text(text = "Home: ${statistics.failed_to_score.home}")
                Text(text = "Away: ${statistics.failed_to_score.away}")
                Text(text = "Total: ${statistics.failed_to_score.total}")
            }
        }
    }
}


@Composable
fun StatisticCard(title: String, content: @Composable () -> Unit) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun ExpandableStatisticCard(title: String, content: @Composable () -> Unit) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val expanded = remember { mutableStateOf(false) }

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { expanded.value = !expanded.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded.value) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                    animationSpec = tween(
                        300
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TableComposable(headers: List<String>, rows: List<List<String>>) {
    Table(headers = headers, rows = rows.map { row -> row.map { it ?: "-" } })
}


/*@Composable
fun TeamStatisticsContent(
    statistics: TeamStatistics,
    lazyListState: LazyListState,
) {

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .onGloballyPositioned { layoutCoordinates ->
                Timber.d("TeamStatisticsContent: LazyColumn Size: ${layoutCoordinates.size}")
            }
    ) {
        Timber.d("TeamStatisticsContent: statistics: $statistics")
        // Form Card
        item {
            StatisticCard(title = "Form") {
                Row(horizontalArrangement = Arrangement.Center) {
                    statistics.form.forEach { letter ->
                        val (color, background) = when (letter) {
                            'W' -> Color.Green to Color.Green.copy(alpha = 0.2f)
                            'D' -> Color.Gray to Color.Gray.copy(alpha = 0.2f)
                            'L' -> Color.Red to Color.Red.copy(alpha = 0.2f)
                            else -> Color.Gray to Color.Gray.copy(alpha = 0.2f)
                        }
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    color = background
                                ),
                            textAlign = TextAlign.Center,
                            maxLines = 2

                        )
                    }
                }
            }
        }
        // Fixtures Card
        item {
            StatisticCard(title = "Fixtures") {
                TableComposable(
                    headers = listOf("Category", "Home", "Away", "Total"),
                    rows = listOf(
                        listOf(
                            "Played",
                            "${statistics.fixtures.played.home}",
                            "${statistics.fixtures.played.away}",
                            "${statistics.fixtures.played.total}"
                        ),
                        listOf(
                            "Wins",
                            "${statistics.fixtures.wins.home}",
                            "${statistics.fixtures.wins.away}",
                            "${statistics.fixtures.wins.total}"
                        ),
                        listOf(
                            "Draws",
                            "${statistics.fixtures.draws.home}",
                            "${statistics.fixtures.draws.away}",
                            "${statistics.fixtures.draws.total}"
                        ),
                        listOf(
                            "Losses",
                            "${statistics.fixtures.loses.home}",
                            "${statistics.fixtures.loses.away}",
                            "${statistics.fixtures.loses.total}"
                        )
                    )
                )
            }
        }
        // Goals Card
        item {
            StatisticCard(title = "Goals") {
                TableComposable(
                    headers = listOf("Type", "Home", "Away", "Total"),
                    rows = listOf(
                        listOf(
                            "For",
                            "${statistics.goals.`for`.total.home}",
                            "${statistics.goals.`for`.total.away}",
                            "${statistics.goals.`for`.total.total}"
                        ),
                        listOf(
                            "Against",
                            "${statistics.goals.against.total.home}",
                            "${statistics.goals.against.total.away}",
                            "${statistics.goals.against.total.total}"
                        )
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Goals For Minute table
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.goals.`for`.minute.map { (minute, goalMinute) ->
                        listOf(minute, "${goalMinute.total ?: "-"}", goalMinute.percentage ?: "N/A")
                    }
                )

            }
        }
        // Penalties Card
        item {
            StatisticCard(title = "Penalties") {
                Text(text = "Scored: ${statistics.penalty.scored.total} (${statistics.penalty.scored.percentage})")
                LinearProgressIndicator(
                    progress = { statistics.penalty.scored.total / statistics.penalty.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Missed: ${statistics.penalty.missed.total} (${statistics.penalty.missed.percentage})")
                LinearProgressIndicator(
                    progress = { statistics.penalty.missed.total / statistics.penalty.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Most Used Formation Card
        item {
            StatisticCard(title = "Most Used Formation: ${statistics.lineups.maxByOrNull { it.played }?.formation}") {
                statistics.lineups.forEach { lineup ->
                    Text(
                        text = "${lineup.formation}: (${lineup.played} times)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        // Cards Card
        item {
            StatisticCard(title = "Cards") {
                // Yellow Cards table
                Row {
                    Text(text = "Yellow Cards")
                    Icon(
                        Icons.Default.Rectangle,
                        contentDescription = "Yellow Cards",
                        tint = Color.Yellow
                    )
                }
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.cards.yellow.map { (minute, cardDetail) ->
                        listOf(minute, "${cardDetail.total ?: "-"}", cardDetail.percentage ?: "N/A")
                    }
                )
                // Red Cards table
                Row {
                    Text(text = "Red Cards")
                    Icon(
                        Icons.Default.Rectangle,
                        contentDescription = "Red Cards",
                        tint = Color.Red
                    )
                }
                TableComposable(
                    headers = listOf("Minute", "Total", "Percentage"),
                    rows = statistics.cards.red.map { (minute, cardDetail) ->
                        listOf(minute, "${cardDetail.total ?: "-"}", cardDetail.percentage ?: "N/A")
                    }
                )
            }
        }
        // Biggest Streaks Card
        item {
            StatisticCard(title = "Biggest Streaks") {
                Text(text = "Wins: ${statistics.biggest.streak.wins}")
                Text(text = "Draws: ${statistics.biggest.streak.draws}")
                Text(text = "Loses: ${statistics.biggest.streak.loses}")
            }
            StatisticCard(title = "Biggest Wins") {
                Text(text = "Home: ${statistics.biggest.wins.home}")
                Text(text = "Away: ${statistics.biggest.wins.away}")
            }
            StatisticCard(title = "Biggest Losses") {
                Text(text = "Home: ${statistics.biggest.loses.home}")
                Text(text = "Away: ${statistics.biggest.loses.away}")
            }
            StatisticCard(title = "Most Goals For") {
                Text(text = "Home: ${statistics.biggest.goals.`for`.home}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.`for`.home.toFloat() / statistics.goals.`for`.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Away: ${statistics.biggest.goals.`for`.away}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.`for`.away.toFloat() / statistics.goals.`for`.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            StatisticCard(title = "Most Goals Against") {
                Text(text = "Home: ${statistics.biggest.goals.against.home}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.against.home.toFloat() / statistics.goals.against.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Away: ${statistics.biggest.goals.against.away}")
                LinearProgressIndicator(
                    progress = { statistics.biggest.goals.against.away.toFloat() / statistics.goals.against.total.total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Clean Sheets Card
        item {
            StatisticCard(title = "Clean Sheets") {
                Text(text = "Home: ${statistics.clean_sheet.home}")
                Text(text = "Away: ${statistics.clean_sheet.away}")
                Text(text = "Total: ${statistics.clean_sheet.total}")
            }
        }
        // Failed to Score Card
        item {
            StatisticCard(title = "Failed to Score") {
                Text(text = "Home: ${statistics.failed_to_score.home}")
                Text(text = "Away: ${statistics.failed_to_score.away}")
                Text(text = "Total: ${statistics.failed_to_score.total}")
            }
        }
    }
}


@Composable
fun StatisticCard(title: String, content: @Composable () -> Unit) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun TableComposable(headers: List<String>, rows: List<List<String>>) {
    Table(headers = headers, rows = rows.map { row -> row.map { it ?: "-" } })
}*/

/*
@RequiresApi(Build.VERSION_CODES.S)
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable

fun TeamStatisticsContentPreview() {

    val statistics = TeamStatistics(
        league = League(
            id = 567,
            name = "Ligi kuu Bara",
            country = "Tanzania",
            logo = "https://media.api-sports.io/football/leagues/567.png",
            flag = "https://media.api-sports.io/flags/tz.svg",
            season = 2024
        ),
        team = Team(
            id = 6432,
            name = "Simba",
            logo = "https://media.api-sports.io/football/teams/6432.png"
        ),
        form = "WWWWDLWWWWWWW",
        fixtures = Fixtures(
            played = Played(home = 7, away = 6, total = 13),
            wins = Played(home = 5, away = 6, total = 11),
            draws = Played(home = 1, away = 0, total = 1),
            loses = Played(home = 1, away = 0, total = 1)
        ),
        goals = Goals(
            `for` = ForAgainstGoals(
                total = Played(home = 18, away = 11, total = 29),
                average = AverageGoals(home = "2.6", away = "1.8", total = "2.2"),
                minute = mapOf(

                    "0 - 15" to GoalMinute(
                        total = 5,
                        percentage = "20.83 %"
                    ),
                    "16-30" to GoalMinute(total = 2, percentage = "8.33%"),
                    "31-45" to GoalMinute(total = 6, percentage = "25.00 %"),
                    "46-60" to GoalMinute(total = 2, percentage = "8.33 %"),
                    "61-75" to GoalMinute(total = 5, percentage = "20.83 %"),
                    "76-90" to GoalMinute(total = 2, percentage = "8.33 %"),
                    "91-105" to GoalMinute(total = 2, percentage = "8.33 %"),
                    "106-120" to GoalMinute(total = null, percentage = "null")
                ),

                under_over = mapOf(
                    "0.5" to UnderOver(
                        under = 1,
                        over = 12
                    ),
                    "1.5" to UnderOver(under = 5, over = 8),
                    "2.5" to UnderOver(under = 8, over = 5),
                    "3.5" to UnderOver(under = 10, over = 3),
                    "4.5" to UnderOver(under = 12, over = 1),

                    ),
            ),
            against = ForAgainstGoals(
                total = Played(home = 3, away = 2, total = 5),
                average = AverageGoals(home = "0.4", away = "0.3", total = "0.4"),
                minute = mapOf(
                    "0 - 15" to GoalMinute(
                        total = 1,
                        percentage = "20.00%"
                    ),
                    "16-30" to GoalMinute(total = 1, percentage = "20.00%"),
                    "31-45" to GoalMinute(total = 1, percentage = "20.00%"),
                    "46-60" to GoalMinute(total = 1, percentage = "20.00%"),
                    "61-75" to GoalMinute(total = 1, percentage = "20.00%"),
                    "76-90" to GoalMinute(total = 2, percentage = "40.00%"),
                    "91-105" to GoalMinute(total = 1, percentage = "20.00%"),
                    "106-120" to GoalMinute(total = null, percentage = "null")
                ),
                under_over = mapOf(
                    "0.5" to UnderOver(
                        under = 10,
                        over = 3
                    ),
                    "1.5" to UnderOver(under = 11, over = 2),
                    "2.5" to UnderOver(under = 13, over = 0),
                    "3.5" to UnderOver(under = 13, over = 0),
                    "4.5" to UnderOver(under = 13, over = 0)
                )
            )
        ),
        biggest = Biggest(
            streak = Streak(wins = 4, draws = 1, loses = 1),
            wins = HomeAway(home = "4 - 0", away = "2 - 5"),
            loses = HomeAway(home = "0 - 1", away = "null"),
            goals = HomeAwayGoals(
                `for` = HomeAway(home = "4", away = "5"),
                against = HomeAway(home = "2", away = "2")
            )
        ),
        clean_sheet = CleanSheet(home = 5, away = 5, total = 10),
        failed_to_score = FailedToScore(home = 1, away = 0, total = 1),
        penalty = Penalty(
            scored = PenaltyDetails(total = 1, percentage = "100.00 %"),
            missed = PenaltyDetails(total = 0, percentage = "0 %"),
            total = 1
        ),
        lineups = listOf(
            Lineup(formation = "4-3-3", played = 5),
            Lineup(formation = "4-4-2", played = 3)
        ),
        cards = Cards(
            yellow = mapOf(
                "0 - 15" to CardDetail(
                    total = null,
                    percentage = null
                ),
                "16-30" to CardDetail(total = null, percentage = null),
                "31-45" to CardDetail(total = null, percentage = null),
                "46-60" to CardDetail(total = null, percentage = null),
                "61-75" to CardDetail(total = null, percentage = null),
                "76-90" to CardDetail(total = null, percentage = null),
                "91-105" to CardDetail(total = null, percentage = null),
                "106-120" to CardDetail(total = null, percentage = null)
            ),


            red = mapOf(
                "0 - 15" to CardDetail(
                    total = null,
                    percentage = null
                ),
                "16-30" to CardDetail(total = null, percentage = null),
                "31-45" to CardDetail(total = null, percentage = null),
                "46-60" to CardDetail(total = null, percentage = null),
                "61-75" to CardDetail(total = null, percentage = null),
                "76-90" to CardDetail(total = null, percentage = null),
                "91-105" to CardDetail(total = null, percentage = null),
                "106-120" to CardDetail(total = null, percentage = null)
            )

        )
    )
    PredictXTheme {
        TeamStatisticsContent(
            statistics = statistics,
            lazyListState = rememberSaveable { LazyListState() }
        )
    }

}*/
