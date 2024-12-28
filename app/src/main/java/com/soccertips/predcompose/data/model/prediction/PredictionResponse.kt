package com.soccertips.predcompose.data.model.prediction

data class PredictionResponse(
    val get: String,
    val parameters: Parameters,
    val errors: List<String>,
    val results: Int,
    val paging: Paging,
    val response: List<Response>
)

data class Parameters(
    val fixture: String
)

data class Paging(
    val current: Int,
    val total: Int
)

data class Response(
    val predictions: Predictions,
    val league: League,
    val teams: Teams,
    val comparison: Comparison,
    val h2h: List<H2H>
)

data class Predictions(
    val winner: Winner,
    val win_or_draw: Boolean,
    val under_over: String,
    val goals: Goals,
    val advice: String,
    val percent: Percent
)

data class Winner(
    val id: Int,
    val name: String,
    val comment: String
)

data class Goals(
    val home: String,
    val away: String
)

data class Percent(
    val home: String,
    val draw: String,
    val away: String
)

data class League(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String,
    val season: Int
)

data class Teams(
    val home: Team,
    val away: Team
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String,
    val last_5: Last5,
    val league: TeamLeague
)

data class Last5(
    val form: String,
    val att: String,
    val def: String,
    val goals: Last5Goals
)

data class Last5Goals(
    val `for`: GoalData,
    val against: GoalData
)

data class GoalData(
    val total: Int,
    val average: Double
)

data class TeamLeague(
    val form: String,
    val fixtures: Fixtures,
    val goals: TeamGoals,
    val biggest: Biggest,
    val clean_sheet: CleanSheet,
    val failed_to_score: FailedToScore
)

data class Fixtures(
    val played: FixtureDetail,
    val wins: FixtureDetail,
    val draws: FixtureDetail,
    val loses: FixtureDetail
)

data class FixtureDetail(
    val home: Int,
    val away: Int,
    val total: Int
)

data class TeamGoals(
    val `for`: GoalStats,
    val against: GoalStats
)

data class GoalStats(
    val total: GoalTotal,
    val average: GoalAverage
)

data class GoalTotal(
    val home: Int,
    val away: Int,
    val total: Int
)

data class GoalAverage(
    val home: Double,
    val away: Double,
    val total: Double
)

data class Biggest(
    val streak: Streak,
    val wins: WinLoss,
    val loses: WinLoss,
    val goals: BiggestGoals
)

data class Streak(
    val wins: Int,
    val draws: Int,
    val loses: Int
)

data class WinLoss(
    val home: String,
    val away: String
)

data class BiggestGoals(
    val `for`: HomeAway,
    val against: HomeAway
)

data class HomeAway(
    val home: Int,
    val away: Int
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

data class Comparison(
    val form: ComparisonData,
    val att: ComparisonData,
    val def: ComparisonData,
    val poisson_distribution: ComparisonData,
    val h2h: ComparisonData,
    val goals: ComparisonData,
    val total: ComparisonData
)

data class ComparisonData(
    val home: String,
    val away: String
)

data class H2H(
    val fixture: Fixture,
    val league: League,
    val teams: TeamsShort,
    val goals: HomeAway,
    val score: Score
)

data class Fixture(
    val id: Int,
    val referee: String?,
    val timezone: String,
    val date: String,
    val timestamp: Long,
    val periods: Periods,
    val venue: Venue,
    val status: Status
)

data class Periods(
    val first: Long,
    val second: Long
)

data class Venue(
    val id: Int?,
    val name: String,
    val city: String?
)

data class Status(
    val long: String,
    val short: String,
    val elapsed: Int,
    val extra: Int?
)

data class TeamsShort(
    val home: TeamShort,
    val away: TeamShort
)

data class TeamShort(
    val id: Int,
    val name: String,
    val logo: String,
    val winner: Boolean?
)

data class Score(
    val halftime: HomeAway,
    val fulltime: HomeAway,
    val extratime: HomeAway?,
    val penalty: HomeAway?
)


