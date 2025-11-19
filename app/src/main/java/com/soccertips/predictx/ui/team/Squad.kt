package com.soccertips.predictx.ui.team

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.team.squad.Player
import com.soccertips.predictx.data.model.team.squad.Response
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation

@Composable
fun SquadScreen(squadResponse: List<Response>, lazyListState: LazyListState) {
    val playersByPosition = squadResponse.flatMap { it.players }.groupBy { it.position }

    // Scrollable Content
    LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
    ) {
        playersByPosition.forEach { (position, players) ->
            item {
                Text(
                        text =
                                if (position == "Goalkeeper") "Goalkeepers"
                                else if (position == "Defender") "Defence"
                                else if (position == "Midfielder") "Midfield"
                                else if (position == "Attacker") "Attack" else position,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(players.size) { player -> PlayerItem(player = players[player]) }

            item { Spacer(modifier = Modifier.height(8.dp)) }
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Image
            Image(
                    painter = rememberAsyncImagePainter(player.photo),
                    contentDescription = "Player Photo",
                    modifier = Modifier.size(48.dp).clip(CircleShape)
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
                        text = stringResource(R.string.age_years, player.age),
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
