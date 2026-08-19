package com.appyfyi.steadygridgallery.data

import android.content.Context
import com.appyfyi.steadygridgallery.data.billing.BillingRepository
import com.appyfyi.steadygridgallery.data.db.AppDatabase
import com.appyfyi.steadygridgallery.data.media.MediaStoreRepository
import com.appyfyi.steadygridgallery.data.prefs.HiddenUnlockSession
import com.appyfyi.steadygridgallery.data.prefs.LockCredentialStore
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import com.appyfyi.steadygridgallery.data.prefs.SettingsRepository
import com.appyfyi.steadygridgallery.data.recycle.RecycleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Hand-rolled composition root (no DI framework is in the approved dependency list). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase = AppDatabase.getInstance(context)

    val mediaStoreRepository = MediaStoreRepository(
        context = context,
        folderStateDao = database.folderStateDao(),
        recycleItemDao = database.recycleItemDao(),
    )

    val recycleRepository = RecycleRepository(
        context = context,
        recycleItemDao = database.recycleItemDao(),
    )

    val settingsRepository = SettingsRepository(
        dao = database.appPreferenceDao(),
        externalScope = applicationScope,
    )

    // Lazy: these touch AndroidKeyStore on first access. Building them eagerly in Application.onCreate
    // would do keystore work before it's needed and makes the Application impossible to stand up
    // under Robolectric (no AndroidKeyStore provider in the JVM test environment).
    val lockCredentialStore by lazy { LockCredentialStore(context) }

    val hiddenUnlockSession = HiddenUnlockSession()

    val purchaseEntitlementStore by lazy { PurchaseEntitlementStore(context) }

    val billingRepository by lazy {
        BillingRepository(
            context = context,
            entitlementStore = purchaseEntitlementStore,
        )
    }
}
