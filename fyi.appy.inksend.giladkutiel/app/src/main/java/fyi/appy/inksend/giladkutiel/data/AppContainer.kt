package fyi.appy.inksend.giladkutiel.data

import android.content.Context
import fyi.appy.inksend.giladkutiel.billing.BillingRepository
import fyi.appy.inksend.giladkutiel.data.db.AppDatabase

/**
 * Manual dependency container (no DI framework in the spec's tech stack).
 * One instance lives on [fyi.appy.inksend.giladkutiel.InkSendApp] and is reused
 * by both the main Activity and the keyboard IME service, since both run in
 * the same process and need the same Room database.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)

    val preferencesRepository = PreferencesRepository(appContext)

    val styleRepository = StyleRepository(
        styleDao = database.stylePresetDao(),
        handwritingFontDao = database.handwritingFontDao(),
        prefs = preferencesRepository,
    )

    val billingRepository = BillingRepository(appContext, preferencesRepository)
}
