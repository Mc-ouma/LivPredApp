package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.standings.TeamStanding
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation

@Composable
fun FixtureStandingsScreen(
        standings: List<TeamStanding>,
        teamId1: Int,
        teamId2: Int,
) {
    val groupedStandings = standings.groupBy { it.group }

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedStandings.forEach { (groupName, groupStandings) ->
            item {
                ModernGroupCard(
                        groupName = groupName,
                        standings = groupStandings,
                        highlightedTeamIds = listOf(teamId1, teamId2)
                )
            }
        }
    }
}

@Composable
fun ModernGroupCard(
        groupName: String,
        standings: List<TeamStanding>,
        highlightedTeamIds: List<Int>
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(colors = cardColors, elevation = cardElevation, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group Header
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
            }

            // Column Headers
            StandingsHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Team Rows
            standings.forEachIndexed { index, teamStanding ->
                ModernTeamRow(
                        teamStanding = teamStanding,
                        isHighlighted = teamStanding.team.id in highlightedTeamIds
                )

                if (index < standings.size - 1) {
                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StandingsHeader() {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
                text = "#",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Team
        Text(
                text = stringResource(R.string.team),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Stats
        listOf("P", "W", "D", "L", "GD", "Pts").forEach { header ->
            Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                            Modifier.width(
                                    if (header == "Pts") 36.dp
                                    else if (header == "GD") 30.dp else 28.dp
                            ),
                    textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModernTeamRow(teamStanding: TeamStanding, isHighlighted: Boolean) {
    val backgroundColor =
            when {
                isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> Color.Transparent
            }

    val borderColor =
            when {
                isHighlighted -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .then(
                                    if (isHighlighted) {
                                        Modifier.border(
                                                width = 2.dp,
                                                color = borderColor,
                                                shape = RoundedCornerShape(8.dp)
                                        )
                                    } else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank with badge
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            val rankColor =
                    when (teamStanding.rank) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }

            if (teamStanding.rank <= 3) {
                Box(
                        modifier =
                                Modifier.size(24.dp)
                                        .clip(CircleShape)
                                        .background(rankColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = "${teamStanding.rank}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = rankColor
                    )
                }
            } else {
                Text(
                        text = "${teamStanding.rank}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Team Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = teamStanding.team.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Statistics
        StatCell(teamStanding.all.played.toString(), 28.dp)
        StatCell(teamStanding.all.win.toString(), 28.dp, Color(0xFF4CAF50))
        StatCell(teamStanding.all.draw.toString(), 28.dp, Color(0xFFFF9800))
        StatCell(teamStanding.all.lose.toString(), 28.dp, Color(0xFFF44336))

        // Goal Difference with indicator
        Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
            val gdColor =
                    when {
                        teamStanding.goalsDiff > 0 -> Color(0xFF4CAF50)
                        teamStanding.goalsDiff < 0 -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }

            Text(
                    text =
                            if (teamStanding.goalsDiff > 0) "+${teamStanding.goalsDiff}"
                            else "${teamStanding.goalsDiff}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = gdColor,
                    textAlign = TextAlign.Center
            )
        }

        // Points with emphasis
        Box(
                modifier =
                        Modifier.width(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    text = "${teamStanding.points}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatCell(
        value: String,
        width: androidx.compose.ui.unit.Dp,
        color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
) {
    Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
        )
    }
}
