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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.prediction.Comparison
import com.soccertips.predictx.data.model.prediction.GoalData
import com.soccertips.predictx.data.model.prediction.H2H
import com.soccertips.predictx.data.model.prediction.Predictions
import com.soccertips.predictx.data.model.prediction.Team
import com.soccertips.predictx.data.model.prediction.Teams
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PredictionCarousel(
        predictions: Predictions,
        comparison: Comparison,
        teams: Teams,
        h2h: List<H2H>,
) {
        val pagerState = rememberPagerState(pageCount = { 4 })
        val cardHeight = 520.dp // Increased height for better content fit

        Column {

                // Horizontal pager for cards
                HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
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
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(4) { page ->
                                Box(
                                        Modifier.size(if (pagerState.currentPage == page) 20.dp else 14.dp)
                                                .padding(4.dp)
                                                .background(
                                                        if (pagerState.currentPage == page)
                                                                MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.secondary,
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
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                stringResource(R.string.goals_analysis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Team headers with logos
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Home team
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                                painter = rememberAsyncImagePainter(model = teams.home.logo),
                                                contentDescription = teams.home.name,
                                                modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                                text = teams.home.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(90.dp),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }

                                Text(
                                        text = "VS",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                )

                                // Away team
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                                painter = rememberAsyncImagePainter(model = teams.away.logo),
                                                contentDescription = teams.away.name,
                                                modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                                text = teams.away.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(90.dp),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.tertiary
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Last 5 matches stats
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                        ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        stringResource(R.string.last_5_matches_goals),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Goals For - improved visualization
                                        GoalComparisonRow(
                                                title = stringResource(R.string.goals_for),
                                                homeValue = teams.home.last_5.goals.`for`.total,
                                                homeAvg = teams.home.last_5.goals.`for`.average,
                                                awayValue = teams.away.last_5.goals.`for`.total,
                                                awayAvg = teams.away.last_5.goals.`for`.average,
                                                homeColor = MaterialTheme.colorScheme.primary,
                                                awayColor = MaterialTheme.colorScheme.tertiary
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Goals Against - improved visualization
                                        GoalComparisonRow(
                                                title = stringResource(R.string.goals_against),
                                                homeValue = teams.home.last_5.goals.against.total,
                                                homeAvg = teams.home.last_5.goals.against.average,
                                                awayValue = teams.away.last_5.goals.against.total,
                                                awayAvg = teams.away.last_5.goals.against.average,
                                                homeColor = Color(0xFFE57373),
                                                awayColor = Color(0xFFEF9A9A),
                                                invertBetter = true
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Season stats card
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                        ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                        ) {
                                                Text(
                                                        stringResource(R.string.season_performance),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Season stats in a cleaner grid
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                // Home team stats
                                                Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.scored),
                                                                value = teams.home.league.goals.`for`.total.total.toString(),
                                                                avg = String.format(Locale.getDefault(), "%.1f", teams.home.league.goals.`for`.average.total),
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.conceded),
                                                                value = teams.home.league.goals.against.total.total.toString(),
                                                                avg = String.format(Locale.getDefault(), "%.1f", teams.home.league.goals.against.average.total),
                                                                color = Color(0xFFE57373)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.clean_sheets),
                                                                value = teams.home.league.clean_sheet.total.toString(),
                                                                avg = null,
                                                                color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.failed_to_score),
                                                                value = teams.home.league.failed_to_score.total.toString(),
                                                                avg = null,
                                                                color = MaterialTheme.colorScheme.outline
                                                        )
                                                }

                                                // Divider
                                                Box(
                                                        modifier = Modifier
                                                                .width(1.dp)
                                                                .height(140.dp)
                                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                )

                                                // Away team stats
                                                Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.scored),
                                                                value = teams.away.league.goals.`for`.total.total.toString(),
                                                                avg = String.format(Locale.getDefault(), "%.1f", teams.away.league.goals.`for`.average.total),
                                                                color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.conceded),
                                                                value = teams.away.league.goals.against.total.total.toString(),
                                                                avg = String.format(Locale.getDefault(), "%.1f", teams.away.league.goals.against.average.total),
                                                                color = Color(0xFFE57373)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.clean_sheets),
                                                                value = teams.away.league.clean_sheet.total.toString(),
                                                                avg = null,
                                                                color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        SeasonStatItem(
                                                                label = stringResource(R.string.failed_to_score),
                                                                value = teams.away.league.failed_to_score.total.toString(),
                                                                avg = null,
                                                                color = MaterialTheme.colorScheme.outline
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun GoalComparisonRow(
        title: String,
        homeValue: Int,
        homeAvg: Double,
        awayValue: Int,
        awayAvg: Double,
        homeColor: Color,
        awayColor: Color,
        invertBetter: Boolean = false
) {
        val maxValue = maxOf(homeValue, awayValue).toFloat().coerceAtLeast(1f)
        val homeBetter = if (invertBetter) homeValue < awayValue else homeValue > awayValue
        val awayBetter = if (invertBetter) awayValue < homeValue else awayValue > homeValue

        Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Home value
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(50.dp)
                        ) {
                                Text(
                                        text = homeValue.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (homeBetter) FontWeight.Bold else FontWeight.Normal,
                                        color = if (homeBetter) homeColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = String.format(Locale.getDefault(), "%.1f/g", homeAvg),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                )
                        }

                        // Progress bars
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                // Home bar
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(homeColor.copy(alpha = 0.2f))
                                ) {
                                        Box(
                                                modifier = Modifier
                                                        .fillMaxWidth(homeValue / maxValue)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(homeColor)
                                        )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Away bar
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(awayColor.copy(alpha = 0.2f))
                                ) {
                                        Box(
                                                modifier = Modifier
                                                        .fillMaxWidth(awayValue / maxValue)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(awayColor)
                                        )
                                }
                        }

                        // Away value
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(50.dp)
                        ) {
                                Text(
                                        text = awayValue.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (awayBetter) FontWeight.Bold else FontWeight.Normal,
                                        color = if (awayBetter) awayColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = String.format(Locale.getDefault(), "%.1f/g", awayAvg),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                )
                        }
                }
        }
}

