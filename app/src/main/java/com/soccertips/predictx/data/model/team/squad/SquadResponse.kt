package com.soccertips.predictx.data.model.team.squad

data class SquadResponse(
    val errors: Any? = null,
    val `get`: String? = null,
    val paging: Paging? = null,
    val parameters: Any? = null,
    val response: List<Response> = emptyList(),
    val results: Int? = null
)

data class Paging(
    val current: Int? = null,
    val total: Int? = null
)

data class Response(
    val players: List<Player> = emptyList(),
    val team: Team? = null
)

data class Player(
    val age: Int? = null,
    val id: Int? = null,
    val name: String? = null,
    val number: Int? = null,
    val photo: String? = null,
    val position: String? = null
)

data class Team(
    val id: Int? = null,
    val logo: String? = null,
    val name: String? = null
)