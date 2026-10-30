package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.lineups.CoachInfo
import com.soccertips.predictx.data.model.lineups.PlayerInfo
import com.soccertips.predictx.data.model.lineups.PlayerLineup
import com.soccertips.predictx.data.model.lineups.TeamColors
import com.soccertips.predictx.data.model.lineups.TeamLineup

// Helper function to safely parse color strings
private fun parseColorOrFallback(colorString: String?, fallback: Color): Color {
    if (colorString == null) return fallback
    return try {
        if (colorString.startsWith("#")) {
            Color(colorString.toColorInt())
        } else {
            Color("#$colorString".toColorInt())
        }
    } catch (_: Exception) {
        fallback
    }
}

// Calculate relative luminance of a color (0.0 = black, 1.0 = white)
private fun Color.luminance(): Float {
    val r =
        if (red <= 0.03928f) red / 12.92f else Math.pow(((red + 0.055f) / 1.055f).toDouble(), 2.4)
            .toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else Math.pow(
        ((green + 0.055f) / 1.055f).toDouble(),
        2.4
    ).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else Math.pow(
        ((blue + 0.055f) / 1.055f).toDouble(),
        2.4
    ).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

// Get contrasting text color based on background luminance
private fun getContrastingTextColor(backgroundColor: Color, preferredTextColor: Color): Color {
    val bgLuminance = backgroundColor.luminance()
    val textLuminance = preferredTextColor.luminance()

    // Calculate contrast ratio (simplified)
    val lighter = maxOf(bgLuminance, textLuminance)
    val darker = minOf(bgLuminance, textLuminance)
    val contrastRatio = (lighter + 0.05f) / (darker + 0.05f)

    // If contrast ratio is too low (< 3:1), use opposite color
    return if (contrastRatio < 3f) {
        if (bgLuminance > 0.5f) Color.Black else Color.White
    } else {
        preferredTextColor
    }
}

// Object to handle localized player positions
object PlayerPositionLocalizer {

    @Composable
    fun getShortLocalizedPosition(position: String?): String {
        return when (position?.uppercase()) {
            "G" -> stringResource(R.string.position_goalkeeper_short)
            "D" -> stringResource(R.string.position_defender_short)
            "M" -> stringResource(R.string.position_midfielder_short)
            "F" -> stringResource(R.string.position_forward_short)
            else -> position ?: stringResource(R.string.na)
        }
    }
}

@Composable
fun FixtureLineupsScreen(lineups: Pair<TeamLineup, TeamLineup>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Football Pitch Visualization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Match Formation Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormationBadge(lineup = lineups.first)
                        Text(
                            text = "vs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FormationBadge(lineup = lineups.second)
                    }

                    // Football Pitch with both teams
                    FootballPitch(
                        homeLineup = lineups.first,
                        awayLineup = lineups.second,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.58f) // Taller vertical pitch ratio for better spacing
                    )
                }
            }
        }

        // Detailed lineups card (substitutes, coach info)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = stringResource(R.string.substitutes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Detailed lineups
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Home team lineup details
                        SubstitutesAndCoach(lineup = lineups.first, modifier = Modifier.weight(1f))

                        // Vertical divider
                        VerticalDivider(
                            modifier = Modifier
                                .height(300.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Away team lineup details
                        SubstitutesAndCoach(lineup = lineups.second, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Football pitch colors
private val PitchGreen = Color(0xFF2E7D32)
private val PitchLightGreen = Color(0xFF388E3C)
private val PitchLineColor = Color.White.copy(alpha = 0.9f)

// Position types for fallback logic
private enum class PositionType { GK, DEF, MID, FWD }

private fun getPositionType(pos: String?): PositionType {
    return when (pos?.uppercase()) {
        "G" -> PositionType.GK
        "D" -> PositionType.DEF
        "M" -> PositionType.MID
        "F" -> PositionType.FWD
        else -> PositionType.MID
    }
}

// Parse formation string like "4-3-3" into [4, 3, 3]
private fun parseFormation(formation: String?): List<Int> {
    if (formation.isNullOrEmpty()) return listOf(4, 4, 2)
    val rows = Regex("\\d+").findAll(formation).map { it.value.toInt() }.toList()
    val sum = rows.sum()
    return if (sum !in 8..12) listOf(4, 4, 2) else rows
}

data class PositionedPlayer(
    val player: PlayerInfo,
    val x: Float, // 0-100 percentage
    val y: Float, // 0-100 percentage
    val teamColors: TeamColors?
)

@Composable
fun FootballPitch(
    homeLineup: TeamLineup,
    awayLineup: TeamLineup,
    modifier: Modifier = Modifier
) {
    val homeTeamName = homeLineup.team.name?.take(3)?.uppercase() ?: "HOM"
    val awayTeamName = awayLineup.team.name?.take(3)?.uppercase() ?: "AWY"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PitchGreen)
    ) {
        // Grass stripes pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Alternating grass stripes (horizontal)
            val stripeCount = 10
            val stripeHeight = height / stripeCount
            for (i in 0 until stripeCount) {
                if (i % 2 == 0) {
                    drawRect(
                        color = PitchLightGreen,
                        topLeft = Offset(0f, i * stripeHeight),
                        size = Size(width, stripeHeight)
                    )
                }
            }
        }

        // Pitch markings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = 2.dp.toPx()
            val halfHeight = height / 2

            // Outer boundary
            drawRect(
                color = PitchLineColor,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(width - strokeWidth, height - strokeWidth),
                style = Stroke(width = strokeWidth)
            )

            // Halfway line
            drawLine(
                color = PitchLineColor,
                start = Offset(0f, halfHeight),
                end = Offset(width, halfHeight),
                strokeWidth = strokeWidth
            )

            // Center circle
            val centerCircleRadius = width * 0.15f
            drawCircle(
                color = PitchLineColor,
                center = Offset(width / 2, halfHeight),
                radius = centerCircleRadius,
                style = Stroke(width = strokeWidth)
            )

            // Center spot
            drawCircle(
                color = PitchLineColor,
                center = Offset(width / 2, halfHeight),
                radius = 4.dp.toPx()
            )

            // === TOP (Away team goal) ===
            val penaltyAreaWidth = width * 0.7f
            val penaltyAreaHeight = height * 0.16f
            val penaltyAreaLeft = (width - penaltyAreaWidth) / 2

            // Top penalty area
            drawRect(
                color = PitchLineColor,
                topLeft = Offset(penaltyAreaLeft, 0f),
                size = Size(penaltyAreaWidth, penaltyAreaHeight),
                style = Stroke(width = strokeWidth)
            )

            // Top goal area (6-yard box)
            val goalAreaWidth = width * 0.3f
            val goalAreaHeight = height * 0.06f
            val goalAreaLeft = (width - goalAreaWidth) / 2
            drawRect(
                color = PitchLineColor,
                topLeft = Offset(goalAreaLeft, 0f),
                size = Size(goalAreaWidth, goalAreaHeight),
                style = Stroke(width = strokeWidth)
            )

            // Top penalty spot
            val penaltySpotY = penaltyAreaHeight * 0.7f
            drawCircle(
                color = PitchLineColor,
                center = Offset(width / 2, penaltySpotY),
                radius = 3.dp.toPx()
            )

            // Top penalty arc
            val arcRadius = centerCircleRadius * 0.8f
            drawArc(
                color = PitchLineColor.copy(alpha = 0.7f),
                startAngle = 35f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(width / 2 - arcRadius, penaltyAreaHeight - arcRadius * 0.3f),
                size = Size(arcRadius * 2, arcRadius),
                style = Stroke(width = strokeWidth)
            )

            // === BOTTOM (Home team goal) ===
            // Bottom penalty area
            drawRect(
                color = PitchLineColor,
                topLeft = Offset(penaltyAreaLeft, height - penaltyAreaHeight),
                size = Size(penaltyAreaWidth, penaltyAreaHeight),
                style = Stroke(width = strokeWidth)
            )

            // Bottom goal area
            drawRect(
                color = PitchLineColor,
                topLeft = Offset(goalAreaLeft, height - goalAreaHeight),
                size = Size(goalAreaWidth, goalAreaHeight),
                style = Stroke(width = strokeWidth)
            )

            // Bottom penalty spot
            drawCircle(
                color = PitchLineColor,
                center = Offset(width / 2, height - penaltySpotY),
                radius = 3.dp.toPx()
            )

            // Bottom penalty arc
            drawArc(
                color = PitchLineColor.copy(alpha = 0.7f),
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(
                    width / 2 - arcRadius,
                    height - penaltyAreaHeight - arcRadius * 0.7f
                ),
                size = Size(arcRadius * 2, arcRadius),
                style = Stroke(width = strokeWidth)
            )

            // Corner arcs
            val cornerRadius = width * 0.04f
            // Top-left
            drawArc(
                color = PitchLineColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(-cornerRadius, -cornerRadius),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(width = strokeWidth)
            )
            // Top-right
            drawArc(
                color = PitchLineColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(width - cornerRadius, -cornerRadius),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(width = strokeWidth)
            )
            // Bottom-left
            drawArc(
                color = PitchLineColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(-cornerRadius, height - cornerRadius),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(width = strokeWidth)
            )
            // Bottom-right
            drawArc(
                color = PitchLineColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(width - cornerRadius, height - cornerRadius),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(width = strokeWidth)
            )
        }

        // Team name watermarks
        Box(modifier = Modifier.fillMaxSize()) {
            // Home team watermark (top 25%)
            Text(
                text = homeTeamName,
                color = Color.White.copy(alpha = 0.1f),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )

            // Away team watermark (bottom 25%)
            Text(
                text = awayTeamName,
                color = Color.White.copy(alpha = 0.1f),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }

        // Position players on the pitch
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val pitchWidth = maxWidth
            val pitchHeight = maxHeight

            // Process home team (top half, plays downward)
            // Mirror X axis so left/right positions match the away team perspective
            val homePlayers = processTeamPlayers(
                lineup = homeLineup,
                side = "home",
                startY = 5f,
                endY = 43f,
                mirrorX = true
            )

            // Process away team (bottom half, plays upward)
            val awayPlayers = processTeamPlayers(
                lineup = awayLineup,
                side = "away",
                startY = 92f,
                endY = 57f,
                mirrorX = false
            )
            (homePlayers + awayPlayers).forEach { positioned ->
                PlayerMarker(
                    player = positioned.player,
                    teamColors = positioned.teamColors,
                    xPercent = positioned.x,
                    yPercent = positioned.y,
                    pitchWidth = pitchWidth,
                    pitchHeight = pitchHeight
                )
            }
        }
    }
}

