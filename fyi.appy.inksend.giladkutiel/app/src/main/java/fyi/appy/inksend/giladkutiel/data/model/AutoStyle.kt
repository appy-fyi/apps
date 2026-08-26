package fyi.appy.inksend.giladkutiel.data.model

import java.text.Normalizer
import kotlin.random.Random

/** Unicode NFC so a keyword and the typed text compare equal regardless of how each was composed. */
private fun String.nfc(): String = Normalizer.normalize(this, Normalizer.Form.NFC)

/**
 * Content-driven styling: instead of the user maintaining a list of styles, the look of the
 * rendered image is chosen automatically from what was typed.
 *
 * How it works:
 *  1. [Intent] is a fixed set of moods, each carrying 3–5 hand-tuned [StyleConfig] looks.
 *  2. [WORD_KEYWORDS] / [PHRASE_KEYWORDS] / [EMOJI_KEYWORDS] map trigger tokens to an intent.
 *  3. [detectIntent] scores every intent against the text's words, phrases, and emoji and
 *     returns the highest-scoring one, or [Intent.NEUTRAL] when nothing matches.
 *  4. [styleFor] resolves the intent and then picks one of its looks at random, so repeated
 *     taps on the same text still produce some visual variety.
 *
 * Note on languages: the keyword tables here are **English only**. Non-English text is
 * translated to English on-device (Google ML Kit) by
 * [fyi.appy.inksend.giladkutiel.service.TextTranslator] before it reaches [detectIntent], so
 * one dictionary covers every language instead of one table per language. This class stays
 * pure and free of Android/ML Kit dependencies so it remains plain-JVM unit-testable; the
 * caller owns the (async) translation step. Emoji are language-independent and are matched in
 * the raw text, so they still work even when translation is unavailable (model not yet
 * downloaded, device offline) — in which case the untranslated text is matched as-is and
 * falls back to [Intent.NEUTRAL] when no English keyword or emoji hits.
 */