@Composable
fun SeasonStatItem(
        label: String,
        value: String,
        avg: String?,
        color: Color
) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                        )
                        if (avg != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                        text = "($avg)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                )
                        }
                }
        }
}

@Composable
fun HeadToHeadCard(h2h: List<H2H>) {
        OutlinedCard(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                stringResource(R.string.head_to_head_history),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (h2h.isEmpty()) {
                                Text(
                                        stringResource(R.string.no_head_to_head_history_available),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 24.dp)
                                )
                        } else {
                                // H2H Summary
                                val homeTeam = h2h.first().teams.home.name
                                val awayTeam = h2h.first().teams.away.name
                                val homeTeamLogo = h2h.first().teams.home.logo
                                val awayTeamLogo = h2h.first().teams.away.logo

                                val homeWins = h2h.count { it.teams.home.winner == true }
                                val awayWins = h2h.count { it.teams.away.winner == true }
                                val draws =
                                        h2h.count { it.teams.home.winner == null && it.teams.away.winner == null }

                                // Team logos and summary header
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Home team summary
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Image(
                                                        painter = rememberAsyncImagePainter(model = homeTeamLogo),
                                                        contentDescription = homeTeam,
                                                        modifier = Modifier.size(36.dp)
                                                )
                                                Text(
                                                        text = homeTeam,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(80.dp),
                                                        textAlign = TextAlign.Center
                                                )
                                        }

                                        // VS indicator
                                        Text(
                                                text = "VS",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.outline
                                        )

                                        // Away team summary
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Image(
                                                        painter = rememberAsyncImagePainter(model = awayTeamLogo),
                                                        contentDescription = awayTeam,
                                                        modifier = Modifier.size(36.dp)
                                                )
                                                Text(
                                                        text = awayTeam,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(80.dp),
                                                        textAlign = TextAlign.Center
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Simple win-draw-loss chart
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(30.dp)
                                                        .padding(vertical = 4.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                        val total = h2h.size.toFloat()

                                        val homeWeight = maxOf(0.01f, homeWins / total)
                                        val drawWeight = maxOf(0.01f, draws / total)
                                        val awayWeight = maxOf(0.01f, awayWins / total)

                                        Box(
                                                modifier =
                                                        Modifier.weight(homeWeight)
                                                                .fillMaxHeight()
                                                                .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                if (homeWins > 0) {
                                                        Text(
                                                                text = "$homeWins",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }

                                        Box(
                                                modifier =
                                                        Modifier.weight(drawWeight)
                                                                .fillMaxHeight()
                                                                .background(MaterialTheme.colorScheme.secondary),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                if (draws > 0) {
                                                        Text(
                                                                text = "$draws",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }

                                        Box(
                                                modifier =
                                                        Modifier.weight(awayWeight)
                                                                .fillMaxHeight()
                                                                .background(MaterialTheme.colorScheme.tertiary),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                if (awayWins > 0) {
                                                        Text(
                                                                text = "$awayWins",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onTertiary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }

                                // Legend row
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                        modifier = Modifier
                                                                .size(10.dp)
                                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = homeTeam.take(10),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                        modifier = Modifier
                                                                .size(10.dp)
                                                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = stringResource(R.string.draws_format, draws).substringBefore(":"),
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                        modifier = Modifier
                                                                .size(10.dp)
                                                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = awayTeam.take(10),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Recent matches list
                                Text(
                                        stringResource(R.string.last_5_matches),
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

                                                val homeWon = match.teams.home.winner == true
                                                val awayWon = match.teams.away.winner == true
                                                val isDraw = match.teams.home.winner == null && match.teams.away.winner == null

                                                Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(
                                                                containerColor = when {
                                                                        isDraw -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                }
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                                // Date and competition
                                                                Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                        Text(
                                                                                text = formattedDate,
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = MaterialTheme.colorScheme.outline
                                                                        )

                                                                        Text(
                                                                                text = match.league.name,
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = MaterialTheme.colorScheme.outline,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis,
                                                                                modifier = Modifier.weight(1f),
                                                                                textAlign = TextAlign.End
                                                                        )
                                                                }

                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                // Match result with team logos
                                                                Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                        // Home team with logo
                                                                        Row(
                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                modifier = Modifier.weight(2f)
                                                                        ) {
                                                                                Image(
                                                                                        painter = rememberAsyncImagePainter(model = match.teams.home.logo),
                                                                                        contentDescription = match.teams.home.name,
                                                                                        modifier = Modifier.size(24.dp)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                                Text(
                                                                                        text = match.teams.home.name,
                                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                                        fontWeight = if (homeWon) FontWeight.Bold else FontWeight.Normal,
                                                                                        color = if (homeWon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                                        maxLines = 1,
                                                                                        overflow = TextOverflow.Ellipsis
                                                                                )
                                                                        }

                                                                        // Score with highlight
                                                                        Box(
                                                                                modifier = Modifier
                                                                                        .background(
                                                                                                when {
                                                                                                        isDraw -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                                                                        else -> MaterialTheme.colorScheme.primaryContainer
                                                                                                },
                                                                                                RoundedCornerShape(8.dp)
                                                                                        )
                                                                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                                                        ) {
                                                                                Text(
                                                                                        text = "${match.goals.home} - ${match.goals.away}",
                                                                                        style = MaterialTheme.typography.titleSmall,
                                                                                        fontWeight = FontWeight.Bold,
                                                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                                )
                                                                        }

                                                                        // Away team with logo
                                                                        Row(
                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                horizontalArrangement = Arrangement.End,
                                                                                modifier = Modifier.weight(2f)
                                                                        ) {
                                                                                Text(
                                                                                        text = match.teams.away.name,
                                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                                        fontWeight = if (awayWon) FontWeight.Bold else FontWeight.Normal,
                                                                                        color = if (awayWon) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                                                                        maxLines = 1,
                                                                                        overflow = TextOverflow.Ellipsis,
                                                                                        textAlign = TextAlign.End
                                                                                )
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                                Image(
                                                                                        painter = rememberAsyncImagePainter(model = match.teams.away.logo),
                                                                                        contentDescription = match.teams.away.name,
                                                                                        modifier = Modifier.size(24.dp)
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
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                stringResource(R.string.match_prediction_overview),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Pie Chart Visual Representation using Canvas
                        PieChartVisual(
                                homePercent = homePercent,
                                drawPercent = drawPercent,
                                awayPercent = awayPercent,
                                modifier = Modifier.size(200.dp).padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legend
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                                LegendItem(
                                        color = MaterialTheme.colorScheme.primary,
                                        text = stringResource(R.string.home__, predictions.percent.home)
                                )
                                LegendItem(
                                        color = MaterialTheme.colorScheme.secondary,
                                        text = stringResource(R.string.draw, predictions.percent.draw)
                                )
                                LegendItem(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        text = stringResource(R.string.away__, predictions.percent.away)
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                                text = predictions.advice,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                        )

                        if (predictions.win_or_draw) {
                                Text(
                                        text = stringResource(R.string.win_or_draw_likely),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }

                        predictions.under_over?.let {
                                Text(
                                        text = stringResource(R.string.goals_under_over_format, it),
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
                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
}

@Composable
fun TeamFormComparisonCard(comparison: Comparison, teams: Teams) {
        OutlinedCard(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                stringResource(R.string.team_form_comparison),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Team headers
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                TeamHeader(team = teams.home, modifier = Modifier.weight(1f))
                                TeamHeader(team = teams.away, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Form comparison bars
                        ComparisonMetric(
                                title = stringResource(R.string.form),
                                homeValue = comparison.form.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                                awayValue = comparison.form.away.removeSuffix("%").toFloatOrNull() ?: 0f
                        )

                        ComparisonMetric(
                                title = stringResource(R.string.attack),
                                homeValue = comparison.att.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                                awayValue = comparison.att.away.removeSuffix("%").toFloatOrNull() ?: 0f
                        )

                        ComparisonMetric(
                                title = stringResource(R.string.defense),
                                homeValue = comparison.def.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                                awayValue = comparison.def.away.removeSuffix("%").toFloatOrNull() ?: 0f
                        )

                        ComparisonMetric(
                                title = stringResource(R.string.h2h),
                                homeValue = comparison.h2h.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                                awayValue = comparison.h2h.away.removeSuffix("%").toFloatOrNull() ?: 0f
                        )

                        ComparisonMetric(
                                title = stringResource(R.string.overall),
                                homeValue = comparison.total.home.removeSuffix("%").toFloatOrNull() ?: 0f,
                                awayValue = comparison.total.away.removeSuffix("%").toFloatOrNull() ?: 0f,
                                isHighlighted = true
                        )
                }
        }
}

@Composable
fun TeamHeader(team: Team, modifier: Modifier = Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                        painter = rememberAsyncImagePainter(model = team.logo),
                        contentDescription = stringResource(R.string.team_logo_format, team.name),
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
        val backgroundColor =
                if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface

        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                        backgroundColor,
                                        androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
        ) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Home value
                        Text(
                                text = "${homeValue.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(36.dp)
                        )

                        // Home progress bar
                        LinearProgressIndicator(
                                progress = { homeValue / 100f },
                                modifier =
                                        Modifier.weight(1f)
                                                .height(8.dp)
                                                .clip(
                                                        androidx.compose.foundation.shape.RoundedCornerShape(
                                                                4.dp
                                                        )
                                                ),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Away progress bar
                        LinearProgressIndicator(
                                progress = { awayValue / 100f },
                                modifier =
                                        Modifier.weight(1f)
                                                .height(8.dp)
                                                .clip(
                                                        androidx.compose.foundation.shape.RoundedCornerShape(
                                                                4.dp
                                                        )
                                                ),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Away value
                        Text(
                                text = "${awayValue.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(36.dp).padding(start = 4.dp),
                                textAlign = TextAlign.End
                        )
                }
        }
}
