package com.soccertips.predcompose.repository

import com.soccertips.predcompose.network.FixtureDetailsService
import javax.inject.Inject

class TeamsRepository
@Inject
constructor(
    private val teamsService: FixtureDetailsService,
) {
    suspend fun getTeams(
        leagueId: String,
        season: String,
        teamId: String,
    ) = teamsService.getTeams(leagueId, season, teamId)

    suspend fun getPlayers(
        teamId: String,
    ) = teamsService.getPlayers(teamId)

    suspend fun getTransfers(
        teamId: String, page: Int
    ) = teamsService.getTransfers(teamId)

    suspend fun getNextFixtures(
        season: String,
        teamId: String,
        next: String,
    ) = teamsService.getNextFixtures(season, teamId, next)


    suspend fun getTeamData(
        id: Int,
    ) = teamsService.getTeamData(id)
}