package com.soccertips.predcompose.data.model.statistics

data class StatisticsResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<Any>,
    val results: Int,
    val paging: Paging,
    val response: List<Response>,
)

data class Parameters(
    val team: String,
    val fixture: String,
)

data class Paging(
    val current: Int,
    val total: Int,
)

data class Response(
    val team: Team,
    val statistics: List<Statistic>,
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String,
)

data class Statistic(
    val type: String,
    val value: Any?,
)
