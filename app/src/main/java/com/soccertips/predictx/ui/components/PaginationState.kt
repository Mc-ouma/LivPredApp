package com.soccertips.predictx.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class PaginationState(
    initialPage: Int = 1,
    private val pageSize: Int = 20
) {
    var currentPage by mutableIntStateOf(initialPage)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasMorePages by mutableStateOf(true)
        private set

    fun loadNextPage() {
        if (!isLoading && hasMorePages) {
            currentPage++
        }
    }

    fun updateLoadingState(loading: Boolean) {
        isLoading = loading
    }

    fun updateHasMorePages(hasMore: Boolean) {
        hasMorePages = hasMore
    }

    fun reset() {
        currentPage = 1
        isLoading = false
        hasMorePages = true
    }

    companion object {
        fun Saver(): Saver<PaginationState, *> = Saver(
            save = { listOf(it.currentPage, it.pageSize) },
            restore = { PaginationState(it[0], it[1]) }
        )
    }
}

@Composable
fun rememberPaginationState(
    initialPage: Int = 1,
    pageSize: Int = 20
): PaginationState {
    return rememberSaveable(saver = PaginationState.Saver()) {
        PaginationState(initialPage, pageSize)
    }
}