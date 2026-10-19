package com.soccertips.predictx.data.model.statistics

data class StatisticsResponse(
    val get: String? = null,
    val parameters: Any? = null,
    val errors: Any? = null,
    val results: Int? = null,
    val paging: Paging? = null,
    val response: List<Response> = emptyList(),
)

data class Paging(
    val current: Int? = null,
    val total: Int? = null,
)

data class Response(
    val team: Team,
    val statistics: List<Statistic>,
)

data class Team(
    val id: Int = 0,
    val name: String = "",
    val logo: String = "",
)

data class Statistic(
    val type: String = "",
    val value: Any? = null,
)
