package com.soccertips.predcompose.network

import com.soccertips.predcompose.model.FixtureResponse
import com.soccertips.predcompose.model.RootResponse
import com.soccertips.predcompose.model.events.FixtureEventsResponse
import com.soccertips.predcompose.model.headtohead.HeadToHeadResponse
import com.soccertips.predcompose.model.lastfixtures.FixtureListResponse
import com.soccertips.predcompose.model.lineups.FixtureLineupResponse
import com.soccertips.predcompose.model.prediction.PredictionResponse
import com.soccertips.predcompose.model.standings.StandingsResponse
import com.soccertips.predcompose.model.statistics.StatisticsResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import timber.log.Timber

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
    suspend fun getFixturesFormHome(
        @Query("season") season: String,
        @Query("team") teamId: String,
        @Query("last") last: String,
    ): FixtureListResponse

    @GET("fixtures")
    suspend fun getFixturesFormAway(
        @Query("season") season: String,
        @Query("team") leagueId: String,
        @Query("last") last: String,
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
}
