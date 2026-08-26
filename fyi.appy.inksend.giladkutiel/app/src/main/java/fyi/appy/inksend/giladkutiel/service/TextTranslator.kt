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
 * offline. [warmUp] pre-fetches the models for the languages the app's UI supports (over
 * Wi-Fi only) so the first real translation is instant; [toEnglish] otherwise downloads the
 * model it needs on demand.
 *
 * Every failure path — language undetermined, unsupported language, model missing while
 * offline — falls back to returning the original text unchanged, so mood detection still
 * runs (English keywords and emoji in the raw text keep working).
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

        val languageTag = runCatching { languageIdentifier.identifyLanguage(text).await() }
            .getOrDefault(UNDETERMINED)
        if (languageTag == UNDETERMINED || languageTag == TranslateLanguage.ENGLISH) {
            return text.also { cache[text] = it }
        }
        val source = TranslateLanguage.fromLanguageTag(languageTag)
            ?: return text.also { cache[text] = it }

        return runCatching {
            val translator = translatorFor(source)
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        }.getOrDefault(text).also { cache[text] = it }
    }

    /** Pre-download the models for the app's supported UI languages, Wi-Fi only, best effort. */
    fun warmUp() {
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
        val WARM_UP_LANGUAGES = listOf(
            TranslateLanguage.SPANISH,
            TranslateLanguage.FRENCH,
            TranslateLanguage.GERMAN,
            TranslateLanguage.PORTUGUESE,
            TranslateLanguage.HINDI,
        )
    }
}
