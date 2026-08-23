package fyi.appy.steadygridgallery.data

import android.content.Context
import fyi.appy.steadygridgallery.data.billing.BillingRepository
import fyi.appy.steadygridgallery.data.db.AppDatabase
import fyi.appy.steadygridgallery.data.hidden.HiddenMediaRepository
import fyi.appy.steadygridgallery.data.media.MediaStoreRepository
import fyi.appy.steadygridgallery.data.prefs.HiddenUnlockSession
import fyi.appy.steadygridgallery.data.prefs.LockCredentialStore
import fyi.appy.steadygridgallery.data.prefs.PurchaseEntitlementStore
import fyi.appy.steadygridgallery.data.prefs.SettingsRepository
import fyi.appy.steadygridgallery.data.recycle.RecycleRepository
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
        hiddenMediaDao = database.hiddenMediaDao(),
    )

    val recycleRepository = RecycleRepository(
        context = context,
        recycleItemDao = database.recycleItemDao(),
    )

    val hiddenMediaRepository = HiddenMediaRepository(
        context = context,
        hiddenMediaDao = database.hiddenMediaDao(),
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
