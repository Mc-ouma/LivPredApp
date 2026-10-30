package com.soccertips.predictx.data.model.lastfixtures

data class FixtureListResponse(
    val get: String? = null,
    val parameters: Any? = null,
    val errors: Any? = null,
    val results: Int? = null,
    val paging: Paging? = null,
    val response: List<FixtureDetails> = emptyList(),
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class FixtureDetails(
    val fixture: FixtureInfo,
    val league: LeagueInfo,
    val teams: TeamsInfo,
    val goals: Goals,
    val score: Score,
)

data class FixtureInfo(
    val id: Int,
    val referee: String?,
    val timezone: String,
    val date: String,
    val timestamp: Long,
    val venue: Venue,
    val status: Status,
)

data class LeagueInfo(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String,
    val season: Int,
    val round: String,
)

data class TeamsInfo(
    val home: TeamInfo,
    val away: TeamInfo,
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String,
    val winner: Boolean?,
)

data class Goals(
    val home: Int,
    val away: Int,
)

data class Score(
    val halftime: HalftimeScore,
    val fulltime: FulltimeScore,
    val extratime: ExtraTimeScore?,
    val penalty: PenaltyScore?,
)

data class HalftimeScore(
    val home: Int,
    val away: Int,
)

data class FulltimeScore(
    val home: Int,
    val away: Int,
)

data class ExtraTimeScore(
    val home: Int?,
    val away: Int?,
)

data class PenaltyScore(
    val home: Int?,
    val away: Int?,
)

data class Venue(
    val id: Int?,
    val name: String,
    val city: String?,
)

data class Status(
    val long: String,
    val short: String,
    val elapsed: Int,
    val extra: Int?,
)
