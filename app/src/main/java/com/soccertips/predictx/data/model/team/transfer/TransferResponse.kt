package com.soccertips.predictx.data.model.team.transfer

data class TransferResponse(
    val get: String? = null,
    val parameters: Parameters? = null,
    val errors: List<Any>? = null,
    val results: Int? = null,
    val paging: Paging? = null,
    val response: List<Response2> = emptyList()
)

data class Parameters(
    val team: String? = null
)

data class Paging(
    val current: Int? = null,
    val total: Int? = null
)

data class Response2(
    val player: Player2? = null,
    val update: String? = null,
    val transfers: List<Transfer> = emptyList()
)

data class Player2(
    val id: Int? = null,
    val name: String? = null
)

data class Transfer(
    val date: String? = null,
    val type: String? = null,
    val teams: Teams? = null
)

data class Teams(
    val `in`: Team? = null,
    val `out`: Team? = null
)

data class Team(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null
)