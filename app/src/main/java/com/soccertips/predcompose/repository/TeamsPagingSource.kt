package com.soccertips.predcompose.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.soccertips.predcompose.data.model.team.transfer.Response2
import com.soccertips.predcompose.network.FixtureDetailsService

class TransferPagingSource(
    private val teamId: String,
    private val teamsService: FixtureDetailsService
) : PagingSource<Int, Response2>() {
    override fun getRefreshKey(state: PagingState<Int, Response2>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Response2> {

        return try {
            val page = params.key ?: 1
            val response = teamsService.getTransfers(
                teamId,
                page
            )
            LoadResult.Page(
                data = response.response,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.response.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}