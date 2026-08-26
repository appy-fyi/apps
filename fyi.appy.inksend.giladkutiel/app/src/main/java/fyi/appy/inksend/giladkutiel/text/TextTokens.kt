package fyi.appy.inksend.giladkutiel.text

import java.text.Normalizer

/** Unicode NFC so a keyword and the typed text compare equal regardless of composition. */
internal fun String.nfc(): String = Normalizer.normalize(this, Normalizer.Form.NFC)

/**
 * A short English stop-word list. These carry no mood signal, so they are dropped before
 * stemming — both to keep [TextTokens.stemList] focused on the words that matter and so the
 * emoji picker in [fyi.appy.inksend.giladkutiel.data.model.EmojiLexicon] never wastes one of
 * its three slots on "the" or "and".
 *
 * Note: sentiment analysis does **not** use this list — negations ("not", "no", "never") are
 * exactly the tokens it must keep, so
 * [fyi.appy.inksend.giladkutiel.text.SentimentAnalyzer] tokenizes the raw text itself.
 */
object StopWords {
    val SET: Set<String> = hashSetOf(
        "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "while", "of",
        "at", "by", "for", "with", "about", "against", "between", "into", "through", "during",
        "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again",
        "further", "once", "here", "there", "all", "any", "both", "each", "few", "more",
        "most", "other", "some", "such", "only", "own", "same", "than", "too", "very", "can",
        "will", "just", "should", "now", "is", "am", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "having", "do", "does", "did", "doing", "would",
        "could", "ought", "i", "me", "my", "we", "our", "you", "your", "he", "him", "his",
        "she", "her", "it", "its", "they", "them", "their", "what", "which", "who", "whom",
        "this", "that", "these", "those", "as", "until", "because", "so", "that's", "im",
        "ive", "youre", "were", "theyre", "dont", "doesnt", "didnt", "wont", "cant", "isnt",
        "arent", "wasnt", "werent", "hasnt", "havent", "hadnt", "get", "got", "getting",
        "like", "also", "still", "yet", "even", "much", "many", "lot", "really", "actually",
        "quite", "rather", "way", "one", "two", "us", "let", "lets", "gonna", "wanna",
    )
}

/**
 * Turns free text into an ordered, de-duplicated list of Porter stems: lower-case + NFC,
 * split on any run of non-(letter | mark | number), drop tokens shorter than two chars and
 * anything in [StopWords], then [PorterStemmer.stem] what's left. Order is first-seen, which
 * is what lets the emoji picker prefer emojis for the words the user wrote first.
 */
object TextTokens {

    // Includes \p{M} (combining marks) so scripts whose vowel signs / nuktas are marks, not
    // letters (Devanagari, Arabic, Hebrew niqqud …), are not split apart mid-word.
    private val SPLIT = Regex("[^\\p{L}\\p{M}\\p{N}]+")

    private val stemmer = ThreadLocal.withInitial { PorterStemmer() }

    fun stemList(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val ps = stemmer.get()!!
        val out = LinkedHashSet<String>()
        for (token in text.lowercase().nfc().split(SPLIT)) {
            if (token.length < 2 || token in StopWords.SET) continue
            val stem = ps.stem(token)
            if (stem.isNotEmpty()) out.add(stem)
        }
        return out.toList()
    }

    fun stemSet(text: String): Set<String> = HashSet(stemList(text))
}

/** Cheap script probes used to pick a font list and lay text out in the right direction. */
object Scripts {
    /** True when [s] contains any Hebrew-block or Hebrew-presentation-form codepoint. */
    fun hasHebrew(s: String): Boolean =
        s.any { it.code in 0x0590..0x05FF || it.code in 0xFB1D..0xFB4F }
}
