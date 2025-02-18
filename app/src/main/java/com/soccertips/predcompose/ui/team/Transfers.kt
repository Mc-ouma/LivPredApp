package com.soccertips.predcompose.ui.team

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.data.model.FixtureResponse
import com.soccertips.predcompose.data.model.events.FixtureEventsResponse
import com.soccertips.predcompose.data.model.headtohead.HeadToHeadResponse
import com.soccertips.predcompose.data.model.lastfixtures.FixtureListResponse
import com.soccertips.predcompose.data.model.lineups.FixtureLineupResponse
import com.soccertips.predcompose.data.model.prediction.PredictionResponse
import com.soccertips.predcompose.data.model.standings.StandingsResponse
import com.soccertips.predcompose.data.model.statistics.StatisticsResponse
import com.soccertips.predcompose.data.model.team.squad.SquadResponse
import com.soccertips.predcompose.data.model.team.teamscreen.TeamModelData
import com.soccertips.predcompose.data.model.team.teamscreen.TeamStatisticsResponse
import com.soccertips.predcompose.data.model.team.transfer.Player2
import com.soccertips.predcompose.data.model.team.transfer.Response2
import com.soccertips.predcompose.data.model.team.transfer.Team
import com.soccertips.predcompose.data.model.team.transfer.Teams
import com.soccertips.predcompose.data.model.team.transfer.Transfer
import com.soccertips.predcompose.data.model.team.transfer.TransferResponse
import com.soccertips.predcompose.network.FixtureDetailsService
import com.soccertips.predcompose.repository.TeamsRepository
import com.soccertips.predcompose.ui.components.rememberPaginationState
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import com.soccertips.predcompose.viewmodel.TeamViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


