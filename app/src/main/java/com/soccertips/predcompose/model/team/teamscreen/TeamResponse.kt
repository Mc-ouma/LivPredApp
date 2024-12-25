package com.soccertips.predcompose.model.team.teamscreen

data class TeamStatisticsResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<String>,
    val results: Int,
    val paging: Paging,
    val response: TeamStatistics
)

data class Parameters(
    val league: String,
    val season: String,
    val team: String
)

data class Paging(
    val current: Int,
    val total: Int
)

data class TeamStatistics(
    val league: League,
    val team: Team,
    val form: String,
    val fixtures: Fixtures,
    val goals: Goals,
    val biggest: Biggest,
    val clean_sheet: CleanSheet,
    val failed_to_score: FailedToScore,
    val penalty: Penalty,
    val lineups: List<Lineup>,
    val cards: Cards
)

data class League(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String,
    val season: Int
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String
)

data class Fixtures(
    val played: Played,
    val wins: Played,
    val draws: Played,
    val loses: Played
)

data class Played(
    val home: Int,
    val away: Int,
    val total: Int
)

data class Goals(
    val `for`: ForAgainstGoals,
    val against: ForAgainstGoals
)

data class ForAgainstGoals(
    val total: Played,
    val average: AverageGoals,
    val minute: Map<String, GoalMinute>,
    val under_over: Map<String, UnderOver>
)

data class AverageGoals(
    val home: String,
    val away: String,
    val total: String
)

data class GoalMinute(
    val total: Int?,
    val percentage: String?
)

data class UnderOver(
    val under: Int,
    val over: Int
)

data class Biggest(
    val streak: Streak,
    val wins: HomeAway,
    val loses: HomeAway,
    val goals: HomeAwayGoals
)

data class Streak(
    val wins: Int,
    val draws: Int,
    val loses: Int
)

data class HomeAway(
    val home: String,
    val away: String
)

data class HomeAwayGoals(
    val `for`: HomeAway,
    val against: HomeAway
)

data class CleanSheet(
    val home: Int,
    val away: Int,
    val total: Int
)

data class FailedToScore(
    val home: Int,
    val away: Int,
    val total: Int
)

data class Penalty(
    val scored: PenaltyDetails,
    val missed: PenaltyDetails,
    val total: Int
)

data class PenaltyDetails(
    val total: Int,
    val percentage: String
)

data class Lineup(
    val formation: String,
    val played: Int
)

data class Cards(
    val yellow: Map<String, CardDetail>,
    val red: Map<String, CardDetail>
)

data class CardDetail(
    val total: Int?,
    val percentage: String?
)
