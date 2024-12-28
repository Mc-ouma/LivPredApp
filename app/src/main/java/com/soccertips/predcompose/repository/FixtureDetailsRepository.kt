package com.soccertips.predcompose.repository

import com.soccertips.predcompose.data.model.ResponseData
import com.soccertips.predcompose.data.model.events.FixtureEventsResponse
import com.soccertips.predcompose.data.model.headtohead.HeadToHeadResponse
import com.soccertips.predcompose.data.model.lastfixtures.FixtureListResponse
import com.soccertips.predcompose.data.model.lineups.FixtureLineupResponse
import com.soccertips.predcompose.data.model.prediction.PredictionResponse
import com.soccertips.predcompose.data.model.standings.StandingsResponse
import com.soccertips.predcompose.data.model.statistics.StatisticsResponse
import com.soccertips.predcompose.network.FixtureDetailsService
import javax.inject.Inject

class FixtureDetailsRepository
@Inject
constructor(
    private val fixtureDetailsService: FixtureDetailsService,
) {
    // suspend fun getFixtureDetails(fixtureId: String): ResponseData = fixtureDetailsService.getFixtureDetails(fixtureId)
    suspend fun getFixtureDetails(fixtureId: String): ResponseData {
        val response = fixtureDetailsService.getFixtureDetails(fixtureId)
        return response.response.firstOrNull() ?: throw Exception("No response found")
    }

    suspend fun getHeadToHeadFixtures(
        teams: String,
        last: String,
    ): HeadToHeadResponse = fixtureDetailsService.getHeadToHeadFixtures(teams, last)

    suspend fun getLineups(fixtureId: String): FixtureLineupResponse =
        fixtureDetailsService.getLineups(fixtureId)

    suspend fun getFixtureStats(
        fixtureId: String,
        teamId: String,
    ): StatisticsResponse = fixtureDetailsService.getFixtureStats(fixtureId, teamId)

    suspend fun getFixtureEvents(fixtureId: String): FixtureEventsResponse =
        fixtureDetailsService.getFixtureEvents(fixtureId)

    suspend fun getStandings(
        leagueId: String,
        season: String,
    ): StandingsResponse = fixtureDetailsService.getStandings(leagueId, season)

    suspend fun getPredictions(fixtureId: String): PredictionResponse =
        fixtureDetailsService.getPredictions(fixtureId)

    suspend fun getLastFixtures(
        season: String,
        teamId: String,
        last: String,
    ): FixtureListResponse = fixtureDetailsService.getLastFixtures(season, teamId, last)

}