private fun processTeamPlayers(
    lineup: TeamLineup,
    side: String,
    startY: Float,
    endY: Float,
    mirrorX: Boolean = false
): List<PositionedPlayer> {
    val players = lineup.startXI ?: return emptyList()
    val positionedPlayers = mutableListOf<PositionedPlayer>()

    // Check if majority of players have grid data
    val hasGridData = players.count { it.player.grid != null } > 8

    val rows: List<List<PlayerInfo>> = if (hasGridData) {
        // Grid-based positioning
        val rowMap = mutableMapOf<Int, MutableList<Pair<Int, PlayerInfo>>>()

        players.forEach { playerLineup ->
            val grid = playerLineup.player.grid ?: return@forEach
            val parts = grid.split(":")
            if (parts.size == 2) {
                val row = parts[0].toIntOrNull() ?: 1
                val col = parts[1].toIntOrNull() ?: 1
                if (!rowMap.containsKey(row)) {
                    rowMap[row] = mutableListOf()
                }
                rowMap[row]!!.add(Pair(col, playerLineup.player))
            }
        }

        // Sort rows by key, then sort players in each row by column
        rowMap.keys.sorted().map { rowKey ->
            rowMap[rowKey]!!.sortedBy { it.first }.map { it.second }
        }
    } else {
        // Fallback: use formation string
        val result = mutableListOf<List<PlayerInfo>>()

        // Find goalkeeper
        val gk = players.find { getPositionType(it.player.pos) == PositionType.GK }
        if (gk != null) {
            result.add(listOf(gk.player))
        }

        // Parse formation and distribute outfield players
        val formationRows = parseFormation(lineup.formation)
        val outfield = players
            .filter { getPositionType(it.player.pos) != PositionType.GK }
            .sortedBy {
                when (getPositionType(it.player.pos)) {
                    PositionType.DEF -> 1
                    PositionType.MID -> 2
                    PositionType.FWD -> 3
                    else -> 99
                }
            }
            .map { it.player }

        var cursor = 0
        formationRows.forEach { count ->
            val rowPlayers = outfield.drop(cursor).take(count)
            if (rowPlayers.isNotEmpty()) {
                result.add(rowPlayers)
            }
            cursor += count
        }

        // Handle any remaining players
        if (cursor < outfield.size) {
            val remaining = outfield.drop(cursor)
            if (result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + remaining
            } else {
                result.add(remaining)
            }
        }

        result
    }

    // Calculate Y positions using linear interpolation
    val numRows = rows.size
    if (numRows == 0) return emptyList()

    rows.forEachIndexed { rowIndex, rowPlayers ->
        val y = if (numRows <= 1) {
            startY
        } else {
            val step = (endY - startY) / (numRows - 1)
            startY + (step * rowIndex)
        }

        rowPlayers.forEachIndexed { playerIndex, player ->
            // Distribute evenly on X axis: (100 / (count + 1)) * (index + 1)
            val baseX = (100f / (rowPlayers.size + 1)) * (playerIndex + 1)
            // Mirror X if needed (for home team to match perspective)
            val x = if (mirrorX) 100f - baseX else baseX

            positionedPlayers.add(
                PositionedPlayer(
                    player = player,
                    x = x,
                    y = y,
                    teamColors = lineup.team.colors
                )
            )
        }
    }

    return positionedPlayers
}

