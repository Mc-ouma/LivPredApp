package com.soccertips.predictx.data.model.team.teamscreen

data class TeamModelData(
    val errors: List<Any>,
    val `get`: String,
    val paging: Paging,
    val parameters: Parameters,
    val response: List<Response>,
    val results: Int
)
data class Response(
    val team: TeamData,
    val venue: Venue
)
data class TeamData(
    val code: String,
    val country: String,
    val founded: Int,
    val id: Int,
    val logo: String,
    val name: String,
    val national: Boolean
)
data class Venue(
    val address: String,
    val capacity: Int,
    val city: String,
    val id: Int,
    val image: String,
    val name: String,
    val surface: String
)