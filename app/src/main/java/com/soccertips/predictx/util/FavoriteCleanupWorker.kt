package com.soccertips.predictx.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.data.local.dao.FavoriteDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class FavoriteCleanupWorker
@AssistedInject
constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val favoriteDao: FavoriteDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val currentTime = System.currentTimeMillis()
            val retentionPeriod = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

            // Get completed favorites that don't have completion timestamp set
            val favoritesToSetTimestamp = favoriteDao.getCompletedFavoritesWithoutTimestamp()

            // Get completed favorites that are older than retention period
            val cutoffTime = currentTime - retentionPeriod
            val favoritesToDelete = favoriteDao.getCompletedFavoritesOlderThan(cutoffTime)

            // Batch update items that need their completedTimestamp set
            if (favoritesToSetTimestamp.isNotEmpty()) {
                val updatedItems =
                        favoritesToSetTimestamp.map { item ->
                            item.copy(completedTimestamp = currentTime)
                        }
                favoriteDao.updateFavoriteItems(updatedItems)
                Timber.d("Updated completedTimestamp for ${updatedItems.size} matches.")
            }

            // Batch delete old completed items
            if (favoritesToDelete.isNotEmpty()) {
                val fixtureIdsToDelete = favoritesToDelete.map { it.fixtureId }
                favoriteDao.deleteFavoriteItems(fixtureIdsToDelete)
                Timber.d("Cleaned up ${fixtureIdsToDelete.size} completed matches from favorites")
            }

            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up favorites")
            return Result.retry()
        }
    }
}
