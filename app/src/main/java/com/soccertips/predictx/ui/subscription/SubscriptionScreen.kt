package com.soccertips.predictx.ui.subscription

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.billingclient.api.ProductDetails
import com.soccertips.predictx.R
import com.soccertips.predictx.manager.SubscriptionManager
import com.soccertips.predictx.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val products by viewModel.productDetails.collectAsState()
    val isConnected by viewModel.billingConnectionState.collectAsState()
    val activity = LocalActivity.current as Activity

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.subscription_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSubscribed) {
                ActiveSubscriptionContent()
            } else {
                PaywallContent(
                    products = products,
                    isConnected = isConnected,
                    onPurchase = { productDetails, offerToken ->
                        viewModel.purchaseSubscription(activity, productDetails, offerToken)
                    },
                    onRestore = { viewModel.restorePurchases() }
                )
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionContent() {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = Icons.Outlined.Verified,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.subscription_active_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.subscription_active_description),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.subscription_manage_hint),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PaywallContent(
    products: List<ProductDetails>,
    isConnected: Boolean,
    onPurchase: (ProductDetails, String) -> Unit,
    onRestore: () -> Unit
) {
    var selectedProductId by remember { mutableStateOf(SubscriptionManager.PRODUCT_ID_YEARLY) }

    Spacer(modifier = Modifier.height(16.dp))

    // Header
    Icon(
        imageVector = Icons.Outlined.Star,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.subscription_header),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.subscription_subheader),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Benefits
    BenefitRow(Icons.Outlined.Block, stringResource(R.string.benefit_no_ads))
    BenefitRow(Icons.Outlined.Speed, stringResource(R.string.benefit_faster_experience))
    BenefitRow(Icons.Outlined.Star, stringResource(R.string.benefit_support_development))

    Spacer(modifier = Modifier.height(24.dp))

    if (!isConnected || products.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        Text(
            text = stringResource(R.string.subscription_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        // Plan cards
        val monthlyProduct = products.find { it.productId == SubscriptionManager.PRODUCT_ID_MONTHLY }
        val yearlyProduct = products.find { it.productId == SubscriptionManager.PRODUCT_ID_YEARLY }

        yearlyProduct?.let { product ->
            PlanCard(
                product = product,
                isSelected = selectedProductId == SubscriptionManager.PRODUCT_ID_YEARLY,
                isBestValue = true,
                onClick = { selectedProductId = SubscriptionManager.PRODUCT_ID_YEARLY }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        monthlyProduct?.let { product ->
            PlanCard(
                product = product,
                isSelected = selectedProductId == SubscriptionManager.PRODUCT_ID_MONTHLY,
                isBestValue = false,
                onClick = { selectedProductId = SubscriptionManager.PRODUCT_ID_MONTHLY }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subscribe button
        val selectedProduct = products.find { it.productId == selectedProductId }
        selectedProduct?.let { product ->
            val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                Button(
                    onClick = { onPurchase(product, offerToken) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.subscription_subscribe_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.subscription_restore))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.subscription_terms),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PlanCard(
    product: ProductDetails,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val offerDetails = product.subscriptionOfferDetails?.firstOrNull()
    val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val price = pricingPhase?.formattedPrice ?: ""
    val billingPeriod = pricingPhase?.billingPeriod ?: ""

    val periodLabel = when {
        billingPeriod.contains("Y") -> stringResource(R.string.plan_yearly)
        billingPeriod.contains("M") -> stringResource(R.string.plan_monthly)
        else -> ""
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = stringResource(R.string.best_value),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/$periodLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
