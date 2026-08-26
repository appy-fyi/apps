package fyi.appy.inksend.giladkutiel.service

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Translates typed text to English on-device (Google ML Kit) so that
 * [fyi.appy.inksend.giladkutiel.data.model.AutoStyle] needs only a single English mood
 * dictionary rather than one keyword table per language.
 *
 * ML Kit runs entirely on the device: the text is never sent to a server. The only network
 * use is a one-time ~30 MB model download per source language, after which translation works
 * offline. [warmUp] pre-fetches the Hebrew model on any network (it is the one non-English
 * language the app commits to) plus the UI-chrome languages over Wi-Fi only; [toEnglish]
 * otherwise downloads the model it needs on demand.
 *
 * Every failure path — language undetermined, unsupported language, model missing while
 * offline — falls back to returning the original text unchanged, so mood detection still
 * runs (English keywords and emoji in the raw text keep working). Crucially, a failed
 * translation is **not** cached: only a real translation (or a confirmed English/no-op) is
 * stored, so a tap that happens before the model finished downloading is retried next time
 * rather than being pinned to the untranslated text.
 */
class TextTranslator {

    private val languageIdentifier = LanguageIdentification.getClient()
    private val translators = ConcurrentHashMap<String, Translator>()

    /** Small LRU of source-text -> English so repeated previews and the follow-up tap are cheap. */
    private val cache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > CACHE_SIZE
        },
    )

    suspend fun toEnglish(text: String): String {
        if (text.isBlank()) return text
        cache[text]?.let { return it }

        val rawTag = runCatching { languageIdentifier.identifyLanguage(text).await() }
            .getOrDefault(UNDETERMINED)
        // Language undetermined (often just too short) — don't cache, a later call may do better.
        if (rawTag == UNDETERMINED) return text
        // ML Kit's language-id still emits the pre-1989 ISO codes for a few languages, while
        // TranslateLanguage uses the current ones; map them across before the lookup.
        val languageTag = LEGACY_LANGUAGE_TAGS[rawTag] ?: rawTag
        if (languageTag == TranslateLanguage.ENGLISH) return text.also { cache[text] = it }

        val source = TranslateLanguage.fromLanguageTag(languageTag) ?: return text // unsupported; don't cache

        return runCatching {
            val translator = translatorFor(source)
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        }.fold(
            onSuccess = { translated -> translated.also { cache[text] = it } },
            onFailure = { text }, // model still downloading / offline — return raw but DON'T cache it
        )
    }

    /**
     * Pre-download translation models, best effort. Hebrew — the one non-English language the
     * app maintains fonts and keyword fallbacks for — is fetched on any network so a first
     * Hebrew message isn't stuck untranslated on cellular; the UI-chrome languages stay
     * Wi-Fi-only.
     */
    fun warmUp() {
        runCatching { translatorFor(TranslateLanguage.HEBREW).downloadModelIfNeeded() }
        val wifiOnly = DownloadConditions.Builder().requireWifi().build()
        WARM_UP_LANGUAGES.forEach { language ->
            runCatching { translatorFor(language).downloadModelIfNeeded(wifiOnly) }
        }
    }

    fun close() {
        runCatching { languageIdentifier.close() }
        translators.values.forEach { runCatching { it.close() } }
        translators.clear()
        cache.clear()
    }

    private fun translatorFor(sourceLanguage: String): Translator =
        translators.getOrPut(sourceLanguage) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build(),
            )
        }

    private companion object {
        const val UNDETERMINED = "und"
        const val CACHE_SIZE = 32

        /** ML Kit language-id -> TranslateLanguage tag for the codes ISO renamed in 1989. */
        val LEGACY_LANGUAGE_TAGS = mapOf("iw" to "he", "in" to "id", "ji" to "yi")

        /** Predownloaded over Wi-Fi only; Hebrew is handled separately on any network. */
        val WARM_UP_LANGUAGES = listOf(
            TranslateLanguage.SPANISH,
            TranslateLanguage.FRENCH,
            TranslateLanguage.GERMAN,
            TranslateLanguage.PORTUGUESE,
            TranslateLanguage.HINDI,
        )
    }
}
