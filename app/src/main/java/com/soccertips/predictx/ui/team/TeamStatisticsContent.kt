package com.soccertips.predictx.ui.team


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.team.teamscreen.TeamStatistics
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import timber.log.Timber
import kotlin.math.roundToInt


@Composable
fun TeamStatisticsContent(
    statistics: TeamStatistics,
    lazyListState: LazyListState,
) {

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Timber.d("TeamStatisticsContent: statistics: $statistics")

        // Recent Form - Redesigned with horizontal scroll
        item {
            ModernFormCard(form = statistics.form)
        }

        // Key Statistics Overview Cards
        item {
            KeyStatsGrid(statistics = statistics)
        }

        // Performance Summary Card
        item {
            PerformanceSummaryCard(statistics = statistics)
        }

        // Goal Statistics with Visual Progress
        item {
            GoalStatisticsCard(statistics = statistics)
        }

        // Goals Timeline
        item {
            GoalsTimelineCard(statistics = statistics)
        }

        // Discipline Card
        item {
            DisciplineCard(statistics = statistics)
        }

        // Formation & Tactics
        item {
            FormationCard(statistics = statistics)
        }

        // Streaks & Records
        item {
            StreaksCard(statistics = statistics)
        }

        // Biggest Wins & Losses
        item {
            BiggestResultsCard(statistics = statistics)
        }

        // Penalty Statistics
        item {
            PenaltyStatisticsCard(statistics = statistics)
        }

        // Clean Sheets & Scoring
        item {
            DefensiveStatsCard(statistics = statistics)
        }
    }
}