enum class Intent(val displayEmoji: String, val styles: List<StyleConfig>) {
    FUNNY(
        "😂",
        listOf(
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#1E1E2E", backgroundColorHex = "#F2C94C", isGradientEnabled = true, gradientEndColorHex = "#F7B267", emoji = "😂"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#E85D9E", isGradientEnabled = true, gradientEndColorHex = "#F7B267", emoji = "🤣"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F7B267", isGradientEnabled = true, gradientEndColorHex = "#F2C94C", emoji = "😜"),
        ),
    ),
    SAD(
        "😢",
        listOf(
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#C9B8FF", backgroundColorHex = "#1B2A4A", isGradientEnabled = true, gradientEndColorHex = "#1E1E2E", emoji = "😢"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#9AA0A6", isGradientEnabled = true, gradientEndColorHex = "#1B2A4A", emoji = "💧"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#89B4FA", backgroundColorHex = "#1E1E2E", isGradientEnabled = true, gradientEndColorHex = "#1B2A4A", emoji = "🥀"),
        ),
    ),
    ROMANTIC(
        "❤️",
        listOf(
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#FFFFFF", backgroundColorHex = "#E85D9E", isGradientEnabled = true, gradientEndColorHex = "#9B59D0", emoji = "❤️"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#D64545", backgroundColorHex = "#F5E9DA", isGradientEnabled = true, gradientEndColorHex = "#EAD3C0", emoji = "🌹"),
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#1E1E2E", backgroundColorHex = "#C9B8FF", isGradientEnabled = true, gradientEndColorHex = "#E85D9E", emoji = "💕"),
        ),
    ),
    ANGRY(
        "😠",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#D64545", isGradientEnabled = true, gradientEndColorHex = "#1E1E2E", emoji = "😤"),
            StyleConfig(font = FontChoice.MONOSPACE, textColorHex = "#D64545", backgroundColorHex = "#000000", isGradientEnabled = true, gradientEndColorHex = "#2A0D0D", emoji = "🔥"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#F2C94C", backgroundColorHex = "#1B2A4A", isGradientEnabled = true, gradientEndColorHex = "#D64545", emoji = "⚡"),
        ),
    ),
    INFORMATIVE(
        "📌",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#FFFFFF", isGradientEnabled = true, gradientEndColorHex = "#E6ECF3", emoji = "📌"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#1B2A4A", backgroundColorHex = "#F5E9DA", isGradientEnabled = true, gradientEndColorHex = "#E6EAF0", emoji = "ℹ️"),
            StyleConfig(font = FontChoice.MONOSPACE, textColorHex = "#FFFFFF", backgroundColorHex = "#1B2A4A", isGradientEnabled = true, gradientEndColorHex = "#89B4FA", emoji = "📊"),
        ),
    ),
    EXCITED(
        "🤩",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#5B47E0", isGradientEnabled = true, gradientEndColorHex = "#C9B8FF", emoji = "🤩"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F2C94C", isGradientEnabled = true, gradientEndColorHex = "#E85D9E", emoji = "🚀"),
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#FFFFFF", backgroundColorHex = "#E85D9E", isGradientEnabled = true, gradientEndColorHex = "#5B47E0", emoji = "✨"),
        ),
    ),
    CELEBRATORY(
        "🎉",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F2C94C", isGradientEnabled = true, gradientEndColorHex = "#F7B267", emoji = "🎉"),
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#FFFFFF", backgroundColorHex = "#9B59D0", isGradientEnabled = true, gradientEndColorHex = "#89B4FA", emoji = "🥳"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#5B47E0", backgroundColorHex = "#F5E9DA", isGradientEnabled = true, gradientEndColorHex = "#E7DAF5", emoji = "🎊"),
        ),
    ),
    CALM(
        "🌿",
        listOf(
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#2DB6A3", isGradientEnabled = true, gradientEndColorHex = "#4CAF7D", emoji = "🌿"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#C9B8FF", isGradientEnabled = true, gradientEndColorHex = "#A9E0D5", emoji = "🧘"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#1B2A4A", backgroundColorHex = "#89B4FA", isGradientEnabled = true, gradientEndColorHex = "#F5E9DA", emoji = "☁️"),
        ),
    ),
    MOTIVATIONAL(
        "💪",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#5B47E0", isGradientEnabled = true, gradientEndColorHex = "#1E1E2E", emoji = "💪"),
            StyleConfig(font = FontChoice.MONOSPACE, textColorHex = "#F2C94C", backgroundColorHex = "#000000", isGradientEnabled = true, gradientEndColorHex = "#1E1E2E", emoji = "🏆"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F7B267", isGradientEnabled = true, gradientEndColorHex = "#F2C94C", emoji = "🔥"),
        ),
    ),
    GRATEFUL(
        "🙏",
        listOf(
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F5E9DA", isGradientEnabled = true, gradientEndColorHex = "#F7B267", emoji = "🙏"),
            StyleConfig(font = FontChoice.CURSIVE, textColorHex = "#1B2A4A", backgroundColorHex = "#F2C94C", isGradientEnabled = true, gradientEndColorHex = "#F7B267", emoji = "💛"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#F7B267", isGradientEnabled = true, gradientEndColorHex = "#E85D9E", emoji = "🌻"),
        ),
    ),

    /** Fallback look for text that matches no intent — the app's original neutral defaults. */
    NEUTRAL(
        "✨",
        listOf(
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#1E1E2E", isGradientEnabled = true, gradientEndColorHex = "#89B4FA", emoji = "✨"),
            StyleConfig(font = FontChoice.SERIF, textColorHex = "#1E1E2E", backgroundColorHex = "#F5E9DA", isGradientEnabled = true, gradientEndColorHex = "#E9DECB", emoji = "🎨"),
            StyleConfig(font = FontChoice.SANS_SERIF, textColorHex = "#FFFFFF", backgroundColorHex = "#5B47E0", isGradientEnabled = true, gradientEndColorHex = "#C9B8FF", emoji = "⭐"),
        ),
    ),
}

