package com.soccertips.predcompose.model

data class FixtureResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<Any>,
    val results: Int,
    val paging: Paging,
    val response: List<ResponseData>,
)

data class Parameters(
    val id: String,
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class ResponseData(
    val fixture: Fixture,
    val league: League,
    val teams: Teams,
    val goals: Goals,
    val score: Score,
    val events: List<Event>,
    val lineups: List<Lineup>,
    val statistics: List<Any>,
    val players: List<Any>,
)

data class Fixture(
    val id: Int,
    val referee: String,
    val timezone: String,
    val date: String,
    val timestamp: Long,
    val periods: Periods,
    val venue: Venue,
    val status: Status,
)

data class Periods(
    val first: Long,
    val second: Long,
)

data class Venue(
    val id: Int,
    val name: String,
    val city: String,
)

data class Status(
    val long: String,
    val short: String,
    val elapsed: Int,
    val extra: Any?,
)

data class League(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String,
    val season: Int,
    val round: String,
)

data class Teams(
    val home: Team,
    val away: Team,
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String,
    val winner: Boolean,
)

data class Goals(
    val home: Int,
    val away: Int,
)

data class Score(
    val halftime: HalfTime,
    val fulltime: FullTime,
    val extratime: Any?,
    val penalty: Any?,
)

data class HalfTime(
    val home: Int,
    val away: Int,
)

data class FullTime(
    val home: Int,
    val away: Int,
)

data class Event(
    val time: EventTime,
    val team: Team,
    val player: Player,
    val assist: Assist?,
    val type: String,
    val detail: String,
    val comments: Any?,
)

data class EventTime(
    val elapsed: Int,
    val extra: Any?,
)

data class Player(
    val id: Int,
    val name: String,
)

data class Assist(
    val id: Int?,
    val name: String?,
)

data class Lineup(
    val team: Team,
    val coach: Coach,
    val formation: String,
    val startXI: List<PlayerDetail>,
    val substitutes: List<PlayerDetail>,
)

data class Coach(
    val id: Int,
    val name: String,
    val photo: String,
)

data class PlayerDetail(
    val player: PlayerInfo,
)

data class PlayerInfo(
    val id: Int,
    val name: String,
    val number: Int,
    val pos: String?,
    val grid: String?,
)
