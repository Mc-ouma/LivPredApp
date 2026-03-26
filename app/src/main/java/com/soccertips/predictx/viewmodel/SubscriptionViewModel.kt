package com.soccertips.predictx.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.soccertips.predictx.manager.SubscriptionManager
import com.soccertips.predictx.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val isSubscribed: StateFlow<Boolean> = subscriptionRepository.isSubscribed
    val productDetails: StateFlow<List<ProductDetails>> = subscriptionRepository.productDetails
    val billingConnectionState: StateFlow<Boolean> = subscriptionRepository.billingConnectionState

    init {
        subscriptionRepository.initialize()
    }

    fun purchaseSubscription(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        subscriptionManager.launchBillingFlow(activity, productDetails, offerToken) { success ->
            if (success) {
                subscriptionRepository.cacheSubscriptionStatus(true)
                Timber.d("Subscription purchase successful")
            }
        }
    }

    fun restorePurchases() {
        subscriptionRepository.refreshPurchases()
    }

    /**
     * Helper to get the monthly ProductDetails from the loaded list.
     */
    fun getMonthlyProduct(): ProductDetails? {
        return productDetails.value.find { it.productId == SubscriptionManager.PRODUCT_ID_MONTHLY }
    }

    /**
     * Helper to get the yearly ProductDetails from the loaded list.
     */
    fun getYearlyProduct(): ProductDetails? {
        return productDetails.value.find { it.productId == SubscriptionManager.PRODUCT_ID_YEARLY }
    }
}
