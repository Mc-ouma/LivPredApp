package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.headtohead.FixtureDetails
import com.soccertips.predictx.data.model.headtohead.TeamInfo
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FixtureHeadToHeadScreen(headToHead: List<FixtureDetails>, navController: NavController) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        if (headToHead.isEmpty()) {
            Text(text = stringResource(R.string.no_head_to_head_data_available))
        } else {
            headToHead.forEach { fixture ->
                FixtureCard(fixture = fixture, navController = navController)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FixtureCard(fixture: FixtureDetails, navController: NavController) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
            colors = cardColors,
            elevation = cardElevation,
            modifier =
                    Modifier.fillMaxWidth().padding(8.dp).clickable {
                        navController.navigate(
                                Routes.FixtureDetails.createRoute(fixture.fixture.id.toString())
                        )
                    },
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
        ) {
            // Parse and format the date
            val inputDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val outputDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val date =
                    try {
                        LocalDateTime.parse(fixture.fixture.date, inputDate).format(outputDate)
                    } catch (e: Exception) {
                        // Handle parsing error gracefully
                        stringResource(R.string.invalid_date)
                    }

            Text(
                    text = fixture.league.name,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold,
            )

            Text(text = date, style = MaterialTheme.typography.labelSmall)

            // Teams and scores
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamSection(
                        team = fixture.teams.home,
                        isWinner = fixture.teams.home.winner == true,
                        modifier = Modifier.weight(1f)
                )
                // Scores (home vs away)
                Text(
                        text = " ${fixture.goals.home} - ${fixture.goals.away}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                        textAlign = TextAlign.Center,
                )
                TeamSection(
                        team = fixture.teams.away,
                        isWinner = fixture.teams.away.winner == true,
                        modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TeamSection(
        team: TeamInfo,
        isWinner: Boolean,
        modifier: Modifier = Modifier,
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.padding(8.dp),
    ) {
        Image(
                painter = rememberAsyncImagePainter(model = team.logo),
                contentDescription = stringResource(R.string.team_logo_format, team.name),
                modifier = Modifier.size(24.dp),
        )
        Text(
                text = team.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isWinner) Color.Green else Color.Unspecified,
                modifier = Modifier.padding(top = 4.dp),
        )
    }
}
