package com.appyfyi.steadygridgallery.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.appyfyi.steadygridgallery.BuildConfig
import com.appyfyi.steadygridgallery.R
import com.appyfyi.steadygridgallery.data.prefs.PRO_UNLOCK_PRODUCT_ID
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

sealed interface BillingUiState {
    data object LoadingProducts : BillingUiState
    data class NotPurchased(val formattedPrice: String) : BillingUiState
    data object Purchasing : BillingUiState
    data object Purchased : BillingUiState
    data object BillingUnavailable : BillingUiState
    data class Error(val message: String) : BillingUiState
}

/**
 * v1 is a one-time in-app purchase only: no subscriptions, accounts, credits, or server-side
 * receipt validation. Entitlement is cached locally after Play Billing acknowledges the purchase.
 */
class BillingRepository(
    private val context: Context,
    private val entitlementStore: PurchaseEntitlementStore,
) {
    private var productDetails: ProductDetails? = null

    private val _uiState = MutableStateFlow<BillingUiState>(BillingUiState.LoadingProducts)
    val uiState: StateFlow<BillingUiState> = _uiState

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _uiState.value = BillingUiState.NotPurchased(currentFormattedPrice())
            else -> _uiState.value = BillingUiState.Error(result.debugMessage)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnectionAndLoad() {
        _uiState.value = BillingUiState.LoadingProducts
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    refreshPurchases()
                } else {
                    _uiState.value = BillingUiState.BillingUnavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.value = BillingUiState.BillingUnavailable
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_UNLOCK_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull()
                productDetails = details
                if (entitlementStore.isPurchased.value) {
                    _uiState.value = BillingUiState.Purchased
                } else {
                    _uiState.value = BillingUiState.NotPurchased(currentFormattedPrice())
                }
            } else {
                _uiState.value = BillingUiState.Error(result.debugMessage)
            }
        }
    }

    private fun currentFormattedPrice(): String {
        val offer = productDetails?.oneTimePurchaseOfferDetails
        return offer?.formattedPrice ?: context.getString(R.string.purchase_default_price)
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails ?: run {
            _uiState.value = BillingUiState.Error(context.getString(R.string.purchase_product_not_loaded))
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        _uiState.value = BillingUiState.Purchasing
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchased = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (purchased) {
                    purchases.forEach { handlePurchase(it) }
                } else if (_uiState.value !is BillingUiState.LoadingProducts) {
                    _uiState.value = BillingUiState.NotPurchased(currentFormattedPrice())
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        fun persistEntitlement() {
            val tokenHash = sha256(purchase.purchaseToken)
            entitlementStore.setEntitlement(PRO_UNLOCK_PRODUCT_ID, isPurchased = true, purchaseTokenHash = tokenHash)
            _uiState.value = BillingUiState.Purchased
        }

        if (purchase.isAcknowledged) {
            persistEntitlement()
        } else {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    persistEntitlement()
                } else {
                    _uiState.value = BillingUiState.Error(result.debugMessage)
                }
            }
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    /**
     * Local-only stand-in for a real Play purchase, for testing Editor/Hidden Folders before the
     * app is registered in Play Console. Guarded by BuildConfig.DEBUG so it's physically absent
     * from release builds, not just hidden in the UI.
     */
    fun debugGrantEntitlement() {
        if (!BuildConfig.DEBUG) return
        entitlementStore.setEntitlement(PRO_UNLOCK_PRODUCT_ID, isPurchased = true, purchaseTokenHash = "debug-bypass")
        _uiState.value = BillingUiState.Purchased
    }

    fun debugRevokeEntitlement() {
        if (!BuildConfig.DEBUG) return
        entitlementStore.setEntitlement(PRO_UNLOCK_PRODUCT_ID, isPurchased = false, purchaseTokenHash = "")
        _uiState.value = BillingUiState.NotPurchased(currentFormattedPrice())
    }
}
