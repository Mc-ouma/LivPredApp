package com.soccertips.predictx.ui.fixturedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.soccertips.predictx.R
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.viewmodel.FixtureDetailsViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

@Composable
fun FixtureScoreAndScorers(
    viewModel: FixtureDetailsViewModel,
    modifier: Modifier = Modifier,
    navController: NavController,
    leagueId: String,
    season: String,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (uiState) {
        is FixtureDetailsUiState.Loading -> {
            Text(
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(16.dp),
                color = Color.Gray,
            )
        }

        is FixtureDetailsUiState.Success -> {
            val response = (uiState as FixtureDetailsUiState.Success).fixtureDetails
            val homeTeamId = response.teams.home.id
            val awayTeamId = response.teams.away.id
            val homeGoalScorers = viewModel.getHomeGoalScorers(response.events, homeTeamId)
            val awayGoalScorers = viewModel.getAwayGoalScorers(response.events, awayTeamId)
            viewModel.formatTimestamp(response.fixture.timestamp)
            val initialMatchStatusText =
                viewModel.getMatchStatusText(
                    response.fixture.status.short,
                    response.fixture.status.elapsed,
                    response.fixture.timestamp,
                )
            val cardColors = LocalCardColors.current
            val cardElevation = LocalCardElevation.current

            var matchStatusText by remember { mutableStateOf(initialMatchStatusText) }
            val timestamp = response.fixture.timestamp

            LaunchedEffect(key1 = timestamp) {
                while (true) {
                    val currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                    val timeDifference = timestamp - currentTime
                    if (timeDifference > 0 && timeDifference <= 3600) {
                        val minutes = timeDifference / 60
                        val seconds = timeDifference % 60
                        matchStatusText = context.getString(R.string.time_format, minutes, seconds)
                    } else {
                        matchStatusText = initialMatchStatusText
                    }
                    delay(1000) // Update every second
                }
            }

            Card(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = cardColors,
                elevation = cardElevation,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Match Status Badge
                    Surface(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = response.fixture.status.short,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (matchStatusText.isNotEmpty()) {
                                Text(
                                    text = " • ",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = matchStatusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Teams and Score Row - Perfectly Symmetrical
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Home Team
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            response.teams.home.let { homeTeam ->
                                TeamColumn(
                                    team = homeTeam,
                                    leagueId = leagueId,
                                    season = season,
                                    navController = navController
                                )
                            }
                        }

                        // Score Display - Centered with decorative elements
                        // Check if match has not started
                        val matchNotStarted = response.fixture.status.short in listOf("TBD", "NS", "PST", "CANC", "ABD", "AWD", "WO")
                        
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Home score
                                    Text(
                                        text = if (matchNotStarted) "-" else "${response.goals.home ?: "-"}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    
                                    // Separator
                                    Text(
                                        text = "  :  ",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                    
                                    // Away score
                                    Text(
                                        text = if (matchNotStarted) "-" else "${response.goals.away ?: "-"}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        // Away Team
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            response.teams.away.let { awayTeam ->
                                TeamColumn(
                                    team = awayTeam,
                                    leagueId = leagueId,
                                    season = season,
                                    navController = navController
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Goal Scorers Section
                    if (homeGoalScorers.isNotEmpty() || awayGoalScorers.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Home scorers - Right aligned
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                homeGoalScorers.forEach { (playerName, elapsed) ->
                                    ScorerRow(
                                        playerName = playerName,
                                        elapsed = elapsed,
                                        isHomeTeam = true
                                    )
                                }
                            }
                            
                            // Center icon
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = stringResource(R.string.soccer_ball),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            
                            // Away scorers - Left aligned
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                awayGoalScorers.forEach { (playerName, elapsed) ->
                                    ScorerRow(
                                        playerName = playerName,
                                        elapsed = elapsed,
                                        isHomeTeam = false
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        is FixtureDetailsUiState.Error -> {
            ErrorScreen(
                paddingValues = PaddingValues(0.dp),
                message = stringResource(R.string.no_data_available),
                // (uiState as FixtureDetailsUiState.Error).message
                onRetry = {}
            )
        }
    }
}

@Composable
fun Scorers(
    playerName: String,
    elapsed: String,
) {
    Text(
        text = "$playerName $elapsed'",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}

@Composable
fun ScorerRow(
    playerName: String,
    elapsed: String,
    isHomeTeam: Boolean,
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isHomeTeam) Arrangement.End else Arrangement.Start
    ) {
        if (isHomeTeam) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$elapsed'",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$elapsed'",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScorersPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ScorerRow(playerName = "M. Salah", elapsed = "23", isHomeTeam = true)
            ScorerRow(playerName = "D. Núñez", elapsed = "67", isHomeTeam = true)
            Spacer(modifier = Modifier.height(16.dp))
            ScorerRow(playerName = "B. Fernandes", elapsed = "45", isHomeTeam = false)
        }
    }
}

@Preview(showBackground = true, name = "Fixture Score Card")
@Composable
private fun FixtureScoreCardPreview() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Match Status Badge
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = " • ",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "Full Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Teams and Score Row - Perfectly Symmetrical
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Home Team
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔴", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Liverpool",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    // Score Display
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "3",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "  :  ",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Away Team
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔵", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Man United",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Goal Scorers Section
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    // Home scorers - Right aligned
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        ScorerRow(playerName = "M. Salah", elapsed = "23", isHomeTeam = true)
                        ScorerRow(playerName = "D. Núñez", elapsed = "45", isHomeTeam = true)
                        ScorerRow(playerName = "L. Díaz", elapsed = "78", isHomeTeam = true)
                    }

                    // Center icon
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = "Soccer Ball",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Away scorers - Left aligned
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        ScorerRow(playerName = "B. Fernandes", elapsed = "56", isHomeTeam = false)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Fixture Score Card - No Goals")
@Composable
private fun FixtureScoreCardNoGoalsPreview() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Match Status Badge
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "NS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = " • ",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "15:00",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Teams and Score Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚪", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Real Madrid",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "  :  ",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔵", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Barcelona",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

