package fyi.appy.steadygridgallery.ui.common

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import fyi.appy.steadygridgallery.R

/** Supported UI languages, backed by the Android 13+ per-app language API so the OS persists
 *  the choice and recreates the app with the new resources — no local storage needed here. */
enum class AppLanguage(val tag: String, val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    SPANISH("es", R.string.language_spanish),
    FRENCH("fr", R.string.language_french),
}

object AppLocale {
    fun current(context: Context): AppLanguage {
        val tag = context.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)?.toLanguageTag()
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    fun set(context: Context, language: AppLanguage) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales = if (language == AppLanguage.SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(language.tag)
        }
    }
}
