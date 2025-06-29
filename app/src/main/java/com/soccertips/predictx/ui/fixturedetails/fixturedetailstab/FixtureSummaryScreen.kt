package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.events.AssistInfo
import com.soccertips.predictx.data.model.events.EventTime
import com.soccertips.predictx.data.model.events.FixtureEvent
import com.soccertips.predictx.data.model.events.PlayerInfo
import com.soccertips.predictx.data.model.events.TeamInfo
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation

@Composable
fun FixtureSummaryScreen(events: List<FixtureEvent>, homeTeamId: Int, awayTeamId: Int) {
    // Sort events by descending elapsed time
    val reversedEvents = events.sortedByDescending { it.time.elapsed }

    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            // Use remember to avoid unnecessary recomposition
            val eventCount = reversedEvents.size

            // Iterate over the sorted events and display them
            reversedEvents.forEachIndexed { index, event ->
                EventCard(
                        event = event,
                        homeTeamId = homeTeamId,
                        awayTeamId = awayTeamId,
                        drawLine = index < eventCount - 1 // Draw line only between events
                )
            }
        }
    }
}

@Composable
fun EventCard(event: FixtureEvent, homeTeamId: Int, awayTeamId: Int, drawLine: Boolean) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        // Home team events on the left
        if (event.team.id == homeTeamId) {
            EventDetails(event = event, alignment = Alignment.Start)
        }

        // Away team events on the right
        if (event.team.id == awayTeamId) {
            EventDetails(event = event, alignment = Alignment.End)
        }

        // Event Time in the center of the card
        Box(modifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp)) {
            Canvas(modifier = Modifier.size(40.dp)) {
                drawCircle(color = Color.Gray, style = Stroke(width = 2.dp.toPx()))
            }
            Text(
                    text = "${event.time.elapsed}${event.time.extra?.let { "+$it" } ?: ""}’",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
            )
        }

        // Draw a vertical line connecting the event times
        if (drawLine) {
            val circleRadius = 12.dp // Radius of the circles
            val strokeWidth = 2.dp // Width of the line
            val offsetY = 4.dp // Offset to center the circles vertically

            val homeEvent = event.team.id == homeTeamId
            val awayEvent = event.team.id == awayTeamId

            val startOffset =
                    if (homeEvent) {
                        Offset(
                                x = 0f,
                                y =
                                        with(LocalDensity.current) {
                                            offsetY.toPx() + circleRadius.toPx()
                                        }
                        )
                    } else {
                        Offset(
                                x = 0f,
                                y =
                                        with(LocalDensity.current) {
                                            offsetY.toPx() - circleRadius.toPx()
                                        }
                        )
                    }

            val endOffset =
                    if (awayEvent) {
                        Offset(
                                x = 0f,
                                y =
                                        with(LocalDensity.current) {
                                            offsetY.toPx() - circleRadius.toPx()
                                        }
                        )
                    } else {
                        Offset(
                                x = 0f,
                                y =
                                        with(LocalDensity.current) {
                                            offsetY.toPx() + circleRadius.toPx()
                                        }
                        )
                    }

            Canvas(modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(56.dp)) {
                val centerOffset = Offset(x = size.width / 2, y = size.height / 2)

                val startY = centerOffset.y + startOffset.y
                val endY = centerOffset.y + endOffset.y

                drawLine(
                        color = Color.LightGray,
                        start = Offset(centerOffset.x, startY),
                        end = Offset(centerOffset.x, endY),
                        strokeWidth = strokeWidth.toPx()
                )
            }
        }
    }
}

