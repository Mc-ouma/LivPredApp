package com.soccertips.predcompose.ui.team

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.soccertips.predcompose.model.team.squad.Player
import com.soccertips.predcompose.model.team.squad.Response
import com.soccertips.predcompose.model.team.squad.Team
import com.soccertips.predcompose.ui.theme.PredComposeTheme

@Composable
fun SquadScreen(
    squadResponse: List<Response>,
    teamInfoVisible: Boolean,
    onTeamInfoVisibilityChanged: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val players = squadResponse.flatMap { it.players }
    val groupedPlayers = players.groupBy { it.position }

    // Observe scroll state to hide/show the page info
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide page info when scrolling up, show it when scrolling down
                onTeamInfoVisibilityChanged(firstVisibleItemIndex == 0)
            }
    }


    // Scrollable Content
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedPlayers.forEach { (position, group) ->
                item {
                    PlayerListByPosition(title = position, players = group)
                }
            }
        }
    }


}


@Composable
fun PlayerListByPosition(title: String, players: List<Player>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = if (title == "Goalkeeper") "Goalkeepers" else if (title == "Defender") "Defenders" else if (title == "Midfielder") "Midfielders" else if (title == "Attacker") " Attackers" else title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Column {
            players.forEach { player ->
                PlayerCard(player = player)
            }
        }
    }
}

@Composable
fun PlayerCard(player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Photo
        Image(
            painter = rememberAsyncImagePainter(model = player.photo),
            contentDescription = "${player.name}'s Photo",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
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
                text = "Age: ${player.age} | Number: ${player.number}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = player.position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

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
                id = 909,
                name = "M. Rashford",
                age = 27,
                number = 10,
                position = "Attacker",
                photo = "https://media.api-sports.io/football/players/909.png"
            )
        )
    )

    PredComposeTheme {
        SquadScreen(squadResponse = listOf(mockResponse), teamInfoVisible = true) { visible ->
        }
    }
}
