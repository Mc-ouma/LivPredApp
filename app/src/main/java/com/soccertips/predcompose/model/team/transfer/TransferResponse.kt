package com.soccertips.predcompose.model.team.transfer

data class TransferResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<Any>,
    val results: Int,
    val paging: Paging,
    val response: List<Response2>
)

data class Parameters(
    val team: String
)

data class Paging(
    val current: Int,
    val total: Int
)

data class Response2(
    val player: Player2,
    val update: String,
    val transfers: List<Transfer>
)

data class Player2(
    val id: Int,
    val name: String
)

data class Transfer(
    val date: String,
    val type: String?,
    val teams: Teams
)

data class Teams(
    val `in`: Team,
    val `out`: Team
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String
)