@OptIn(FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun TransferScreen(
    viewModel: TeamViewModel,
    transfers: List<Response2>,
    teamId: String,
    onTeamInfoVisibilityChanged: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()

    val paginationState = rememberPaginationState()

    // Constants
    val LOAD_MORE_THRESHOLD = 5

    LaunchedEffect(lazyListState) {
        launch {
            snapshotFlow {
                val layoutInfo = lazyListState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItemIndex =
                    lazyListState.firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size
                lastVisibleItemIndex > (totalItems - LOAD_MORE_THRESHOLD)
            }
                .distinctUntilChanged()
                .collect { shouldLoadMore ->
                    if (shouldLoadMore && !viewModel.isLoading && paginationState.hasMorePages) {
                        paginationState.loadNextPage()
                        viewModel.getTransfers(teamId)
                    }
                }
        }
        launch {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .collect { firstVisibleItemIndex ->
                    onTeamInfoVisibilityChanged(firstVisibleItemIndex == 0)
                }
        }
    }


    /*LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        itemsIndexed(transfers) { index, response ->
            //   val isExpanded = expandedItems.contains(index)
            val isExpanded by remember(
                expandedItems,
                index
            ) { derivedStateOf { expandedItems.contains(index) } }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedItems = if (isExpanded) {
                                expandedItems - index
                            } else {
                                expandedItems + index
                            }
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = response.player.name ?: "Unknown Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }

                if (isExpanded) {
                    response.transfers.forEach { transfer ->
                        TransferItem(
                            playerName = response.player.name ?: "Unknown Player",
                            transfer = transfer,
                            teamId = teamId
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading Indicator
        if (viewModel.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }*/
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        itemsIndexed(items = transfers) { index, response ->
            TransferItem(
                transfer = response.transfers.first(),
                teamId = teamId,
                playerName = response.player.name ?: "Unknown Player",
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }

        if (transfers.isNotEmpty() && viewModel.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Show loading indicator when initially loading
    if (transfers.isEmpty() && viewModel.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun TransferItem(
    transfer: Transfer,
    playerName: String,
    teamId: String, // The ID of the team you consider “current” (optional)
    modifier: Modifier = Modifier
) {
    // Determine if the transfer is incoming (the current team is the destination)
    val isIncoming = transfer.teams.`in`.id == teamId.toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Player Name
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Teams and transfer direction row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "From" team (the team the player is leaving)
                TeamLogoAndName(team = transfer.teams.out)

                Spacer(modifier = Modifier.width(8.dp))

                // Transfer direction icon (arrow)
                Icon(
                    imageVector =  Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = if (isIncoming) "Incoming Transfer" else "Outgoing Transfer",
                    tint = if (isIncoming) Color.Green else Color.Red,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // "To" team (the team the player is joining)
                TeamLogoAndName(team = transfer.teams.`in`)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transfer Date and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transfer.date ?: "Unknown date",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                TransferTypeLabel(type = transfer.type)
            }
        }
    }
}

@Composable
fun TeamLogoAndName(
    team: Team,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Load team logo (using Coil's AsyncImage or similar)
        Image(
            painter = rememberAsyncImagePainter(model = team.logo),
            contentDescription = team.name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = team.name ?: "Unknown Team",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TransferTypeLabel(
    type: String?,
    modifier: Modifier = Modifier
) {
    val text = type ?: "N/A"
    // Color coding based on transfer type
    val backgroundColor = when {
        text.contains("Free", ignoreCase = true) -> Color.Green.copy(alpha = 0.2f)
        text.contains("Loan", ignoreCase = true) -> Color.Blue.copy(alpha = 0.2f)
        text.contains("Swap", ignoreCase = true) -> Color.Magenta.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    }
    val textColor = when {
        text.contains("Free", ignoreCase = true) -> Color.Green
        text.contains("Loan", ignoreCase = true) -> Color.Blue
        text.contains("Swap", ignoreCase = true) -> Color.Magenta
        else -> MaterialTheme.colorScheme.secondary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = textColor,
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}


@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun TransferScreenPreview() {
    val sampleResponses = listOf(
        Response2(
            player = Player2(id = 19285, name = "L. Steele"),
            transfers = listOf(
                Transfer(
                    date = "2006-08-10",
                    type = "€ 250K",
                    teams = Teams(
                        `in` = Team(
                            id = 60,
                            name = "West Brom",
                            logo = "https://media.api-sports.io/football/teams/60.png"
                        ),
                        out = Team(
                            id = 33,
                            name = "Manchester United",
                            logo = "https://media.api-sports.io/football/teams/33.png"
                        )
                    )
                )
            ),
            update = "2023-04-05T04:02:27+00:00"
        ),
        Response2(
            player = Player2(id = 231667, name = "L. Giverin"),
            transfers = listOf(
                Transfer(
                    date = "2012-08-29",
                    type = "Loan",
                    teams = Teams(
                        `in` = Team(
                            id = 740,
                            name = "Antwerp",
                            logo = "https://media.api-sports.io/football/teams/740.png"
                        ),
                        out = Team(
                            id = 33,
                            name = "Manchester United",
                            logo = "https://media.api-sports.io/football/teams/33.png"
                        )
                    )
                )
            ),
            update = "2023-09-03T13:53:31+00:00"
        ),
        Response2(
            player = Player2(id = 115964, name = "J. Greening"),
            transfers = listOf(
                Transfer(
                    date = "2001-08-01",
                    type = "€ 3M",
                    teams = Teams(
                        `in` = Team(
                            id = 70,
                            name = "Middlesbrough",
                            logo = "https://media.api-sports.io/football/teams/70.png"
                        ),
                        out = Team(
                            id = 33,
                            name = "Manchester United",
                            logo = "https://media.api-sports.io/football/teams/33.png"
                        )
                    )
                ),
                Transfer(
                    date = "2004-08-31",
                    type = "€ 2M",
                    teams = Teams(
                        `in` = Team(
                            id = 33,
                            name = "Manchester United",
                            logo = "https://media.api-sports.io/football/teams/33.png"
                        ),
                        out = Team(
                            id = 70,
                            name = "Middlesbrough",
                            logo = "https://media.api-sports.io/football/teams/70.png"
                        )
                    )
                ),
                Transfer(
                    date = "2006-08-10",
                    type = "€ 250K",
                    teams = Teams(
                        `in` = Team(
                            id = 60,
                            name = "West Brom",
                            logo = "https://media.api-sports.io/football/teams/60.png"
                        ),
                        out = Team(
                            id = 33,
                            name = "Manchester United",
                            logo = "https://media.api-sports.io/football/teams/33.png"
                        )
                    )
                )
            ),
            update = "2023-09-05T00:32:22+00:00"
        )
    )

    PredComposeTheme {
        TransferScreen(
            transfers = sampleResponses,
            teamId = "60",
            viewModel = TeamViewModel(
                repository = TeamsRepository(
                    // Replace this TODO with an actual implementation or a fake service for preview purposes
                    teamsService = DummyTeamsService()
                )
            )
        ) { visible ->
            // Optionally handle team info visibility change for preview purposes
        }
    }
}

class DummyTeamsService : FixtureDetailsService {
    override suspend fun getFixtureDetails(fixtureId: String): FixtureResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getLastFixtures(
        season: String,
        teamId: String,
        last: String
    ): FixtureListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getNextFixtures(
        season: String,
        teamId: String,
        next: String
    ): FixtureListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getHeadToHeadFixtures(
        teams: String,
        last: String
    ): HeadToHeadResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getLineups(fixtureId: String): FixtureLineupResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getFixtureStats(
        fixtureId: String,
        teamId: String
    ): StatisticsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getFixtureEvents(fixtureId: String): FixtureEventsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getStandings(
        leagueId: String,
        season: String
    ): StandingsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getPredictions(fixtureId: String): PredictionResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getTeams(
        leagueId: String,
        season: String,
        teamId: String
    ): TeamStatisticsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getPlayers(teamId: String): SquadResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getTransfers(
        teamId: String,
        page: Int
    ): TransferResponse {
        TODO("Not yet implemented")
    }


    override suspend fun getTeamData(teamId: Int): TeamModelData {
        TODO("Not yet implemented")
    }
}
