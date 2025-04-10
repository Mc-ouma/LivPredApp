package com.soccertips.predictx.data.model.events

data class FixtureEventsResponse(
    val get: String,
    val parameters: FixtureParameters,
    val errors: List<String>,
    val results: Int,
    val paging: Paging,
    val response: List<FixtureEvent>,
)

data class FixtureParameters(
    val fixture: String,
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class FixtureEvent(
    val time: EventTime,
    val team: TeamInfo,
    val player: PlayerInfo,
    val assist: AssistInfo?,
    val type: String,
    val detail: String,
    val comments: String?,
)

data class EventTime(
    val elapsed: Int,
    val extra: Int?,
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String,
)

data class PlayerInfo(
    val id: Int?,
    val name: String,
)

data class AssistInfo(
    val id: Int?,
    val name: String?,
)
