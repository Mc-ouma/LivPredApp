package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.InlineBannerAdView
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.data.model.Fixture
import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.data.model.prediction.Comparison
import com.soccertips.predictx.data.model.prediction.H2H
import com.soccertips.predictx.data.model.prediction.Predictions
import com.soccertips.predictx.data.model.prediction.Teams
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.components.AnimatedButton
import com.soccertips.predictx.ui.components.ErrorMessage
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.viewmodel.SharedViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import timber.log.Timber

// Extension function to safely find the Activity from any Context
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

@Composable
fun FixtureMatchDetailsScreen(
    fixtures: List<SharedViewModel.FixtureWithType>,
    predictions: Predictions?,
    comparison: Comparison,
    teams: Teams,
    h2h: List<H2H>,
    fixtureDetails: ResponseData,
    homeTeamId: String,
    awayTeamId: String,
    navController: NavController,
    rewardedAdManager: RewardedAdManager
) {
    // Safely convert team IDs to integers, defaulting to 0 if invalid or empty
    val homeTeamIdInt = homeTeamId.toIntOrNull() ?: 0
    val awayTeamIdInt = awayTeamId.toIntOrNull() ?: 0

    // Check for invalid team IDs
    if (homeTeamIdInt == 0 || awayTeamIdInt == 0) {
        ErrorMessage(message = stringResource(R.string.invalid_team_ids), onRetry = {})
        return
    }

    // Use regular Column instead of LazyColumn to avoid nested scrolling issues
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Fixture detail card
        FixtureDetailCard(fixture = fixtureDetails.fixture)
        Spacer(modifier = Modifier.height(16.dp))
        InlineBannerAdView()
        Spacer(modifier = Modifier.height(16.dp))

        // Home and away fixtures section
        FixtureListScreen(
            combinedFormData = fixtures,
            homeTeamIdInt = homeTeamIdInt,
            awayTeamIdInt = awayTeamIdInt,
            fixtureDetails = fixtureDetails,
            navController = navController
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Prediction card section
        if (predictions == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.no_predictions_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            val context = LocalContext.current
            val showPredictions = remember { androidx.compose.runtime.mutableStateOf(false) }
            val activityContext = remember { context.findActivity() }

            if (!showPredictions.value) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnimatedButton(
                        onClick = {
                            if (activityContext != null && rewardedAdManager?.isAdLoaded() == true) {
                                Timber.tag("RewardedAd").d("Button clicked - showing rewarded ad from activity: ${activityContext.javaClass.simpleName}")
                                rewardedAdManager.showRewardedAd(
                                    activity = activityContext,
                                    onRewardEarned = {
                                        showPredictions.value = true
                                        Timber.tag("RewardedAd").d("Reward earned callback executed")
                                    }
                                )
                            } else {
                                Timber.tag("RewardedAd").e("Cannot show ad: activity=${activityContext != null}, adLoaded=${rewardedAdManager?.isAdLoaded()}")
                                Toast.makeText(context, "Ad not ready yet. Please try again later.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = rewardedAdManager?.isAdLoaded() == true,
                        text = stringResource(R.string.watch_ad_for_extra_predictions),
                        pulseEnabled = true
                    )
                }
            }

            if (showPredictions.value) {
                PredictionCarousel(predictions, comparison, teams, h2h)
            }
        }
    }
}

@Composable
fun FixtureListScreen(
    combinedFormData: List<SharedViewModel.FixtureWithType>,
    homeTeamIdInt: Int,
    awayTeamIdInt: Int,
    fixtureDetails: ResponseData,
    navController: NavController
) {
    // Derive filtered lists only once to improve performance
    val homeTeamFixtures by
    remember(combinedFormData) {
        derivedStateOf { combinedFormData.filter { it.isHome }.take(4) }
    }

    val awayTeamFixtures by
    remember(combinedFormData) {
        derivedStateOf { combinedFormData.filter { !it.isHome }.take(4) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        FixtureColumn(
            fixturesWithType = homeTeamFixtures,
            columnTitle = fixtureDetails.teams.home.name,
            homeTeamIdInt = homeTeamIdInt,
            awayTeamIdInt = awayTeamIdInt,
            modifier = Modifier.weight(1f),
            navController = navController
        )

        FixtureColumn(
            fixturesWithType = awayTeamFixtures,
            columnTitle = fixtureDetails.teams.away.name,
            homeTeamIdInt = homeTeamIdInt,
            awayTeamIdInt = awayTeamIdInt,
            modifier = Modifier.weight(1f),
            navController = navController
        )
    }
}

@Composable
fun FixtureColumn(
    fixturesWithType: List<SharedViewModel.FixtureWithType>,
    columnTitle: String,
    homeTeamIdInt: Int,
    awayTeamIdInt: Int,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier
            .width(200.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = columnTitle,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (fixturesWithType.isEmpty()) {
            Text(
                text = stringResource(R.string.no_fixtures_available),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                fixturesWithType.forEach { fixtureWithType ->
                    FixtureCard(
                        fixture = fixtureWithType.fixture,
                        isHome = fixtureWithType.isHome,
                        teamId = TeamId(homeTeamIdInt, awayTeamIdInt),
                        navController = navController
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

data class TeamId(val homeTeamId: Int, val awayTeamId: Int)

@Composable
fun FixtureCard(
    fixture: com.soccertips.predictx.data.model.lastfixtures.FixtureDetails,
    isHome: Boolean,
    teamId: TeamId,
    navController: NavController
) {
    val cardColor =
        getCardColor(
            fixture,
            isHome,
            teamId,
        )
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clickable {
                    navController.navigate(
                        Routes.FixtureDetails.createRoute(fixture.fixture.id.toString())
                    )
                },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.weight(5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter =
                            rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(fixture.teams.home.logo)
                                    .crossfade(true)
                                    .build()
                            ),
                        contentDescription = stringResource(R.string.home_team_logo),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = fixture.teams.home.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter =
                            rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(fixture.teams.away.logo)
                                    .crossfade(true)
                                    .build()
                            ),
                        contentDescription = stringResource(R.string.away_team_logo),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = fixture.teams.away.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            VerticalDivider(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(cardColor)
                    .padding(4.dp)
            ) {
                Text(
                    text = fixture.score.fulltime.home.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )

                Text(
                    text = fixture.score.fulltime.away.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun getCardColor(
    fixture: com.soccertips.predictx.data.model.lastfixtures.FixtureDetails,
    isHome: Boolean,
    teamId: TeamId,
): Color {
    return when {
        // Match is a draw
        fixture.score.fulltime.home == fixture.score.fulltime.away ->
            MaterialTheme.colorScheme.surfaceVariant

        // Home team logic
        isHome -> {
            if ((fixture.teams.home.winner == true && fixture.teams.home.id == teamId.homeTeamId) ||
                (fixture.teams.away.winner == true &&
                        fixture.teams.away.id == teamId.homeTeamId)
            ) {
                Color.Green.copy(alpha = 0.3f)
            } else {
                Color.Red.copy(alpha = 0.3f)
            }
        }

        // Away team logic
        !isHome -> {
            if ((fixture.teams.away.winner == true && fixture.teams.away.id == teamId.awayTeamId) ||
                (fixture.teams.home.winner == true &&
                        fixture.teams.home.id == teamId.awayTeamId)
            ) {
                Color.Green.copy(alpha = 0.3f)
            } else {
                Color.Red.copy(alpha = 0.3f)
            }
        }

        // Default fallback
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
fun FixtureDetailCard(fixture: Fixture) {
    // Format the fixture date safely
    val dateUnavailable = stringResource(R.string.date_unavailable)
    val date =
        remember(fixture.date) {
            try {
                val inputDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                val outputDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                LocalDateTime.parse(fixture.date, inputDate).format(outputDate)
            } catch (e: Exception) {
                dateUnavailable
            }
        }

    // Handle potentially missing information
    val referee = fixture.referee?.takeIf { it.isNotBlank() } ?: "-"
    val venueName = fixture.venue?.name?.takeIf { it.isNotBlank() } ?: "-"
    val venueCity = fixture.venue?.city?.takeIf { it.isNotBlank() } ?: "-"
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Date info
            FixtureDetailRow(icon = Icons.Default.CalendarMonth, text = date)
            Spacer(modifier = Modifier.height(8.dp))

            // Referee info
            FixtureDetailRow(icon = Icons.Default.Sports, text = referee)
            Spacer(modifier = Modifier.height(8.dp))

            // Venue info
            FixtureDetailRow(icon = Icons.Default.Stadium, text = venueName)

            // Only show city if different from venue name to avoid redundancy
            if (!venueName.contains(venueCity, ignoreCase = true)) {
                Text(
                    text = venueCity,
                    modifier =
                        Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .align(Alignment.CenterHorizontally),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun FixtureDetailRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            modifier =
                Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .align(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
