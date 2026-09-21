package com.triviamap.data.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.triviamap.domain.monetization.SupportEvent
import com.triviamap.domain.monetization.SupportOffer
import com.triviamap.domain.monetization.SupportRepository
import com.triviamap.domain.monetization.SupportTier
import com.triviamap.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tips through Google Play Billing. Purchases are consumed right away (so they can be repeated) and only
 * flip a local "supporter" flag: no server-side verification, acceptable because nothing valuable is unlocked.
 */
@Singleton
class BillingSupportRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: UserPreferencesRepository
) : SupportRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val details = mutableMapOf<SupportTier, ProductDetails>()

    private val _offers = MutableStateFlow<List<SupportOffer>>(emptyList())
    override val offers: StateFlow<List<SupportOffer>> = _offers

    private val _events = MutableSharedFlow<SupportEvent>(extraBufferCapacity = 4)
    override val events: SharedFlow<SupportEvent> = _events

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadOffers()
                        consumeInterruptedPurchases()
                    }
                }
            }

            override fun onBillingServiceDisconnected() = Unit // auto-reconnect is enabled
        })
    }

    override fun purchase(activity: Activity, tier: SupportTier) {
        val product = details[tier]
        if (product == null || !client.isReady) {
            _events.tryEmit(SupportEvent.Failed)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) _events.tryEmit(SupportEvent.Failed)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.orEmpty().forEach { handle(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.tryEmit(SupportEvent.Failed)
        }
    }

    private suspend fun loadOffers() {
        val products = SupportTier.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build())
        details.clear()
        result.productDetailsList.orEmpty().forEach { d ->
            SupportTier.fromProductId(d.productId)?.let { details[it] = d }
        }
        _offers.value = SupportTier.entries.mapNotNull { tier ->
            details[tier]?.oneTimePurchaseOfferDetails?.formattedPrice?.let { SupportOffer(tier, it) }
        }
    }

    private suspend fun consumeInterruptedPurchases() {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        result.purchasesList.forEach { handle(it, announce = false) }
    }

    private suspend fun handle(purchase: Purchase, announce: Boolean = true) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return // pending: handled when it completes
        val consumed = client.consumePurchase(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
        if (consumed.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        prefs.setSupporter(true)
        if (announce) _events.tryEmit(SupportEvent.Thanks(purchase.products.firstNotNullOfOrNull { SupportTier.fromProductId(it) }))
    }
}
