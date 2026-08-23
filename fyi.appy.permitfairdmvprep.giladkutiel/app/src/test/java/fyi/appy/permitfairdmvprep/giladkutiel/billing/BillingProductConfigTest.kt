package fyi.appy.permitfairdmvprep.giladkutiel.billing

import com.android.billingclient.api.BillingClient
import fyi.appy.permitfairdmvprep.giladkutiel.data.ProductIds
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingProductConfigTest {
    @Test
    fun `the only configured product is lifetime_unlock as an INAPP product`() {
        assertEquals(1, CONFIGURED_PRODUCTS.size)

        val product = CONFIGURED_PRODUCTS.single()
        assertEquals(ProductIds.LIFETIME_UNLOCK, product.productId)
        assertEquals(BillingClient.ProductType.INAPP, product.productType)

        assertEquals(0, CONFIGURED_PRODUCTS.count { it.productType == BillingClient.ProductType.SUBS })
    }
}
