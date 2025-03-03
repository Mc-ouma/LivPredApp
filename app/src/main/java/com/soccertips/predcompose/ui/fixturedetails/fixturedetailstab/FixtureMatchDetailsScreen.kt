package com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.data.model.Fixture
import com.soccertips.predcompose.data.model.ResponseData
import com.soccertips.predcompose.data.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.data.model.prediction.Predictions
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.viewmodel.SharedViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FixtureMatchDetailsScreen(
    fixtures: List<SharedViewModel.FixtureWithType>,
    predictions: Predictions?,
    fixtureDetails: ResponseData,
    homeTeamId: String,
    awayTeamId: String,
    navController: NavController
) {
    // Safely convert team IDs to integers, defaulting to 0 if invalid or empty
    val homeTeamIdInt = homeTeamId.toIntOrNull() ?: 0
    val awayTeamIdInt = awayTeamId.toIntOrNull() ?: 0

    // Check for invalid team IDs and display an error message if necessary
    if (homeTeamIdInt == 0 || awayTeamIdInt == 0) {
        Text("Invalid team ID(s). Please check your input.", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Fixture detail card
        FixtureDetailCard(fixture = fixtureDetails.fixture)

        // Spacer between sections
        Spacer(modifier = Modifier.height(16.dp))

        // Home and away fixtures section
        FixtureListScreen(
            combinedFormData = fixtures,
            homeTeamIdInt = homeTeamIdInt,
            awayTeamIdInt = awayTeamIdInt,
            fixtureDetails = fixtureDetails,
            navController = navController
        )

        // Spacer between sections
        Spacer(modifier = Modifier.height(16.dp))

        // Prediction card section
        if (predictions == null) {
            Text("No predictions available.", modifier = Modifier.padding(16.dp))
        } else {
            PredictionCard(predictions = predictions)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            FixtureColumn(
                fixturesWithType = combinedFormData.filter { it.isHome },
                columnTitle = "${fixtureDetails.teams.home.name} Last Fixtures",
                homeTeamIdInt = homeTeamIdInt,
                awayTeamIdInt = awayTeamIdInt,
                modifier = Modifier.weight(1f),
                navController = navController
            )

            FixtureColumn(
                fixturesWithType = combinedFormData.filter { !it.isHome },
                columnTitle = "${fixtureDetails.teams.away.name} Last Fixtures",
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
        Text(
            text = columnTitle,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        if (fixturesWithType.isEmpty()) {
            Text(
                text = "No fixtures available",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp) // Adjust the max height as needed
            ) {
                items(fixturesWithType.take(4)) { fixtureWithType -> // Display only the first 4 items
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
    fixture: FixtureDetails,
    isHome: Boolean,
    teamId: TeamId,
    navController: NavController
) {
    val cardColor = getCardColor(
        fixture,
        isHome,
        teamId,
    )
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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
                        painter = rememberAsyncImagePainter(model = fixture.teams.home.logo),
                        contentDescription = "Home Team Logo",
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
                        painter = rememberAsyncImagePainter(model = fixture.teams.away.logo),
                        contentDescription = "Away Team Logo",
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
                modifier = Modifier
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
    fixture: FixtureDetails,
    isHome: Boolean,
    teamId: TeamId
): Color {

    return when {
        // Match is a draw
        fixture.score.fulltime.home == fixture.score.fulltime.away -> Color.Gray.copy(alpha = 0.5f)

        // Home team logic
        isHome -> {
            if ((fixture.teams.home.winner == true && fixture.teams.home.id == teamId.homeTeamId) ||
                (fixture.teams.away.winner == true && fixture.teams.away.id == teamId.homeTeamId)
            ) {
                Color.Green
            } else {
                Color.Red
            }
        }

        // Away team logic
        !isHome -> {
            if ((fixture.teams.away.winner == true && fixture.teams.away.id == teamId.awayTeamId) ||
                (fixture.teams.home.winner == true && fixture.teams.home.id == teamId.awayTeamId)
            ) {
                Color.Green
            } else {
                Color.Red
            }
        }

        // Default fallback
        else -> Color.Gray.copy(alpha = 0.3f)
    }
}


@Composable
fun FixtureDetailCard(fixture: Fixture) {
    val date =
        fixture.date?.let {
            val inputDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val outputDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            LocalDateTime.parse(it, inputDate).format(outputDate)
        } ?: ""
    val referee = fixture.referee ?: ""
    val venueName = fixture.venue.name ?: ""
    val venueCity = fixture.venue.city ?: ""

    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(
            modifier =
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                )
                Text(
                    text = date,
                    modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .align(Alignment.CenterVertically),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Icon(
                    imageVector = Icons.Default.Sports,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                )
                Text(
                    text = referee,
                    modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .align(Alignment.CenterVertically),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier =
                Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Stadium,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                )
                Text(
                    text = "$venueName, $venueCity",
                    modifier =
                    Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .align(Alignment.CenterVertically),
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
fun PredictionCard(predictions: Predictions) {
    val underOverText =
        predictions.under_over?.let {
            when {
                it.startsWith("-") -> "Under ${it.removePrefix("-")}"
                it.startsWith("+") -> "Over ${it.removePrefix("+")}"
                else -> it
            }
        } ?: ""

    OutlinedCard(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            predictions.winner.name?.let { winnerName ->
                Text(
                    text = "Winner: $winnerName (${predictions.winner.comment})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            underOverText?.let {
                Text(
                    text = "Goals: $it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            predictions.advice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row {
                    Text(text = "Home: ${predictions.percent.home}")
                    LinearProgressIndicator(
                        progress = {
                            predictions.percent.home
                                .removeSuffix("%")
                                .toFloat() / 100
                        },
                        modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(4.dp)),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    )

                }

                Row {

                    Text(text = "Draw: ${predictions.percent.draw}")
                    LinearProgressIndicator(
                        progress = {
                            predictions.percent.draw
                                .removeSuffix("%")
                                .toFloat() / 100
                        },
                        modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(4.dp)),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    )
                }

                Row {

                    Text(text = "Away: ${predictions.percent.away}")
                    LinearProgressIndicator(
                        progress = {
                            predictions.percent.away
                                .removeSuffix("%")
                                .toFloat() / 100
                        },
                        modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(4.dp)),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    )
                }
            }
        }
    }
}




