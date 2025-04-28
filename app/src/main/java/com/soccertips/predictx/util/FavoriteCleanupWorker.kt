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

            favorites.forEach { item ->
                if ((item.mStatus == "Match Finished" || item.mStatus == "FT") &&
                    item.completedTimestamp != null &&
                    currentTime - item.completedTimestamp > retentionPeriod) {

                    favoriteDao.deleteFavoriteItem(item.fixtureId)
                    clearedCount++
                } else if ((item.mStatus == "Match Finished" || item.mStatus == "FT") &&
                    item.completedTimestamp == null) {

                    // First time seeing this completed match - mark completion time
                    favoriteDao.updateFavoriteItem(item.copy(completedTimestamp = currentTime))
                }
            }

            Timber.Forest.d("Cleaned up $clearedCount completed matches from favorites")
            return Result.success()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Error cleaning up favorites")
            return Result.retry()
        }
    }
}
