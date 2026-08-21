package fyi.appy.steadygridgallery.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_FILE = "steady_gallery_purchase_entitlement"
private const val KEY_PRODUCT_ID = "productId"
private const val KEY_IS_PURCHASED = "isPurchased"
private const val KEY_TOKEN_HASH = "purchaseTokenHash"
private const val KEY_UPDATED_AT = "updatedAt"

const val PRO_UNLOCK_PRODUCT_ID = "steady_gallery_pro_unlock"

data class PurchaseEntitlement(
    val productId: String,
    val isPurchased: Boolean,
    val purchaseTokenHash: String,
    val updatedAtMillis: Long,
)

class PurchaseEntitlementStore(context: Context) {
    private val prefs = SecureStorage.open(context, PREFS_FILE)

    private val _isPurchased = MutableStateFlow(prefs.getBoolean(KEY_IS_PURCHASED, false))
    val isPurchased: StateFlow<Boolean> = _isPurchased

    fun getEntitlement(): PurchaseEntitlement = PurchaseEntitlement(
        productId = prefs.getString(KEY_PRODUCT_ID, PRO_UNLOCK_PRODUCT_ID) ?: PRO_UNLOCK_PRODUCT_ID,
        isPurchased = prefs.getBoolean(KEY_IS_PURCHASED, false),
        purchaseTokenHash = prefs.getString(KEY_TOKEN_HASH, "") ?: "",
        updatedAtMillis = prefs.getLong(KEY_UPDATED_AT, 0L),
    )

    fun setEntitlement(productId: String, isPurchased: Boolean, purchaseTokenHash: String) {
        prefs.edit()
            .putString(KEY_PRODUCT_ID, productId)
            .putBoolean(KEY_IS_PURCHASED, isPurchased)
            .putString(KEY_TOKEN_HASH, purchaseTokenHash)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        _isPurchased.value = isPurchased
    }
}
