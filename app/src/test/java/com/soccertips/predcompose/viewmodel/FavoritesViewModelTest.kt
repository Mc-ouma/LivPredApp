package com.soccertips.predcompose.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.turbine.test
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.util.WorkManagerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate


@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private lateinit var viewModel: FavoritesViewModel
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var workManagerWrapper: WorkManagerWrapper
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        favoriteDao = mock<FavoriteDao>()
        workManagerWrapper = mock<WorkManagerWrapper>()
        viewModel = FavoritesViewModel(favoriteDao, workManagerWrapper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFavorites should emit Loading and Success states`() = runTest {
        // Arrange
        val favoriteItems = listOf(
            FavoriteItem(
                fixtureId = "1", mDate = "2023-10-01", mTime = "10:00",
                homeTeam = "Arsenal", awayTeam = "Everton", league = "EPL",
                mStatus = "Finished", outcome = "2", pick = "2",
                color = Color.Red.toArgb(),
                hLogoPath = "https://example.com/arsenal.png",
                aLogoPath = "https://example.com/everton.png",
                leagueLogo = "https://example.com/epl.png"
            ),
            FavoriteItem(
                fixtureId = "2",
                mDate = "2023-10-02",
                mTime = "11:00",
                color = Color.Red.toArgb(),
                pick = "1",
                outcome = "1",
                mStatus = "Finished",
                league = "EPL",
                awayTeam = "Everton",
                homeTeam = "Arsenal",
                hLogoPath = "https://example.com/arsenal.png",
                aLogoPath = "https://example.com/everton.png",
                leagueLogo = "https://example.com/epl.png"
            )
        )
        whenever(favoriteDao.getAllFavorites()).thenReturn(favoriteItems)
        whenever(workManagerWrapper.getWorkInfosForUniqueWork(any())).thenReturn(emptyList())

        // Act & Assert
        viewModel.uiState.test {
            // Assert initial state (Loading)
            val initialState = awaitItem()
            assert(initialState is UiState.Loading) {
                "Initial state should be Loading"
            }

            // Assert Loading state (emitted again by loadFavorites)
            viewModel.loadFavorites()
            val loadingState = awaitItem()
            assert(loadingState is UiState.Loading) {
                "Expected UiState.Loading State"
            }

            // Assert Success state with sorted items
            val successState = awaitItem() as UiState.Success<List<FavoriteItem>>
            assert(successState.data == favoriteItems.sortedBy { it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN })

            // Ensure no more emissions
            awaitComplete()
        }
    }

    @Test
    fun `loadFavorites should emit Error state on exception`() = runTest {
        // Arrange
        val errorMessage = "Database error"
        whenever(favoriteDao.getAllFavorites()).thenThrow(RuntimeException(errorMessage))

        // Act & Assert
        viewModel.uiState.test {
            // Assert Loading state
            viewModel.loadFavorites()
            assert(awaitItem() is UiState.Loading)

            // Assert Error state
            val errorState = awaitItem() as UiState.Error
            assert(errorState.message == errorMessage)

            // Ensure no more emissions
            awaitComplete()
        }
    }

    @Test
    fun `restoreFavorites should insert item and update UI state`() = runTest {
        // Arrange
        val item = FavoriteItem(
            fixtureId = "1", mDate = "2023-10-01", mTime = "10:00",
            homeTeam = "Arsenal", awayTeam = "Everton", league = "EPL",
            mStatus = "Finished", outcome = "2", pick = "2",
            color = Color.Red.toArgb(),
            hLogoPath = "https://example.com/arsenal.png",
            aLogoPath = "https://example.com/everton.png",
            leagueLogo = "https://example.com/epl.png"
        )
        whenever(favoriteDao.insert(item)).thenReturn(Unit)
        whenever(favoriteDao.getAllFavorites()).thenReturn(listOf(item))

        // Act & Assert
        viewModel.uiState.test {
            viewModel.restoreFavorites(item)

            // Assert Success state with the restored item
            val successState = awaitItem() as UiState.Success
            assert(successState.data == listOf(item))

            // Ensure no more emissions
            awaitComplete()
        }

        // Verify DAO interaction
        verify(favoriteDao).insert(item)
    }

    @Test
    fun `removeFromFavorites should delete item and update UI state`() = runTest {
        // Arrange
        val item = FavoriteItem(
            fixtureId = "1", mDate = "2023-10-01", mTime = "10:00",
            homeTeam = "Arsenal", awayTeam = "Everton", league = "EPL",
            mStatus = "Finished", outcome = "2", pick = "2",
            color = Color.Red.toArgb(),
            hLogoPath = "https://example.com/arsenal.png",
            aLogoPath = "https://example.com/everton.png",
            leagueLogo = "https://example.com/epl.png"
        )
        whenever(favoriteDao.delete(item.fixtureId.toString())).thenReturn(Unit)
        whenever(favoriteDao.getAllFavorites()).thenReturn(emptyList())

        // Act & Assert
        viewModel.uiState.test {
            viewModel.removeFromFavorites(item)

            // Assert Success state with the item removed
            val successState = awaitItem() as UiState.Success
            assert(successState.data.isEmpty())

            // Ensure no more emissions
            awaitComplete()
        }

        // Verify DAO interaction
        verify(favoriteDao).delete(item.fixtureId.toString())
    }

    @Test
    fun `scheduleNotification should enqueue unique periodic work`() = runTest {
        // Arrange
        val item = FavoriteItem(
            fixtureId = "1", mDate = "2023-10-01", mTime = "10:00",
            homeTeam = "Arsenal", awayTeam = "Everton", league = "EPL",
            mStatus = "Finished", outcome = "2", pick = "2",
            color = Color.Red.toArgb(),
            hLogoPath = "https://example.com/arsenal.png",
            aLogoPath = "https://example.com/everton.png",
            leagueLogo = "https://example.com/epl.png"
        )
        whenever(favoriteDao.getAllFavorites()).thenReturn(listOf(item))
        whenever(workManagerWrapper.getWorkInfosForUniqueWork("checkDueItems_1")).thenReturn(
            emptyList()
        )

        // Act
        viewModel.loadFavorites()

        // Verify WorkManager interaction
        verify(workManagerWrapper).enqueueUniquePeriodicWork(
            "checkDueItems_1",
            any() // Use any() to ignore the specific PeriodicWorkRequest
        )
    }

    @Test
    fun `cancelNotification should cancel unique work`() = runTest {
        // Arrange
        val item = FavoriteItem(
            fixtureId = "1", mDate = "2023-10-01", mTime = "10:00",
            homeTeam = "Arsenal", awayTeam = "Everton", league = "EPL",
            mStatus = "Finished", outcome = "2", pick = "2",
            color = Color.Red.toArgb(),
            hLogoPath = "https://example.com/arsenal.png",
            aLogoPath = "https://example.com/everton.png",
            leagueLogo = "https://example.com/epl.png"
        )

        // Act
        viewModel.cancelNotification(item.fixtureId.toString())

        // Verify WorkManager interaction
        verify(workManagerWrapper).cancelUniqueWork("checkDueItems_1")
    }

    @Test
    fun `favoriteCount should emit count from DAO`() = runTest {
        // Arrange
        val count = 5
        whenever(favoriteDao.getFavoriteCount()).thenReturn(flowOf(count))

        // Act & Assert
        viewModel.favoriteCount.test {
            // Assert emitted count
            assert(awaitItem() == count)

            // Ensure no more emissions
            awaitComplete()
        }
    }
}