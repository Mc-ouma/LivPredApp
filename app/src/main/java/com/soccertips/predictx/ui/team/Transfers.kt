package com.soccertips.predictx.ui.team

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.team.transfer.Response2
import com.soccertips.predictx.data.model.team.transfer.Team
import com.soccertips.predictx.data.model.team.transfer.Transfer
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun TransferScreen(
    transfers: LazyPagingItems<Response2>,
    teamId: String,
    lazyListState: LazyListState
) {


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (transfers.loadState.refresh) {
            is LoadState.Error -> {
                Text(
                    text = "Error loading transfers",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is LoadState.Loading -> {
                CircularProgressIndicator()
            }

            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(transfers.itemCount) { index ->
                        transfers[index]?.let { transfer ->
                            TransferItem(
                                transfer = transfer.transfers.first(),
                                teamId = teamId,
                                playerName = transfer.player.name ?: "Unknown Player",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        if (transfers.loadState.append is LoadState.Loading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        if (transfers.loadState.append is LoadState.Error) {
                            Text(
                                text = "Error loading more transfers",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
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
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = cardElevation,
        colors = cardColors,
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
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
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

