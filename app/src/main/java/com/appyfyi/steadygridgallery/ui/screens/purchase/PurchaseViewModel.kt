package com.appyfyi.steadygridgallery.ui.screens.purchase

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.data.billing.BillingRepository
import com.appyfyi.steadygridgallery.data.billing.BillingUiState
import com.appyfyi.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.StateFlow

class PurchaseViewModel(private val billingRepository: BillingRepository) : ViewModel() {
    val uiState: StateFlow<BillingUiState> = billingRepository.uiState

    /** The connection itself is started once at app launch (see MainActivity); this only retries it. */
    fun retryConnection() = billingRepository.startConnectionAndLoad()

    fun buy(activity: Activity) = billingRepository.launchPurchaseFlow(activity)

    fun restore() = billingRepository.refreshPurchases()

    fun debugGrantEntitlement() = billingRepository.debugGrantEntitlement()

    fun debugRevokeEntitlement() = billingRepository.debugRevokeEntitlement()

    companion object {
        val Factory = viewModelFactory {
            initializer { PurchaseViewModel(appContainer().billingRepository) }
        }
    }
}
