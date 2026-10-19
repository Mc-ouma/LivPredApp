package com.soccertips.predictx.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.soccertips.predictx.manager.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val sharedPreferences: SharedPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val KEY_IS_SUBSCRIBED = "is_subscribed"
        private const val TAG = "SubscriptionRepo"
    }

    /**
     * Whether the user currently has an active subscription.
     * This is the single source of truth for ad-free status across the app.
     */
    val isSubscribed: StateFlow<Boolean> = subscriptionManager.isSubscribed
        .stateIn(scope, SharingStarted.Eagerly, getCachedSubscriptionStatus())

    val productDetails = subscriptionManager.productDetails
    val billingConnectionState = subscriptionManager.billingConnectionState

    fun initialize() {
        subscriptionManager.initialize()
    }

    fun refreshPurchases() {
        subscriptionManager.queryPurchases()
    }

    /**
     * Quick synchronous check using cached value.
     * Use this in hot paths like ad loading where you can't suspend.
     */
    fun isSubscribedSync(): Boolean {
        return isSubscribed.value
    }

    /**
     * Cache subscription status locally for fast startup checks.
     */
    fun cacheSubscriptionStatus(isSubscribed: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_SUBSCRIBED, isSubscribed) }
        Timber.tag(TAG).d("Cached subscription status: $isSubscribed")
    }

    private fun getCachedSubscriptionStatus(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_SUBSCRIBED, false)
    }

    fun destroy() {
        subscriptionManager.destroy()
    }
}
