package com.soccertips.predictx.manager

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_MONTHLY = "remove_ads_monthly"
        const val PRODUCT_ID_YEARLY = "remove_ads_yearly"
        private const val TAG = "SubscriptionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var billingClient: BillingClient? = null
    private var isConnecting = false

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _billingConnectionState = MutableStateFlow(false)
    val billingConnectionState: StateFlow<Boolean> = _billingConnectionState.asStateFlow()

    private var onPurchaseComplete: ((Boolean) -> Unit)? = null

    fun initialize() {
        if (billingClient?.isReady == true) {
            scope.launch { queryPurchasesInternal() }
            return
        }
        if (isConnecting) return

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        startConnection()
    }

    private fun startConnection() {
        isConnecting = true
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.tag(TAG).d("Billing client connected")
                    _billingConnectionState.value = true
                    scope.launch {
                        queryProductDetailsInternal()
                        queryPurchasesInternal()
                    }
                } else {
                    Timber.tag(TAG).e("Billing setup failed: ${billingResult.debugMessage}")
                    _billingConnectionState.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                _billingConnectionState.value = false
                Timber.tag(TAG).w("Billing service disconnected")
            }
        })
    }

    private fun ensureConnected(action: () -> Unit) {
        if (billingClient?.isReady == true) {
            action()
        } else {
            startConnection()
        }
    }

    private suspend fun queryProductDetailsInternal() {
        val client = billingClient ?: return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList ?: emptyList()
            Timber.tag(TAG).d("Product details loaded: ${_productDetails.value.size} products")
        } else {
            Timber.tag(TAG).e("Failed to query product details: ${result.billingResult.debugMessage}")
        }
    }

    private suspend fun queryPurchasesInternal() {
        val client = billingClient ?: return

        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _purchases.value = result.purchasesList
            val hasActiveSubscription = result.purchasesList.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _isSubscribed.value = hasActiveSubscription
            Timber.tag(TAG).d("Purchases queried: subscribed=$hasActiveSubscription")

            // Acknowledge any unacknowledged purchases
            result.purchasesList.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
            }.forEach { acknowledgePurchase(it) }
        } else {
            Timber.tag(TAG).e("Failed to query purchases: ${result.billingResult.debugMessage}")
        }
    }

    fun queryPurchases() {
        ensureConnected {
            scope.launch { queryPurchasesInternal() }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
        onComplete: (Boolean) -> Unit
    ) {
        onPurchaseComplete = onComplete
        launchBillingFlow(activity, productDetails, offerToken)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { purchaseList ->
                    _purchases.value = purchaseList
                    val hasActive = purchaseList.any {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    _isSubscribed.value = hasActive

                    purchaseList.filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }.forEach { acknowledgePurchase(it) }

                    onPurchaseComplete?.invoke(hasActive)
                    onPurchaseComplete = null
                }
                Timber.tag(TAG).d("Purchase updated successfully")
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.tag(TAG).d("User cancelled purchase")
                onPurchaseComplete?.invoke(false)
                onPurchaseComplete = null
            }

            else -> {
                Timber.tag(TAG).e("Purchase failed: ${billingResult.debugMessage}")
                onPurchaseComplete?.invoke(false)
                onPurchaseComplete = null
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.tag(TAG).d("Purchase acknowledged")
            } else {
                Timber.tag(TAG).e("Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