@Composable
fun EventDetails(event: FixtureEvent, alignment: Alignment.Horizontal) {
    // Handle null checks for event properties
    val playerName = event.player.name // Player name is non-null
    val eventDetail = event.detail // Event detail is non-null
    val eventType = event.type // Event type is non-null

    Column(
            horizontalAlignment = alignment, // Left or Right alignment based on team
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
    ) {
        EventIcon(event)

        when (eventType) {
            "Goal" -> {
                Text(
                        text = playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                )
                Text(
                        text =
                                when (eventDetail) {
                                    "Normal Goal" -> stringResource(R.string.goal_type_normal)
                                    "Own Goal" -> stringResource(R.string.goal_type_own)
                                    "Penalty" -> stringResource(R.string.goal_type_penalty)
                                    "Missed Penalty" ->
                                            stringResource(R.string.goal_type_missed_penalty)
                                    else -> eventDetail
                                },
                        style = MaterialTheme.typography.bodySmall,
                )
            }
            "Card" -> {
                Text(
                        text = playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                )
                Text(
                        text =
                                when (eventDetail) {
                                    "Yellow Card" -> stringResource(R.string.card_type_yellow)
                                    "Red Card" -> stringResource(R.string.card_type_red)
                                    else -> eventDetail
                                },
                        color = if (eventDetail == "Red Card") Color.Red else Color.Yellow,
                        style = MaterialTheme.typography.bodySmall,
                )
            }
            "subst" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        event.assist?.name?.let { assistName ->
                            Text(
                                    text = assistName,
                                    style =
                                            MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold
                                            ),
                            )
                        }
                        Text(
                                text = playerName,
                                style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column {
                        Text(
                                text = " ➡️ ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                        )
                        Text(
                                text = " ⬅️ ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                        )
                    }
                }
            }
            "Var" -> {
                Text(
                        text = playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                )
                Text(
                        text =
                                when (eventDetail) {
                                    "Goal cancelled" -> stringResource(R.string.goal_cancelled)
                                    "Penalty confirmed" -> stringResource(R.string.penalty_confirmed)
                                    else -> eventDetail
                                },
                        style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> {
                Text(
                        text = "$eventType - $eventDetail",
                        style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun EventIcon(event: FixtureEvent) {
    when (event.type) {
        "Goal" ->
                Icon(
                        imageVector = Icons.Filled.SportsSoccer,
                        contentDescription = "Goal Icon",
                        tint =
                                if (event.detail == "Own Goal" || event.detail == "Missed Penalty")
                                        Color.Red
                                else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                )
        "Card" ->
                Icon(
                        imageVector = Icons.Filled.Square,
                        contentDescription = "Card Icon",
                        tint = if (event.detail == "Red Card") Color.Red else Color.Yellow,
                        modifier = Modifier.size(24.dp),
                )
        "subst" ->
                Icon(
                        imageVector = Icons.TwoTone.SwapHoriz,
                        contentDescription = "Substitution Icon",
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp),
                )
        "Var" ->
                Icon(
                        imageVector = Icons.Filled.Computer,
                        contentDescription = "VAR Icon",
                        tint = if (event.detail == "Goal cancelled") Color.Red else Color.Green,
                        modifier = Modifier.size(24.dp),
                )
        else ->
                Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Event Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp),
                )
    }
}

@Preview(showBackground = true)
@Composable
fun FixtureEventsScreenPreview() {
    val sampleEvents =
            listOf(
                    // Goal Events
                    FixtureEvent(
                            time = EventTime(elapsed = 10, extra = null),
                            team =
                                    TeamInfo(
                                            id = 1,
                                            name = "Team 1",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/463.png",
                                    ),
                            player = PlayerInfo(id = 101, name = "Mikel O'Neil"),
                            assist = null,
                            type = "Goal",
                            detail = "Normal Goal",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 15, extra = null),
                            team =
                                    TeamInfo(
                                            id = 1,
                                            name = "Team 1",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/463.png",
                                    ),
                            player = PlayerInfo(id = 102, name = "Gareth Bale"),
                            assist = null,
                            type = "Goal",
                            detail = "Own Goal",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 20, extra = null),
                            team =
                                    TeamInfo(
                                            id = 2,
                                            name = "Team 2",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/442.png",
                                    ),
                            player = PlayerInfo(id = 201, name = "David Beckham"),
                            assist = null,
                            type = "Goal",
                            detail = "Penalty",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 25, extra = null),
                            team =
                                    TeamInfo(
                                            id = 2,
                                            name = "Team 2",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/442.png",
                                    ),
                            player = PlayerInfo(id = 202, name = "Lionel Messi"),
                            assist = null,
                            type = "Goal",
                            detail = "Missed Penalty",
                            comments = null,
                    ),
                    // Card Events
                    FixtureEvent(
                            time = EventTime(elapsed = 30, extra = null),
                            team =
                                    TeamInfo(
                                            id = 1,
                                            name = "Team 1",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/463.png",
                                    ),
                            player = PlayerInfo(id = 101, name = "Player E"),
                            assist = null,
                            type = "Card",
                            detail = "Yellow Card",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 35, extra = null),
                            team =
                                    TeamInfo(
                                            id = 2,
                                            name = "Team 2",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/442.png",
                                    ),
                            player = PlayerInfo(id = 201, name = "Neymar Jr"),
                            assist = null,
                            type = "Card",
                            detail = "Red Card",
                            comments = null,
                    ),
                    // Substitution Events
                    FixtureEvent(
                            time = EventTime(elapsed = 50, extra = null),
                            team =
                                    TeamInfo(
                                            id = 1,
                                            name = "Team 1",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/463.png",
                                    ),
                            player = PlayerInfo(id = 101, name = "Aubameyang"),
                            assist = AssistInfo(id = 103, name = "Fernandes"),
                            type = "subst",
                            detail = "Substitution 1",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 55, extra = null),
                            team =
                                    TeamInfo(
                                            id = 2,
                                            name = "Team 2",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/442.png",
                                    ),
                            player = PlayerInfo(id = 202, name = "Iniesta"),
                            assist = AssistInfo(id = 104, name = "Xavi"),
                            type = "subst",
                            detail = "Substitution 2",
                            comments = null,
                    ),
                    // VAR Events
                    FixtureEvent(
                            time = EventTime(elapsed = 60, extra = null),
                            team =
                                    TeamInfo(
                                            id = 1,
                                            name = "Team 1",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/463.png",
                                    ),
                            player = PlayerInfo(id = 101, name = "Quaresma"),
                            assist = null,
                            type = "Var",
                            detail = "Goal cancelled",
                            comments = null,
                    ),
                    FixtureEvent(
                            time = EventTime(elapsed = 65, extra = null),
                            team =
                                    TeamInfo(
                                            id = 2,
                                            name = "Team 2",
                                            logo =
                                                    "https://media.api-sports.io/football/teams/442.png",
                                    ),
                            player = PlayerInfo(id = 201, name = "Player L"),
                            assist = null,
                            type = "Var",
                            detail = "Penalty confirmed",
                            comments = null,
                    ),
            )

    FixtureSummaryScreen(events = sampleEvents, homeTeamId = 1, awayTeamId = 2)
}
