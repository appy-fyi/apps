package fyi.appy.permitfairdmvprep.giladkutiel.billing

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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.data.EntitlementCacheEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.ProductIds
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface BillingUiState {
    data object Loading : BillingUiState
    data class Ready(val formattedPrice: String) : BillingUiState
    data object Unavailable : BillingUiState
    data object ProductNotFound : BillingUiState
}

data class ConfiguredProduct(val productId: String, val productType: String)

/**
 * The billing product request builder's source of truth: exactly one INAPP product, never a
 * SUBS product — appy build-spec test scenario "Price unaffordable for the actual audience".
 */
val CONFIGURED_PRODUCTS: List<ConfiguredProduct> = listOf(
    ConfiguredProduct(ProductIds.LIFETIME_UNLOCK, BillingClient.ProductType.INAPP),
)

sealed interface PurchaseEvent {
    data object Completed : PurchaseEvent
    data object Pending : PurchaseEvent
    data object Canceled : PurchaseEvent
    data object AlreadyOwned : PurchaseEvent
    data class Error(val message: String) : PurchaseEvent
}

/**
 * Implements the appy build-spec feature "Transparent one-time lifetime unlock": exactly one
 * INAPP product (lifetime_unlock), no subscription product, no trial.
 */
class BillingRepository(
    context: Context,
    private val db: AppDatabase,
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow<BillingUiState>(BillingUiState.Loading)
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>()
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            scope.launch {
                queryProductDetailsInternal()
                queryExistingPurchases()
            }
            return
        }
        _uiState.value = BillingUiState.Loading
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetailsInternal()
                        queryExistingPurchases()
                    }
                } else {
                    _uiState.value = BillingUiState.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.value = BillingUiState.Unavailable
            }
        })
    }

    private suspend fun queryProductDetailsInternal() {
        val products = CONFIGURED_PRODUCTS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(it.productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = BillingUiState.Unavailable
            return
        }
        val details = result.productDetailsList?.firstOrNull()
        if (details == null) {
            _uiState.value = BillingUiState.ProductNotFound
            return
        }
        productDetails = details
        _uiState.value = BillingUiState.Ready(details.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty())
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails ?: return
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> scope.launch { _purchaseEvents.emit(PurchaseEvent.Canceled) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                queryExistingPurchases()
                _purchaseEvents.emit(PurchaseEvent.AlreadyOwned)
            }
            else -> scope.launch { _purchaseEvents.emit(PurchaseEvent.Error(billingResult.debugMessage)) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        scope.launch {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                        )
                    }
                    persistEntitlement(purchase)
                    _purchaseEvents.emit(PurchaseEvent.Completed)
                }
                Purchase.PurchaseState.PENDING -> _purchaseEvents.emit(PurchaseEvent.Pending)
                else -> Unit
            }
        }
    }

    suspend fun restorePurchases() {
        queryExistingPurchases()
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val result = billingClient.queryPurchasesAsync(params)
        val ownedPurchase = result.purchasesList.firstOrNull {
            it.products.contains(ProductIds.LIFETIME_UNLOCK) && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (ownedPurchase != null) {
            if (!ownedPurchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(ownedPurchase.purchaseToken).build(),
                )
            }
            persistEntitlement(ownedPurchase)
        }
    }

    private suspend fun persistEntitlement(purchase: Purchase) {
        db.entitlementCacheDao().upsert(
            EntitlementCacheEntity(
                productId = ProductIds.LIFETIME_UNLOCK,
                isOwned = true,
                purchaseTokenHash = sha256(purchase.purchaseToken),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun observeIsOwned(): Flow<Boolean> = db.entitlementCacheDao().observe(ProductIds.LIFETIME_UNLOCK).map { it?.isOwned == true }
}
