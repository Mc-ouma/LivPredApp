package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun FixtureHeadToHeadScreen(headToHead: List<FixtureDetails>, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (headToHead.isEmpty()) {
            item {
                Text(text = stringResource(R.string.no_head_to_head_data_available))
            }
        } else {
            items(headToHead) { fixture ->
                FixtureCard(fixture = fixture, navController = navController)
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
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable {
                        navController.navigate(
                                Routes.FixtureDetails.createRoute(fixture.fixture.id.toString())
                        )
                    },
    ) {
        Column(
                modifier = Modifier.padding(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header with league info
            androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
            ) {
                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                            painter = rememberAsyncImagePainter(model = fixture.league.logo),
                            contentDescription = "League Logo",
                            modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                            text = fixture.league.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Teams and scores
            Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamSection(
                        team = fixture.teams.home,
                        isWinner = fixture.teams.home.winner == true,
                        modifier = Modifier.weight(1f)
                )

                // Scores (home vs away)
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .weight(0.7f)
                ) {
                    Text(
                            text = "${fixture.goals.home} - ${fixture.goals.away}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                    )
                    Text(
                            text = "FT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                    )
                }

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
            modifier = modifier,
    ) {
        Image(
                painter = rememberAsyncImagePainter(model = team.logo),
                contentDescription = stringResource(R.string.team_logo_format, team.name),
                modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
                text = team.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
        )
    }
}
