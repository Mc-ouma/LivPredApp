package com.soccertips.predictx.network

import com.soccertips.predictx.data.model.FixtureResponse
import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.data.model.events.FixtureEventsResponse
import com.soccertips.predictx.data.model.headtohead.HeadToHeadResponse
import com.soccertips.predictx.data.model.lastfixtures.FixtureListResponse
import com.soccertips.predictx.data.model.lineups.FixtureLineupResponse
import com.soccertips.predictx.data.model.prediction.PredictionResponse
import com.soccertips.predictx.data.model.standings.StandingsResponse
import com.soccertips.predictx.data.model.statistics.StatisticsResponse
import com.soccertips.predictx.data.model.team.squad.SquadResponse
import com.soccertips.predictx.data.model.team.teamscreen.TeamModelData
import com.soccertips.predictx.data.model.team.teamscreen.TeamStatisticsResponse
import com.soccertips.predictx.data.model.team.transfer.TransferResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getServerResponses(
        @Url url: String,
    ): RootResponse

}

interface FixtureDetailsService {
    @GET("fixtures")
    suspend fun getFixtureDetails(
        @Query("id") fixtureId: String,
    ): FixtureResponse

    @GET("fixtures")
    suspend fun getLastFixtures(
        @Query("season") season: String,
        @Query("team") teamId: String,
        @Query("last") last: String,
    ): FixtureListResponse

    @GET("fixtures")
    suspend fun getNextFixtures(
        @Query("season") season: String,
        @Query("team") teamId: String,
        @Query("next") next: String,
    ): FixtureListResponse

    @GET("fixtures/headtohead")
    suspend fun getHeadToHeadFixtures(
        @Query("h2h") teams: String,
        @Query("last") last: String,
    ): HeadToHeadResponse

    @GET("fixtures/lineups")
    suspend fun getLineups(
        @Query("fixture") fixtureId: String,
    ): FixtureLineupResponse

    @GET("fixtures/statistics")
    suspend fun getFixtureStats(
        @Query("fixture") fixtureId: String,
        @Query("team") teamId: String,
    ): StatisticsResponse

    @GET("fixtures/events")
    suspend fun getFixtureEvents(
        @Query("fixture") fixtureId: String,
    ): FixtureEventsResponse

    @GET("standings")
    suspend fun getStandings(
        @Query("league") leagueId: String,
        @Query("season") season: String,
    ): StandingsResponse

    @GET("predictions")
    suspend fun getPredictions(
        @Query("fixture") fixtureId: String,
    ): PredictionResponse


    @GET("teams/statistics")
    suspend fun getTeams(
        @Query("league") leagueId: String,
        @Query("season") season: String,
        @Query("team") teamId: String,
    ): TeamStatisticsResponse

    @GET("players/squads")
    suspend fun getPlayers(
        @Query("team") teamId: String,
    ): SquadResponse

    @GET("transfers")
    suspend fun getTransfers(
        @Query("team") teamId: String,
    ): TransferResponse

    @GET("teams")
    suspend fun getTeamData(
        @Query("id") teamId: Int,
    ): TeamModelData
}
