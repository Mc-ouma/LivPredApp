package com.soccertips.predcompose.model.standings

data class StandingsResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<String>,
    val results: Int,
    val paging: Paging,
    val response: List<LeagueWrapper>,
)

data class Parameters(
    val league: String,
    val season: String,
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class LeagueWrapper(
    val league: LeagueDetails,
)

data class LeagueDetails(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String,
    val season: Int,
    val standings: List<List<TeamStanding>>,
)

data class TeamStanding(
    val rank: Int,
    val team: TeamInfo,
    val points: Int,
    val goalsDiff: Int,
    val group: String,
    val form: String,
    val status: String,
    val description: String?,
    val all: OverallRecord,
    val home: HomeAwayRecord,
    val away: HomeAwayRecord,
    val update: String,
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String,
)

data class OverallRecord(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goals: Goals,
)

data class HomeAwayRecord(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goals: Goals,
)

data class Goals(
    val `for`: Int,
    val against: Int,
)
