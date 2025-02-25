package com.soccertips.predcompose.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.soccertips.predcompose.data.model.team.transfer.Response2
import com.soccertips.predcompose.network.FixtureDetailsService
import kotlinx.coroutines.flow.Flow
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

    fun getTransfers(
        teamId: String
    ): Flow<PagingData<Response2>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { TransferPagingSource(teamId, teamsService) }
        ).flow
    }

    suspend fun getNextFixtures(
        season: String,
        teamId: String,
        next: String,
    ) = teamsService.getNextFixtures(season, teamId, next)


    suspend fun getTeamData(
        id: Int,
    ) = teamsService.getTeamData(id)
}