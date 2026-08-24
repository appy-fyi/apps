package fyi.appy.inksend.giladkutiel.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import fyi.appy.inksend.giladkutiel.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A single non-consumable product ("unlock_all_styles", $4.99) — never a
 * subscription — that unlocks custom colors and the handwriting font creator.
 * Purchased state is re-verified against Play Billing on every app start and
 * cached in [PreferencesRepository] so gated screens don't block on a network
 * round trip.
 */
class BillingRepository(
    private val context: Context,
    private val prefs: PreferencesRepository,
) {
    companion object {
        const val PRODUCT_ID = "unlock_all_styles"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { applyPurchases(purchases) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    private var connected = false

    private suspend fun ensureConnected() {
        if (connected) return
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    connected = false
                }
            })
        }
    }

    /** Re-verifies purchase state against Play Billing and re-caches the flag. */
    suspend fun refreshPurchaseState() {
        ensureConnected()
        if (!connected) return
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        applyPurchases(result.purchasesList)
    }

    private suspend fun applyPurchases(purchases: List<com.android.billingclient.api.Purchase>) {
        val owned = purchases.any { purchase ->
            purchase.products.contains(PRODUCT_ID) &&
                purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
        }
        if (owned) prefs.setPurchased(true)
        purchases.filter {
            it.products.contains(PRODUCT_ID) &&
                it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED &&
                !it.isAcknowledged
        }.forEach { purchase ->
            val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) {}
        }
    }

    /** Launches the single one-time-purchase flow for [PRODUCT_ID]; never a subscription flow. */
    suspend fun launchPurchase(activity: Activity) {
        ensureConnected()
        if (!connected) return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result = billingClient.queryProductDetails(params)
        val productDetails = result.productDetailsList?.firstOrNull() ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build(),
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }
}
