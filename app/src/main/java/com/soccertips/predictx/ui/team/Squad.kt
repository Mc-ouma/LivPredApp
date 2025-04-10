package com.soccertips.predictx.ui.team

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.team.squad.Player
import com.soccertips.predictx.data.model.team.squad.Response
import com.soccertips.predictx.data.model.team.squad.Team
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.ui.theme.PredictXTheme
import timber.log.Timber

@Composable
fun SquadScreen(
    squadResponse: List<Response>,
    lazyListState: LazyListState
) {
    val playersByPosition = squadResponse.flatMap { it.players }.groupBy { it.position }

    // Scrollable Content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .onGloballyPositioned { layoutCoordinates ->
                // Update the top padding of the LazyColumn based on the height of the team info
                Timber.d("Squad Size: ${layoutCoordinates.size.height}")
            }
    ) {
        // Display players for each position

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .height(1125.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    // Update the top padding of the LazyColumn based on the height of the team info
                    Timber.d("Squad Size: ${layoutCoordinates.size.height}")
                },
        ) {
            playersByPosition.forEach { (position, players) ->
                item {
                    Text(
                        text = if (position == "Goalkeeper") "Goalkeepers" else if (position == "Defender") "Defence" else if (position == "Midfielder") "Midfield" else if (position == "Attacker") "Attack" else position,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(players.size) { player ->
                    PlayerItem(player = players[player])
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item {
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }


}


@Composable
fun PlayerItem(player: Player) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Image
            Image(
                painter = rememberAsyncImagePainter(player.photo),
                contentDescription = "Player Photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Player Details
            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Age: ${player.age} years",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Player Number
            Text(
                text = "#${player.number}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun SquadScreenPreview() {

    val mockResponse = Response(
        team = Team(
            id = 33,
            name = "Manchester United",
            logo = "https://media.api-sports.io/football/teams/33.png"
        ),
        players = listOf(
            Player(
                id = 50132,
                name = "A. Bayındır",
                age = 26,
                number = 1,
                position = "Goalkeeper",
                photo = "https://media.api-sports.io/football/players/50132.png"
            ),
            Player(
                id = 889,
                name = "V. Lindelöf",
                age = 30,
                number = 2,
                position = "Defender",
                photo = "https://media.api-sports.io/football/players/889.png"
            ),
            Player(
                id = 19220,
                name = "M. Mount",
                age = 25,
                number = 7,
                position = "Midfielder",
                photo = "https://media.api-sports.io/football/players/19220.png"
            ),
            Player(
                id = 19221,
                name = "Mark Mount",
                age = 28,
                number = 17,
                position = "Midfielder",
                photo = "https://media.api-sports.io/football/players/19220.png"
            ),
            Player(
                id = 909,
                name = "M. Rashford",
                age = 27,
                number = 10,
                position = "Attacker",
                photo = "https://media.api-sports.io/football/players/909.png"
            )
        )
    )

    PredictXTheme {
        SquadScreen(squadResponse = listOf(mockResponse),
            lazyListState = rememberLazyListState()
        )
    }
}
