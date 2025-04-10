package com.soccertips.predictx.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.soccertips.predictx.data.model.team.transfer.Response2
import com.soccertips.predictx.network.FixtureDetailsService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferPagingSource(
    private val teamId: String,
    private val teamsService: FixtureDetailsService
) : PagingSource<Int, Response2>() {

    private var allData: List<Response2> = emptyList()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Response2> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize

        return try {
            if (allData.isEmpty()) {
                val response = teamsService.getTransfers(teamId)
                allData = response.response.sortedByDescending { transferResponse ->
                    transferResponse.transfers.firstOrNull()?.date?.let { dateString ->
                        try {
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            format.parse(dateString) ?: Date(0) // Default to epoch if parsing fails
                        } catch (e: Exception) {
                            Date(0) // Default to epoch if parsing fails
                        }
                    } ?: Date(0) // Default to epoch if date is null
                }
            }

            val startIndex = pageNumber * pageSize
            val endIndex = minOf(startIndex + pageSize, allData.size)

            val pageData = if (startIndex < allData.size) {
                allData.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            LoadResult.Page(
                data = pageData,
                prevKey = if (pageNumber > 0) pageNumber - 1 else null,
                nextKey = if (endIndex < allData.size) pageNumber + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Response2>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}