@Composable
fun PlayerMarker(
    player: PlayerInfo,
    teamColors: TeamColors?,
    xPercent: Float,
    yPercent: Float,
    pitchWidth: Dp,
    pitchHeight: Dp
) {
    val density = LocalDensity.current
    val markerSize = 28.dp
    val nameHeight = 16.dp // Approximate height for the name text with padding
    val columnWidth = 48.dp
    val halfColumnWidth = columnWidth / 2
    val totalMarkerHeight = markerSize + nameHeight

    // Calculate actual position from percentage
    val xOffset = pitchWidth * (xPercent / 100f)
    val yOffset = pitchHeight * (yPercent / 100f)

    // Clamp Y position to keep the entire marker (circle + name) within bounds
    val minY = markerSize / 2
    val maxY = pitchHeight - totalMarkerHeight + (markerSize / 2)
    val clampedYOffset = yOffset.coerceIn(minY, maxY)

    val playerColor = if (player.pos == "G") teamColors?.goalkeeper else teamColors?.player
    val primaryColor = parseColorOrFallback(playerColor?.primary, Color(0xFF1565C0))
    val rawNumberColor = parseColorOrFallback(playerColor?.number, Color.White)
    val borderColor = parseColorOrFallback(playerColor?.border, Color.White)

    // Ensure number color has sufficient contrast with primary background
    val numberColor = getContrastingTextColor(primaryColor, rawNumberColor)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = with(density) { (xOffset - halfColumnWidth).roundToPx() },
                    y = with(density) { (clampedYOffset - (markerSize / 2)).roundToPx() }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(columnWidth)
        ) {
            // Player circle with number
            Box(
                modifier = Modifier
                    .size(markerSize)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .border(1.5.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number?.toString() ?: "",
                    color = numberColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Player name (last name only) with high contrast background for readability
            Text(
                text = player.name?.split(" ")?.lastOrNull()?.take(8) ?: "",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(3.dp))
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun FormationBadge(lineup: TeamLineup) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Team Logo
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = lineup.team.logo),
                contentDescription = lineup.team.name?.take(20)
                    ?: stringResource(R.string.team_logo),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = lineup.team.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = lineup.formation ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SubstitutesAndCoach(lineup: TeamLineup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Team header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = lineup.team.logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lineup.team.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Substitutes players
            lineup.substitutes?.forEach { playerLineup ->
                PlayerRow(player = playerLineup.player, teamColors = lineup.team.colors)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coach section
            CoachSection(coach = lineup.coach)
        }
    }
}

@Composable
fun CoachSection(coach: CoachInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coach photo
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter =
                        rememberAsyncImagePainter(
                            model = coach.photo,
                            onError = {
                                /* Handle error gracefully */
                            }
                        ),
                    contentDescription = coach.name?.take(20)
                        ?: stringResource(R.string.coach_photo),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Coach info
            Column {
                Text(
                    text = stringResource(R.string.coach_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = coach.name ?: stringResource(R.string.coach_default),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PlayerRow(
    player: PlayerInfo,
    teamColors: TeamColors?,
) {
    if (teamColors == null) {
        // Handle the case where teamColors is null
        Text(
            text = player.name?.take(20) ?: stringResource(R.string.unknown_player),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val playerColor = if (player.pos == "G") teamColors.goalkeeper else teamColors.player

    // Use helper function instead of try-catch in composable
    val primaryColor = parseColorOrFallback(playerColor.primary, MaterialTheme.colorScheme.primary)
    val rawNumberColor =
        parseColorOrFallback(playerColor.number, MaterialTheme.colorScheme.onPrimary)
    parseColorOrFallback(playerColor.border, MaterialTheme.colorScheme.outline)

    // Ensure number color has sufficient contrast with primary background
    val numberColor = getContrastingTextColor(primaryColor, rawNumberColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player Number in a circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number?.toString() ?: stringResource(R.string.na),
                    color = numberColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Name and Position
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name?.take(20) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Position indicator chip
                    Surface(
                        modifier = Modifier.padding(end = 6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = primaryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text =
                                PlayerPositionLocalizer.getShortLocalizedPosition(
                                    player.pos
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFixtureLineupsScreen() {
    val sampleLineup = TeamLineup(
        team = com.soccertips.predictx.data.model.lineups.TeamInfo(
            id = 1,
            name = "Sample FC",
            logo = "https://via.placeholder.com/150",
            colors =
                TeamColors(
                    player = com.soccertips.predictx.data.model.lineups.PlayerColors(
                        primary = "#1565C0",
                        number = "#FFFFFF",
                        border = "#0D47A1"
                    ),
                    goalkeeper = com.soccertips.predictx.data.model.lineups.PlayerColors(
                        primary = "#D32F2F",
                        number = "#FFFFFF",
                        border = "#B71C1C"
                    )
                )
        ),
        formation = "3-2-4-1",
        startXI = List(11) {
            PlayerLineup(
                player = PlayerInfo(
                    id = it + 1,
                    name = "Player" + "${it + 1}",
                    number = it + 1,
                    pos = when (it) {
                        0 -> "G"
                        in 1..4 -> "D"
                        in 5..7 -> "M"
                        else -> "F"
                    },
                    grid = null
                )
            )
        },
        substitutes = List(7) {
            PlayerLineup(
                player = PlayerInfo(
                    id = it + 12,
                    name = "Substitute ${it + 1}",
                    number = it + 12,
                    pos = "M",
                    grid = null
                )
            )
        },
        coach = CoachInfo(
            id = 1,
            name = "Coach Name",
            photo = "https://via.placeholder.com/100"
        )
    )
    FixtureLineupsScreen(
        lineups = Pair(sampleLineup, sampleLineup)
    )
}



