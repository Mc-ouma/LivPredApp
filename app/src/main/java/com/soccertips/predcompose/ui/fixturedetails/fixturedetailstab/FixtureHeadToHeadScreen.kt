package com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.soccertips.predcompose.R
import com.soccertips.predcompose.data.model.headtohead.FixtureDetails
import com.soccertips.predcompose.data.model.headtohead.TeamInfo
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FixtureHeadToHeadScreen(headToHead: List<FixtureDetails>, navController: NavController) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        if (headToHead.isEmpty()) {
            Text(text = "No head-to-head data available")
        } else {
            headToHead.forEach { fixture ->
                FixtureCard(fixture = fixture, navController = navController)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FixtureCard(
    fixture: FixtureDetails,
    navController: NavController
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate(Routes.FixtureDetails.createRoute(fixture.fixture.id.toString()))
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // Parse and format the date
            val inputDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val outputDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val date = try {
                LocalDateTime.parse(fixture.fixture.date, inputDate).format(outputDate)
            } catch (e: Exception) {
                // Handle parsing error gracefully
                "Invalid Date"
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
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f),
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

@OptIn(ExperimentalGlideComposeApi::class)
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
        GlideImage(
            model = team.logo,
            contentDescription = "${team.name} logo",
            modifier = Modifier.size(24.dp),
            failure = placeholder(R.drawable.placeholder),
        )
        Text(
            text = team.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isWinner) Color.Green else Color.Unspecified,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