/**
 * Single-word triggers, keyed by the lowercased word. A word is matched only when it appears
 * as a whole token in the text, so "informative" here never fires on "misinformative".
 * English only. Non-English text is translated to English before it reaches [detectIntent]
 * (see the class KDoc).
 */
val WORD_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to listOf("funny", "lol", "lmao", "lmfao", "rofl", "haha", "hahaha", "hehe", "joke", "joking", "hilarious", "comedy", "laugh", "laughing", "laughed", "meme", "silly", "ridiculous", "lolz", "giggle"),
    Intent.SAD to listOf("sad", "sadness", "unhappy", "cry", "crying", "cried", "tears", "heartbroken", "depressed", "depressing", "miserable", "lonely", "grief", "grieving", "sorrow", "hurts", "hurting", "devastated", "gutted"),
    Intent.ROMANTIC to listOf("love", "loved", "loving", "romantic", "romance", "darling", "sweetheart", "babe", "honey", "kiss", "kisses", "crush", "adore", "adorable", "forever", "valentine", "cutie", "beloved"),
    Intent.ANGRY to listOf("angry", "mad", "furious", "hate", "rage", "raging", "pissed", "annoyed", "annoying", "unacceptable", "outrageous", "livid", "irritated", "ugh", "fuming"),
    Intent.INFORMATIVE to listOf("fyi", "note", "notice", "update", "reminder", "info", "information", "details", "schedule", "meeting", "agenda", "report", "summary", "deadline", "instructions", "recap", "memo", "briefing", "announcement"),
    Intent.EXCITED to listOf("excited", "exciting", "omg", "yay", "yayy", "thrilled", "pumped", "stoked", "hyped", "hype", "woah", "whoa", "eek", "wow"),
    Intent.CELEBRATORY to listOf("congrats", "congratulations", "anniversary", "celebrate", "celebration", "celebrating", "cheers", "woohoo", "woo", "party", "hooray", "hurrah"),
    Intent.CALM to listOf("calm", "relax", "relaxing", "relaxed", "peace", "peaceful", "breathe", "chill", "chilling", "serene", "serenity", "quiet", "meditate", "meditation", "unwind", "mindful"),
    Intent.MOTIVATIONAL to listOf("believe", "hustle", "grind", "focus", "focused", "discipline", "stronger", "motivation", "motivated", "motivate", "goals", "persevere", "unstoppable", "determined", "perseverance"),
    Intent.GRATEFUL to listOf("thanks", "thankful", "grateful", "gratitude", "appreciate", "appreciated", "blessed", "thx"),
)

/**
 * Multi-word triggers, matched as a lowercased substring of the whole text. Phrases score
 * higher than single words since they carry more signal. English only, as above.
 */
val PHRASE_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to listOf("so funny", "made me laugh", "can't stop laughing", "cracking up"),
    Intent.SAD to listOf("i miss you", "miss you", "feeling down", "broke my heart", "so sad"),
    Intent.ROMANTIC to listOf("i love you", "love you", "my heart", "my love", "be mine"),
    Intent.ANGRY to listOf("fed up", "had enough", "so mad", "this is unacceptable", "sick of"),
    Intent.INFORMATIVE to listOf("please note", "heads up", "for your information", "just so you know", "action items"),
    Intent.EXCITED to listOf("can't wait", "cannot wait", "so ready", "let's go", "lets go", "here we go"),
    Intent.CELEBRATORY to listOf("happy birthday", "we did it", "way to go", "you nailed it", "job well done"),
    Intent.CALM to listOf("take it easy", "no rush", "deep breath", "it's okay", "all good"),
    Intent.MOTIVATIONAL to listOf("you got this", "keep going", "never give up", "push through", "don't quit", "dont quit", "one step at a time"),
    Intent.GRATEFUL to listOf("thank you", "thank you so much", "means a lot", "means the world", "i appreciate"),
)

