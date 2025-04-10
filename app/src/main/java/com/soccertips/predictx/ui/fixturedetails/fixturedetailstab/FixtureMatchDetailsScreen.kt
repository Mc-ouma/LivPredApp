package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.Fixture
import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.data.model.lastfixtures.FixtureDetails
import com.soccertips.predictx.data.model.prediction.Biggest
import com.soccertips.predictx.data.model.prediction.BiggestGoals
import com.soccertips.predictx.data.model.prediction.CleanSheet
import com.soccertips.predictx.data.model.prediction.Comparison
import com.soccertips.predictx.data.model.prediction.ComparisonData
import com.soccertips.predictx.data.model.prediction.FailedToScore
import com.soccertips.predictx.data.model.prediction.FixtureDetail
import com.soccertips.predictx.data.model.prediction.Fixtures
import com.soccertips.predictx.data.model.prediction.GoalAverage
import com.soccertips.predictx.data.model.prediction.GoalData
import com.soccertips.predictx.data.model.prediction.GoalStats
import com.soccertips.predictx.data.model.prediction.GoalTotal
import com.soccertips.predictx.data.model.prediction.Goals
import com.soccertips.predictx.data.model.prediction.H2H
import com.soccertips.predictx.data.model.prediction.HomeAway
import com.soccertips.predictx.data.model.prediction.Last5
import com.soccertips.predictx.data.model.prediction.Last5Goals
import com.soccertips.predictx.data.model.prediction.League
import com.soccertips.predictx.data.model.prediction.Percent
import com.soccertips.predictx.data.model.prediction.Periods
import com.soccertips.predictx.data.model.prediction.Predictions
import com.soccertips.predictx.data.model.prediction.Score
import com.soccertips.predictx.data.model.prediction.Status
import com.soccertips.predictx.data.model.prediction.Streak
import com.soccertips.predictx.data.model.prediction.Team
import com.soccertips.predictx.data.model.prediction.TeamGoals
import com.soccertips.predictx.data.model.prediction.TeamLeague
import com.soccertips.predictx.data.model.prediction.TeamShort
import com.soccertips.predictx.data.model.prediction.Teams
import com.soccertips.predictx.data.model.prediction.TeamsShort
import com.soccertips.predictx.data.model.prediction.Venue
import com.soccertips.predictx.data.model.prediction.WinLoss
import com.soccertips.predictx.data.model.prediction.Winner
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.viewmodel.SharedViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            PredictionCarousel(predictions, comparison, teams, h2h)
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
            columnTitle = fixtureDetails.teams.home.name,
            homeTeamIdInt = homeTeamIdInt,
            awayTeamIdInt = awayTeamIdInt,
            modifier = Modifier.weight(1f),
            navController = navController
        )

        FixtureColumn(
            fixturesWithType = combinedFormData.filter { !it.isHome },
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
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                .padding(8.dp)
        ){
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
                    modifier = Modifier.size(16.dp),
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Icon(
                    imageVector = Icons.Default.Sports,
                    modifier = Modifier.size(16.dp),
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                    modifier = Modifier.size(16.dp),
                    contentDescription = null,
                )
                Text(
                    text = "$venueName, $venueCity",
                    modifier =
                        Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .align(Alignment.CenterVertically),
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}


// Additional cards would be implemented similarly

@Preview
@Composable
private fun PredictionCarouselPreview() {
    val predictions = Predictions(
        winner = Winner(23, "Arsenal", "Home team is likely to win"),
        under_over = "+2.5",
        percent = Percent("50%", "25%", "25%"),
        advice = "Home team is likely to win",
        win_or_draw = true,
        goals = Goals("1", "2"),
    )
    val comparison = Comparison(
        form = ComparisonData("50", "50"),
        att = ComparisonData("50", "50"),
        def = ComparisonData("50", "50"),
        h2h = ComparisonData("50", "50"),
        total = ComparisonData("50", "50"),
        poisson_distribution = ComparisonData("50", "50"),
        goals = ComparisonData("50", "50")

    )
    val teams = Teams(
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

    val h2h = listOf(
        H2H(
            fixture = com.soccertips.predictx.data.model.prediction.Fixture(
                id = 1,
                date = "2022-12-25T12:00:00+00:00",
                referee = "John Doe",
                venue = Venue(22, "Emirates Stadium", "London"),
                timezone = "UTC",
                periods = Periods(1, 2),
                status = Status("Match Finished", "FT", 90, null),
                timestamp = 2
            ),
            goals = HomeAway(1, 2),
            league = League(
                1,
                "Premier League",
                "England",
                "https://media.api-sports.io/football/leagues/1.png",
                "https://media.api-sports.io/flags/gb.svg",
                2022
            ),
            teams = TeamsShort(
                home = TeamShort(
                    1,
                    "Arsenal",
                    "https://media.api-sports.io/football/teams/57.png",
                    winner = true
                ),
                away = TeamShort(
                    2,
                    "Chelsea",
                    "https://media.api-sports.io/football/teams/61.png",
                    winner = false
                )
            ),
            score = Score(
                halftime = HomeAway(1, 2),
                fulltime = HomeAway(1, 2),
                extratime = HomeAway(1, 2),
                penalty = HomeAway(1, 2)
            ),

            )
    )


    PredictionCarousel(
        predictions = predictions,
        comparison = comparison,
        teams = teams,
        h2h = h2h
    )


}
@Preview
@Composable
private fun FixtureDetailPrev() {
    FixtureDetailCard(
        fixture = Fixture(
            id = 1,
            date = "2022-12-25T12:00:00+00:00",
            referee = "John Doe",
            venue = com.soccertips.predictx.data.model.Venue(22, "Emirates Stadium", "London"),
            timezone = "UTC",
            periods = com.soccertips.predictx.data.model.Periods(1, 2),
            status = com.soccertips.predictx.data.model.Status("Match Finished", "FT", 90, null),
            timestamp = 2
        )
    )

}