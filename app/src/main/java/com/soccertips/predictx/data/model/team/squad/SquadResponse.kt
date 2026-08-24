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
    val players: List<Player>,
    val team: Team
)

data class Player(
    val age: Int,
    val id: Int,
    val name: String,
    val number: Int,
    val photo: String,
    val position: String
)

data class Team(
    val id: Int,
    val logo: String,
    val name: String
)