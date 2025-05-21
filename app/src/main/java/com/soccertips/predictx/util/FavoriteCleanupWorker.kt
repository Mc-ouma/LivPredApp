package com.soccertips.predictx.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.data.local.entities.FavoriteItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class FavoriteCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val favoriteDao: FavoriteDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val currentTime = System.currentTimeMillis()
            val retentionPeriod = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

            val favorites = favoriteDao.getAllFavorites()
            var clearedCount = 0
            val itemsToUpdate = mutableListOf<FavoriteItem>()

            favorites.forEach { item ->
                val isMatchFinished = item.mStatus == "Match Finished" || item.mStatus == "FT"

                if (isMatchFinished) {
                    if (item.completedTimestamp == 0L) {
                        // First time seeing this completed match - mark completion time
                        itemsToUpdate.add(item.copy(completedTimestamp = currentTime))
                    } else if (currentTime - item.completedTimestamp > retentionPeriod) {
                        // Match is finished, timestamp is set, and retention period has passed
                        favoriteDao.deleteFavoriteItem(item.fixtureId)
                        clearedCount++
                    }
                }
                // No action for matches not yet finished or finished but within retention period (and timestamp already set)
            }

            // Batch update items that had their completedTimestamp set
            itemsToUpdate.forEach { updatedItem ->
                favoriteDao.updateFavoriteItem(updatedItem)
            }

            if (itemsToUpdate.isNotEmpty()) {
                Timber.Forest.d("Updated completedTimestamp for ${itemsToUpdate.size} matches.")
            }
            Timber.Forest.d("Cleaned up $clearedCount completed matches from favorites")
            return Result.success()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Error cleaning up favorites")
            return Result.retry()
        }
    }
}