@Composable
fun ModernFormCard(form: String?) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.form),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                form?.reversed()?.forEachIndexed { index, letter ->
                    item {
                        val (icon, color, background) = when (letter) {
                            'W' -> Triple(
                                Icons.Default.Check,
                                Color(0xFF4CAF50),
                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                            )

                            'D' -> Triple(
                                Icons.Default.Remove,
                                Color(0xFFFF9800),
                                Color(0xFFFF9800).copy(alpha = 0.15f)
                            )

                            'L' -> Triple(
                                Icons.Default.Close,
                                Color(0xFFF44336),
                                Color(0xFFF44336).copy(alpha = 0.15f)
                            )

                            else -> Triple(
                                Icons.Default.Remove,
                                Color.Gray,
                                Color.Gray.copy(alpha = 0.15f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(background)
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (letter) {
                                    'W' -> "Win"
                                    'D' -> "Draw"
                                    'L' -> "Loss"
                                    else -> "-"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyStatsGrid(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Win Rate
        val winRate = if (statistics.fixtures.played.total > 0) {
            (statistics.fixtures.wins.total.toFloat() / statistics.fixtures.played.total * 100).roundToInt()
        } else 0

        Card(
            colors = cardColors,
            elevation = cardElevation,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$winRate%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Win Rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Goals Per Game
        Card(
            colors = cardColors,
            elevation = cardElevation,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statistics.goals.`for`.average.total,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Goals/Game",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PerformanceSummaryCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Wins
            PerformanceRow(
                label = stringResource(R.string.wins),
                home = statistics.fixtures.wins.home,
                away = statistics.fixtures.wins.away,
                total = statistics.fixtures.wins.total,
                played = statistics.fixtures.played.total,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Draws
            PerformanceRow(
                label = stringResource(R.string.draws),
                home = statistics.fixtures.draws.home,
                away = statistics.fixtures.draws.away,
                total = statistics.fixtures.draws.total,
                played = statistics.fixtures.played.total,
                color = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Losses
            PerformanceRow(
                label = stringResource(R.string.losses),
                home = statistics.fixtures.loses.home,
                away = statistics.fixtures.loses.away,
                total = statistics.fixtures.loses.total,
                played = statistics.fixtures.played.total,
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun PerformanceRow(
    label: String,
    home: Int,
    away: Int,
    total: Int,
    played: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatBox("H", home.toString(), color.copy(alpha = 0.7f))
                StatBox("A", away.toString(), color.copy(alpha = 0.7f))
                StatBox("T", total.toString(), color)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val progress = if (played > 0) total.toFloat() / played else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GoalStatisticsCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.goals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Goals For
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Goals Scored",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${statistics.goals.`for`.total.total}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("Home", "${statistics.goals.`for`.total.home}")
                    MiniStat("Away", "${statistics.goals.`for`.total.away}")
                    MiniStat("Avg", statistics.goals.`for`.average.total)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Goals Against
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF44336).copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Goals Conceded",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${statistics.goals.against.total.total}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("Home", "${statistics.goals.against.total.home}")
                    MiniStat("Away", "${statistics.goals.against.total.away}")
                    MiniStat("Avg", statistics.goals.against.average.total)
                }
            }
        }
    }
}

@Composable
fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun GoalsTimelineCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val expanded = remember { mutableStateOf(false) }

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goals Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val rotation by animateFloatAsState(
                    targetValue = if (expanded.value) 180f else 0f,
                    label = "rotation"
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Goals Scored by Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    statistics.goals.`for`.minute.forEach { (minute, goalMinute) ->
                        if (goalMinute.total != null && goalMinute.total > 0) {
                            val percentage =
                                goalMinute.percentage?.replace("%", "")?.toFloatOrNull()?.div(100)
                                    ?: 0f

                            TimelineBar(
                                period = minute,
                                total = goalMinute.total,
                                percentage = percentage,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Goals Conceded by Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )

                    statistics.goals.against.minute.forEach { (minute, goalMinute) ->
                        if (goalMinute.total != null && goalMinute.total > 0) {
                            val percentage =
                                goalMinute.percentage?.replace("%", "")?.toFloatOrNull()?.div(100)
                                    ?: 0f

                            TimelineBar(
                                period = minute,
                                total = goalMinute.total,
                                percentage = percentage,
                                color = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineBar(period: String, total: Int, percentage: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = period,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )

            if (percentage > 0.15f) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
            }
        }

        if (percentage <= 0.15f) {
            Text(
                text = "$total",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun DisciplineCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val expanded = remember { mutableStateOf(false) }

    val totalYellow = statistics.cards.yellow.values.sumOf { it.total ?: 0 }
    val totalRed = statistics.cards.red.values.sumOf { it.total ?: 0 }

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardBadge(count = totalYellow, color = Color(0xFFFFEB3B))
                    CardBadge(count = totalRed, color = Color(0xFFF44336))

                    val rotation by animateFloatAsState(
                        targetValue = if (expanded.value) 180f else 0f,
                        label = "rotation"
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Yellow Cards by Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    statistics.cards.yellow.forEach { (minute, cardDetail) ->
                        if (cardDetail.total != null && cardDetail.total > 0) {
                            val percentage =
                                cardDetail.percentage?.replace("%", "")?.toFloatOrNull()?.div(100)
                                    ?: 0f
                            TimelineBar(minute, cardDetail.total, percentage, Color(0xFFFFEB3B))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (totalRed > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Red Cards by Period",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        statistics.cards.red.forEach { (minute, cardDetail) ->
                            if (cardDetail.total != null && cardDetail.total > 0) {
                                val percentage =
                                    cardDetail.percentage?.replace("%", "")?.toFloatOrNull()
                                        ?.div(100) ?: 0f
                                TimelineBar(minute, cardDetail.total, percentage, Color(0xFFF44336))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardBadge(count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FormationCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.lineups),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            statistics.lineups.forEach { lineup ->
                val totalGames = statistics.fixtures.played.total
                val percentage = if (totalGames > 0) lineup.played.toFloat() / totalGames else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lineup.formation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${lineup.played} matches",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(percentage * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreaksCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Biggest Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakItem(
                    label = stringResource(R.string.wins),
                    value = statistics.biggest.streak.wins,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = Color(0xFF4CAF50)
                )
                StreakItem(
                    label = stringResource(R.string.draws),
                    value = statistics.biggest.streak.draws,
                    icon = Icons.Default.Remove,
                    color = Color(0xFFFF9800)
                )
                StreakItem(
                    label = stringResource(R.string.losses),
                    value = statistics.biggest.streak.loses,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun StreakItem(label: String, value: Int, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun BiggestResultsCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Biggest Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Biggest Wins
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Biggest Wins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultItem(
                        "Home",
                        statistics.biggest.wins.home ?: "-"
                    )
                    ResultItem(
                        "Away",
                        statistics.biggest.wins.away ?: "-"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Biggest Losses
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF44336).copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Biggest Losses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultItem(
                        "Home",
                        statistics.biggest.loses.home ?: "-"
                    )
                    ResultItem(
                        "Away",
                        statistics.biggest.loses.away ?: "-"
                    )
                }
            }
        }
    }
}

@Composable
fun ResultItem(location: String, score: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = score,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = location,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PenaltyStatisticsCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.penalties),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val scoredProgress = if (statistics.penalty.total > 0) {
                statistics.penalty.scored.total.toFloat() / statistics.penalty.total
            } else 0f

            val missedProgress = if (statistics.penalty.total > 0) {
                statistics.penalty.missed.total.toFloat() / statistics.penalty.total
            } else 0f

            // Scored
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scored",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${statistics.penalty.scored.total} (${statistics.penalty.scored.percentage})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { scoredProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Missed
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Missed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${statistics.penalty.missed.total} (${statistics.penalty.missed.percentage})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { missedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF44336),
                    trackColor = Color(0xFFF44336).copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun DefensiveStatsCard(statistics: TeamStatistics) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Defensive & Attacking Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clean Sheets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${statistics.clean_sheet.total}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Clean Sheets",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "H: ${statistics.clean_sheet.home}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "A: ${statistics.clean_sheet.away}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Failed to Score
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5722).copy(alpha = 0.1f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${statistics.failed_to_score.total}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Failed to Score",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "H: ${statistics.failed_to_score.home}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "A: ${statistics.failed_to_score.away}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