/** Emoji triggers, matched as a substring so multi-codepoint sequences (❤️, ☁️) still hit. */
val EMOJI_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to listOf("😂", "🤣", "😹", "😆", "😅", "🙃"),
    Intent.SAD to listOf("😢", "😭", "😥", "😔", "💔", "🥺", "😞", "🥀"),
    Intent.ROMANTIC to listOf("❤️", "🥰", "😍", "💕", "💖", "💗", "😘", "🌹"),
    Intent.ANGRY to listOf("😠", "😡", "🤬", "👿", "💢"),
    Intent.INFORMATIVE to listOf("📌", "📊", "ℹ️", "📝", "🗓️"),
    Intent.EXCITED to listOf("🤩", "🚀", "🙌"),
    Intent.CELEBRATORY to listOf("🎉", "🎊", "🥳", "🍾", "🎈"),
    Intent.CALM to listOf("🌿", "🧘", "☁️", "🕊️", "🍃"),
    Intent.MOTIVATIONAL to listOf("💪", "🏆", "🔥", "⚡"),
    Intent.GRATEFUL to listOf("🙏", "💛", "🌻"),
)

/**
 * A stable colour + glyph for the floating overlay button to show while the user types, so
 * the button previews the look a tap will produce. Carries hex straight from the chosen
 * [StyleConfig]; the caller parses it.
 */
data class ButtonHint(val emoji: String, val backgroundColorHex: String)

private fun buildKeywordMap(vararg entries: Pair<Intent, List<String>>): Map<String, Intent> {
    val map = LinkedHashMap<String, Intent>()
    for ((intent, keywords) in entries) {
        for (keyword in keywords) map.putIfAbsent(keyword.lowercase().nfc(), intent)
    }
    return map
}

object AutoStyle {

    private const val PHRASE_WEIGHT = 3
    private const val WORD_WEIGHT = 1
    private const val EMOJI_WEIGHT = 2

    // Includes \p{M} (combining marks) so Indic tokens like "मज़ेदार" — whose nukta and vowel
    // signs are marks, not letters — are not split apart mid-word.
    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{M}\\p{N}]+")

    /**
     * Scores every non-[Intent.NEUTRAL] intent against [text] and returns the strongest match,
     * or [Intent.NEUTRAL] when nothing scores. Ties break toward the earlier [Intent] entry,
     * so the result is stable for a given input.
     */
    fun detectIntent(text: String): Intent {
        if (text.isBlank()) return Intent.NEUTRAL
        val lower = text.lowercase().nfc()
        val tokens = lower.split(TOKEN_SPLIT).filterTo(HashSet()) { it.isNotEmpty() }

        val scores = HashMap<Intent, Int>()
        fun add(intent: Intent, weight: Int) { scores[intent] = (scores[intent] ?: 0) + weight }

        for ((phrase, intent) in PHRASE_KEYWORDS) {
            if (lower.contains(phrase)) add(intent, PHRASE_WEIGHT)
        }
        for (token in tokens) {
            WORD_KEYWORDS[token]?.let { add(it, WORD_WEIGHT) }
        }
        for ((emoji, intent) in EMOJI_KEYWORDS) {
            if (text.contains(emoji)) add(intent, EMOJI_WEIGHT)
        }

        val best = Intent.entries
            .filter { it != Intent.NEUTRAL }
            .maxByOrNull { scores[it] ?: 0 }
        return if (best != null && (scores[best] ?: 0) > 0) best else Intent.NEUTRAL
    }

    /** Detects the intent for [text] and returns one of its looks, chosen with [random]. */
    fun styleFor(text: String, random: Random = Random.Default): StyleConfig =
        detectIntent(text).styles.random(random)

    /**
     * A non-random appearance for the overlay button to preview [text]'s detected mood as
     * the user types. Unlike [styleFor] it never picks at random — it pairs the intent's
     * representative emoji with its first (canonical) look's background colour — so the
     * button stays put between keystrokes for the same text. [detectIntent] is only
     * dictionary lookups over the typed string, cheap enough to run on every text change.
     * Returns null when nothing matches, so the caller keeps the button's neutral default.
     */
    fun buttonHintFor(text: String): ButtonHint? {
        val intent = detectIntent(text)
        if (intent == Intent.NEUTRAL) return null
        return ButtonHint(intent.displayEmoji, intent.styles.first().backgroundColorHex)
    }
}
