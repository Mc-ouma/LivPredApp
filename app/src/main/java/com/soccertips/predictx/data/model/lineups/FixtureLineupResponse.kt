package com.soccertips.predictx.data.model.lineups

data class FixtureLineupResponse(
    val get: String,
    val parameters: FixtureParameters,
    val errors: List<String>,
    val results: Int,
    val paging: Paging,
    val response: List<TeamLineup>,
)

data class FixtureParameters(
    val fixture: String,
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class TeamLineup(
    val team: TeamInfo,
    val formation: String,
    val startXI: List<PlayerLineup>,
    val substitutes: List<PlayerLineup>,
    val coach: CoachInfo,
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String,
    val colors: TeamColors,
)

data class TeamColors(
    val player: PlayerColors,
    val goalkeeper: PlayerColors,
)

data class PlayerColors(
    val primary: String,
    val number: String,
    val border: String,
)

data class PlayerLineup(
    val player: PlayerInfo,
)

data class PlayerInfo(
    val id: Int,
    val name: String,
    val number: Int,
    val pos: String,
    val grid: String?,
)

data class CoachInfo(
    val id: Int,
    val name: String,
    val photo: String,
)
