package com.soccertips.predictx.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.soccertips.predictx.data.local.entities.FavoriteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * BroadcastReceiver for handling match reminder notifications.
 *
 * This receiver listens for the custom action "com.soccertips.predictx.ACTION_MATCH_REMINDER"
 * to comply with Android 15's requirement that intents must have actions that match intent-filters.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // First check if the action matches what we expect
        if (intent.action != "com.soccertips.predictx.ACTION_MATCH_REMINDER") {
            Timber.w("Received intent with unexpected action: ${intent.action}")
            return
        }

        val fixtureId = intent.getStringExtra("fixtureId") ?: return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val favoriteItem = FavoriteItem(
                    fixtureId = fixtureId,
                    homeTeam = intent.getStringExtra("homeTeam"),
                    awayTeam = intent.getStringExtra("awayTeam"),
                    mDate = intent.getStringExtra("mDate"),
                    mTime = intent.getStringExtra("mTime"),
                    hLogoPath = intent.getStringExtra("hLogoPath"),
                    aLogoPath = intent.getStringExtra("aLogoPath"),
                    league = intent.getStringExtra("league"),
                    mStatus = intent.getStringExtra("mStatus"),
                    outcome = intent.getStringExtra("outcome"),
                    pick = intent.getStringExtra("pick"),
                    color = intent.getIntExtra("color", 0),
                    leagueLogo = intent.getStringExtra("leagueLogo"),
                    completedTimestamp = intent.getLongExtra("completedTimestamp", 0L)
                )

                showNotification(context, favoriteItem)
            } catch (e: Exception) {
                Timber.e(e, "Failed to show notification for fixture $fixtureId")
            }
        }
    }

    private suspend fun showNotification(context: Context, item: FavoriteItem) {
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationBuilder = NotificationBuilder(context)

        val notification = notificationBuilder.buildMatchNotification(item).build()

        try {
            notificationManager.notify(item.fixtureId.hashCode(), notification)
        } catch (e: SecurityException) {
            Timber.e(e, "Notification permission not granted")
        }
    }
}

