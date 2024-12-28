package com.soccertips.predcompose.ui.team

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.model.team.transfer.Player2
import com.soccertips.predcompose.model.team.transfer.Response2
import com.soccertips.predcompose.model.team.transfer.Team
import com.soccertips.predcompose.model.team.transfer.Teams
import com.soccertips.predcompose.model.team.transfer.Transfer
import com.soccertips.predcompose.repository.TeamsRepository
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import com.soccertips.predcompose.viewmodel.TeamViewModel
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class)
@Composable
fun TransferScreen(
    viewModel: TeamViewModel,
    transfers: List<Response2>,
    teamId: String,
    onTeamInfoVisibilityChanged: (Boolean) -> Unit
) {
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var expandedItems by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val lazyListState = rememberLazyListState()

    // Observe scroll state to hide/show the page info
    //pagination

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                onTeamInfoVisibilityChanged(firstVisibleItemIndex == 0)
            }

        if (lazyListState.isScrollInProgress && lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == transfers.size - 1 && !viewModel.isLoading) {
            currentPage++
            viewModel.getTransfers(teamId, currentPage)
        }
    }



    LazyColumn(
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
    }
}


@Composable
fun TransferItem(playerName: String, transfer: Transfer, teamId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Player Name
        Text(
            text = playerName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Arrow Icon
            val isIncoming = transfer.teams.`in`.id == teamId.toInt()
            Icon(
                imageVector = if (isIncoming) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = if (isIncoming) "Incoming" else "Outgoing",
                tint = if (isIncoming) Color.Green else Color.Red,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Team To Logo
            Image(
                painter = rememberAsyncImagePainter(model = transfer.teams.`in`.logo),
                contentDescription = "Team Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Team To Name
                Text(
                    text = transfer.teams.`in`.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                // Transfer Date
                Text(
                    text = transfer.date ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Transfer Type
            val transferType = transfer.type ?: "N/A"

            // Determine the color based on transfer type
            val transferColor = when {
                transferType.contains("Loan", ignoreCase = true) -> Color.Blue
                transferType.contains("Free", ignoreCase = true) -> Color.Gray
                transferType.contains("Swap", ignoreCase = true) -> Color.Magenta
                transferType.contains("€") || transferType.contains("$") || transferType.contains("£") -> Color.Green
                transferType == "N/A" -> Color.LightGray
                else -> MaterialTheme.colorScheme.primary
            }

            // Format transfer type if monetary (e.g., "€ 250K", "€ 30.5M")
            val formattedTransferType = when {
                transferType.contains("€") -> transferType // Already formatted
                else -> transferType // Keep as-is for other types
            }
            Text(
                text = formattedTransferType,
                style = MaterialTheme.typography.bodyMedium,
                color = transferColor,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
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
                    ),
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
                    ),
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
            transfers = sampleResponses, teamId = "33",
            viewModel = TeamViewModel(
                repository = TeamsRepository(
                    teamsService = TODO()
                )
            ),
        ) { visible ->
        }

    }
}

