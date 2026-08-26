package fyi.appy.inksend.giladkutiel.data.model

import fyi.appy.inksend.giladkutiel.text.PorterStemmer
import fyi.appy.inksend.giladkutiel.text.Scripts
import fyi.appy.inksend.giladkutiel.text.Sentiment
import fyi.appy.inksend.giladkutiel.text.SentimentAnalyzer
import fyi.appy.inksend.giladkutiel.text.TextTokens
import fyi.appy.inksend.giladkutiel.text.nfc
import kotlin.random.Random

/**
 * A bundled OFL font shipped in `assets/fonts/`. Each carries a display name (for the
 * Settings gallery) and its asset path (for `Typeface.createFromAsset`). Latin faces cover
 * English; the four Hebrew-capable faces (Heebo, Frank Ruhl Libre, Suez One, Secular One,
 * Rubik) additionally have full Hebrew coverage.
 */
enum class BundledFont(val displayName: String, val assetPath: String) {
    PACIFICO("Pacifico", "fonts/Pacifico.ttf"),
    ANTON("Anton", "fonts/Anton.ttf"),
    PLAYFAIR("Playfair Display", "fonts/PlayfairDisplay.ttf"),
    CAVEAT("Caveat", "fonts/Caveat.ttf"),
    SPACE_MONO("Space Mono", "fonts/SpaceMono.ttf"),
    COMFORTAA("Comfortaa", "fonts/Comfortaa.ttf"),
    LOBSTER("Lobster", "fonts/Lobster.ttf"),
    BEBAS_NEUE("Bebas Neue", "fonts/BebasNeue.ttf"),
    HEEBO("Heebo", "fonts/Heebo.ttf"),
    FRANK_RUHL("Frank Ruhl Libre", "fonts/FrankRuhlLibre.ttf"),
    SUEZ_ONE("Suez One", "fonts/SuezOne.ttf"),
    SECULAR_ONE("Secular One", "fonts/SecularOne.ttf"),
    RUBIK("Rubik", "fonts/Rubik.ttf"),
}

/** A two-stop background gradient, both stops as `#RRGGBB`. */
data class Gradient(val startHex: String, val endHex: String)

/**
 * Content-driven styling. The look of the rendered image is derived from what was typed:
 *
 *  1. The text is translated to English on-device, then tokenized + Porter-stemmed
 *     ([TextTokens]) so a single English dictionary covers every language.
 *  2. [detectIntent] scores the ten [Intent]s against the text's keyword **stems**, its
 *     multi-word phrases, and any emoji, and returns the best — or null when nothing hits.
 *  3. On a null, [resolveIntent] falls back to [SentimentAnalyzer]: POSITIVE / NEGATIVE /
 *     NEUTRAL is mapped **loosely** to a set of intents ([SENTIMENT_INTENTS]) and one is
 *     picked at random.
 *  4. [planFor] turns the intent into a [RenderPlan]: a random font from the intent's list
 *     for the text's language (Hebrew vs. English), a random gradient, an auto-contrasting
 *     text colour, and up to three emojis from [EmojiLexicon]. The random picks are
 *     deliberate — the same text is meant to produce a fresh image each tap.
 *
 * This class has no Android dependencies so it stays plain-JVM unit-testable; the caller owns
 * the async translation step.
 */
enum class Intent(
    val previewEmoji: String,
    val englishFonts: List<BundledFont>,
    val hebrewFonts: List<BundledFont>,
    val gradients: List<Gradient>,
) {
    FUNNY(
        "😂",
        listOf(BundledFont.PACIFICO, BundledFont.COMFORTAA, BundledFont.CAVEAT),
        listOf(BundledFont.SECULAR_ONE, BundledFont.RUBIK),
        listOf(
            Gradient("#F2C94C", "#F7B267"), Gradient("#E85D9E", "#F7B267"),
            Gradient("#F7B267", "#F2C94C"), Gradient("#FFD166", "#EF476F"),
            Gradient("#06D6A0", "#F2C94C"),
        ),
    ),
    SAD(
        "😢",
        listOf(BundledFont.PLAYFAIR, BundledFont.CAVEAT),
        listOf(BundledFont.FRANK_RUHL, BundledFont.HEEBO),
        listOf(
            Gradient("#1B2A4A", "#1E1E2E"), Gradient("#9AA0A6", "#1B2A4A"),
            Gradient("#1E1E2E", "#1B2A4A"), Gradient("#2C3E50", "#4CA1AF"),
            Gradient("#3A6073", "#16222A"),
        ),
    ),
    ROMANTIC(
        "❤️",
        listOf(BundledFont.PACIFICO, BundledFont.PLAYFAIR, BundledFont.CAVEAT),
        listOf(BundledFont.FRANK_RUHL, BundledFont.HEEBO),
        listOf(
            Gradient("#E85D9E", "#9B59D0"), Gradient("#F5E9DA", "#EAD3C0"),
            Gradient("#C9B8FF", "#E85D9E"), Gradient("#FF6A88", "#FF99AC"),
            Gradient("#B24592", "#F15F79"),
        ),
    ),
    ANGRY(
        "😠",
        listOf(BundledFont.ANTON, BundledFont.BEBAS_NEUE, BundledFont.SPACE_MONO),
        listOf(BundledFont.SUEZ_ONE, BundledFont.RUBIK),
        listOf(
            Gradient("#D64545", "#1E1E2E"), Gradient("#000000", "#2A0D0D"),
            Gradient("#1B2A4A", "#D64545"), Gradient("#8E0E00", "#1F1C18"),
            Gradient("#CB2D3E", "#EF473A"),
        ),
    ),
    INFORMATIVE(
        "📌",
        listOf(BundledFont.SPACE_MONO, BundledFont.BEBAS_NEUE, BundledFont.PLAYFAIR),
        listOf(BundledFont.HEEBO, BundledFont.FRANK_RUHL),
        listOf(
            Gradient("#5B6B8C", "#2E3A4E"), Gradient("#3B4A63", "#1B2A4A"),
            Gradient("#1B2A4A", "#89B4FA"), Gradient("#42556E", "#5C7A99"),
            Gradient("#4B5D73", "#8296AE"),
        ),
    ),
    EXCITED(
        "🤩",
        listOf(BundledFont.PACIFICO, BundledFont.LOBSTER, BundledFont.COMFORTAA),
        listOf(BundledFont.SECULAR_ONE, BundledFont.SUEZ_ONE),
        listOf(
            Gradient("#5B47E0", "#C9B8FF"), Gradient("#F2C94C", "#E85D9E"),
            Gradient("#E85D9E", "#5B47E0"), Gradient("#FC466B", "#3F5EFB"),
            Gradient("#F09819", "#EDDE5D"),
        ),
    ),
    CELEBRATORY(
        "🎉",
        listOf(BundledFont.LOBSTER, BundledFont.PACIFICO, BundledFont.COMFORTAA),
        listOf(BundledFont.SECULAR_ONE, BundledFont.SUEZ_ONE),
        listOf(
            Gradient("#F2C94C", "#F7B267"), Gradient("#9B59D0", "#89B4FA"),
            Gradient("#F857A6", "#FF5858"), Gradient("#FFAFBD", "#FFC3A0"),
            Gradient("#C6426E", "#642B73"),
        ),
    ),
    CALM(
        "🌿",
        listOf(BundledFont.COMFORTAA, BundledFont.PLAYFAIR, BundledFont.CAVEAT),
        listOf(BundledFont.HEEBO, BundledFont.FRANK_RUHL),
        listOf(
            Gradient("#2DB6A3", "#4CAF7D"), Gradient("#5EAFA0", "#A9E0D5"),
            Gradient("#5E8BB0", "#B9D7EA"), Gradient("#89F7FE", "#66A6FF"),
            Gradient("#3E7C78", "#7AB8A0"),
        ),
    ),
    MOTIVATIONAL(
        "💪",
        listOf(BundledFont.ANTON, BundledFont.BEBAS_NEUE, BundledFont.COMFORTAA),
        listOf(BundledFont.SUEZ_ONE, BundledFont.RUBIK),
        listOf(
            Gradient("#5B47E0", "#1E1E2E"), Gradient("#0F2027", "#2C5364"),
            Gradient("#F7B267", "#EF476F"), Gradient("#C33764", "#1D2671"),
            Gradient("#1F1C2C", "#928DAB"),
        ),
    ),
    GRATEFUL(
        "🙏",
        listOf(BundledFont.CAVEAT, BundledFont.PLAYFAIR, BundledFont.COMFORTAA),
        listOf(BundledFont.FRANK_RUHL, BundledFont.RUBIK),
        listOf(
            Gradient("#F5E9DA", "#F7B267"), Gradient("#F2C94C", "#F7B267"),
            Gradient("#F7B267", "#E85D9E"), Gradient("#FFE0B2", "#FFB74D"),
            Gradient("#E6A57E", "#D98E73"),
        ),
    ),
    ;

    companion object {
        /** The ten intents, exactly (todo.txt) — there is no neutral entry. */
        val ALL: List<Intent> = entries
    }
}

/** Single-token triggers, keyed by [PorterStemmer] output (the same stems queries produce). */
val STEM_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to listOf("funny", "lol", "lmao", "rofl", "haha", "hehe", "joke", "joking", "hilarious", "comedy", "laugh", "laughing", "meme", "silly", "ridiculous", "giggle", "prank", "goofy"),
    Intent.SAD to listOf("sad", "sadness", "unhappy", "cry", "crying", "cried", "tears", "heartbroken", "depressed", "miserable", "lonely", "grief", "grieving", "sorrow", "hurts", "hurting", "devastated", "gutted", "down"),
    Intent.ROMANTIC to listOf("love", "loved", "loving", "romantic", "romance", "darling", "sweetheart", "babe", "honey", "kiss", "kisses", "crush", "adore", "adorable", "valentine", "cutie", "beloved", "smitten"),
    Intent.ANGRY to listOf("angry", "mad", "furious", "hate", "rage", "raging", "pissed", "annoyed", "annoying", "unacceptable", "outrageous", "livid", "irritated", "fuming", "disgusted"),
    Intent.INFORMATIVE to listOf("fyi", "note", "notice", "update", "reminder", "info", "information", "details", "schedule", "meeting", "agenda", "report", "summary", "deadline", "instructions", "recap", "memo", "briefing", "announcement", "heads"),
    Intent.EXCITED to listOf("excited", "exciting", "omg", "yay", "thrilled", "pumped", "stoked", "hyped", "hype", "woah", "whoa", "wow", "eek"),
    Intent.CELEBRATORY to listOf("congrats", "congratulations", "anniversary", "celebrate", "celebration", "celebrating", "cheers", "woohoo", "party", "hooray", "hurrah", "milestone"),
    Intent.CALM to listOf("calm", "relax", "relaxing", "relaxed", "peace", "peaceful", "breathe", "chill", "chilling", "serene", "serenity", "quiet", "meditate", "meditation", "unwind", "mindful", "gentle"),
    Intent.MOTIVATIONAL to listOf("believe", "hustle", "grind", "focus", "focused", "discipline", "stronger", "motivation", "motivated", "motivate", "persevere", "unstoppable", "determined", "perseverance", "conquer"),
    Intent.GRATEFUL to listOf("thanks", "thankful", "grateful", "gratitude", "appreciate", "appreciated", "blessed", "thx"),
)

/** Multi-word triggers, matched as a lowercased substring of the whole text (higher weight). */
val PHRASE_KEYWORDS: Map<String, Intent> = buildRawMap(
    Intent.FUNNY to listOf("so funny", "made me laugh", "cant stop laughing", "cracking up"),
    Intent.SAD to listOf("i miss you", "miss you", "feeling down", "broke my heart", "so sad"),
    Intent.ROMANTIC to listOf("i love you", "love you", "my heart", "my love", "be mine"),
    Intent.ANGRY to listOf("fed up", "had enough", "so mad", "this is unacceptable", "sick of"),
    Intent.INFORMATIVE to listOf("please note", "heads up", "for your information", "just so you know", "action items"),
    Intent.EXCITED to listOf("cant wait", "cannot wait", "so ready", "lets go", "here we go"),
    Intent.CELEBRATORY to listOf("happy birthday", "we did it", "way to go", "you nailed it", "job well done"),
    Intent.CALM to listOf("take it easy", "no rush", "deep breath", "its okay", "all good", "good night", "goodnight", "sweet dreams", "sleep well", "sleep tight", "sleep now"),
    Intent.MOTIVATIONAL to listOf("you got this", "keep going", "never give up", "push through", "dont quit", "one step at a time"),
    Intent.GRATEFUL to listOf("thank you", "means a lot", "means the world", "i appreciate"),
)

/** Emoji triggers, matched as a substring so multi-codepoint sequences (❤️, ☁️) still hit. */
val EMOJI_KEYWORDS: Map<String, Intent> = buildRawMap(
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
 * Loose sentiment → intent mapping used only when no keyword matched (todo.txt: "'positive'
 * sentiment can map to FUNNY, ROMANTIC, EXCITED, CELEBRATORY, MOTIVATIONAL, GRATEFUL"). One
 * of the listed intents is then picked at random.
 */
val SENTIMENT_INTENTS: Map<Sentiment, List<Intent>> = mapOf(
    Sentiment.POSITIVE to listOf(
        Intent.FUNNY, Intent.ROMANTIC, Intent.EXCITED,
        Intent.CELEBRATORY, Intent.MOTIVATIONAL, Intent.GRATEFUL,
    ),
    Sentiment.NEGATIVE to listOf(Intent.SAD, Intent.ANGRY),
    Sentiment.NEUTRAL to listOf(Intent.INFORMATIVE, Intent.CALM),
)

/**
 * A small curated emoji set per [Intent], used as the last resort by [AutoStyle.planFor] when
 * [EmojiLexicon] found no keyword match: since every message resolves to an intent, this makes
 * the ink-drop [EmojiLexicon.DEFAULT] effectively unreachable in the rendered image — a plain
 * "on my way" or an untranslated Hebrew line still gets a fitting mood emoji instead.
 */
val INTENT_EMOJIS: Map<Intent, List<String>> = mapOf(
    Intent.FUNNY to listOf("😂", "🤣", "😄"),
    Intent.SAD to listOf("😢", "🥺", "💙"),
    Intent.ROMANTIC to listOf("❤️", "🥰", "😘"),
    Intent.ANGRY to listOf("😤", "😠", "😑"),
    Intent.INFORMATIVE to listOf("📌", "📝", "💬"),
    Intent.EXCITED to listOf("🤩", "🎉", "✨"),
    Intent.CELEBRATORY to listOf("🎉", "🥳", "🎊"),
    Intent.CALM to listOf("🌿", "😌", "🍃"),
    Intent.MOTIVATIONAL to listOf("💪", "🔥", "⭐"),
    Intent.GRATEFUL to listOf("🙏", "💛", "🤗"),
)

/** A stable colour + glyph for the overlay button to preview the detected mood while typing. */
data class ButtonHint(val emoji: String, val backgroundColorHex: String)

private fun buildKeywordMap(vararg entries: Pair<Intent, List<String>>): Map<String, Intent> {
    val stemmer = PorterStemmer()
    val map = LinkedHashMap<String, Intent>()
    for ((intent, keywords) in entries) {
        for (keyword in keywords) map.putIfAbsent(stemmer.stem(keyword), intent)
    }
    return map
}

private fun buildRawMap(vararg entries: Pair<Intent, List<String>>): Map<String, Intent> {
    val map = LinkedHashMap<String, Intent>()
    for ((intent, keywords) in entries) {
        for (keyword in keywords) map.putIfAbsent(keyword.lowercase().nfc(), intent)
    }
    return map
}

object AutoStyle {

    private const val PHRASE_WEIGHT = 3
    private const val STEM_WEIGHT = 1
    private const val EMOJI_WEIGHT = 2

    /**
     * Scores every [Intent] against [text] (already English) and returns the strongest, or
     * null when nothing scores. Ties break toward the earlier enum entry, so the result is
     * stable for a given input.
     */
    fun detectIntent(text: String): Intent? {
        if (text.isBlank()) return null
        val lower = text.lowercase().nfc()
        val stems = TextTokens.stemSet(lower)

        val scores = HashMap<Intent, Int>()
        fun add(intent: Intent, weight: Int) { scores[intent] = (scores[intent] ?: 0) + weight }

        for ((phrase, intent) in PHRASE_KEYWORDS) if (lower.contains(phrase)) add(intent, PHRASE_WEIGHT)
        for (stem in stems) STEM_KEYWORDS[stem]?.let { add(it, STEM_WEIGHT) }
        for ((emoji, intent) in EMOJI_KEYWORDS) if (text.contains(emoji)) add(intent, EMOJI_WEIGHT)

        val best = Intent.ALL.maxByOrNull { scores[it] ?: 0 }
        return if (best != null && (scores[best] ?: 0) > 0) best else null
    }

    /**
     * The intent for [text]: [detectIntent], or — when that is null — a random one of the
     * intents [SentimentAnalyzer] maps the text's polarity to. Always returns an intent.
     */
    fun resolveIntent(text: String, random: Random = Random.Default): Intent =
        resolveIntent(originalText = text, translatedText = text, random = random)

    /**
     * Like [resolveIntent] but with the pre-translation [originalText] too: when the English
     * [translatedText] matched nothing and the original is Hebrew, [HebrewFallback] gets a
     * turn before the English sentiment analyser — so a message whose translation never ran
     * (model still downloading, offline) still styles by its actual mood instead of always
     * collapsing to a neutral look.
     */
    fun resolveIntent(originalText: String, translatedText: String, random: Random): Intent {
        detectIntent(translatedText)?.let { return it }
        if (Scripts.hasHebrew(originalText)) {
            HebrewFallback.keywordIntent(originalText)?.let { return it }
            HebrewFallback.sentiment(originalText)?.let { return SENTIMENT_INTENTS.getValue(it).random(random) }
        }
        return SENTIMENT_INTENTS.getValue(SentimentAnalyzer.analyze(translatedText)).random(random)
    }

    /**
     * The full [RenderPlan] for a message. [originalText] is the text as typed — its script
     * decides whether Hebrew or English fonts are used and it is what actually gets drawn;
     * [translatedText] is the English rendition that drives mood + emoji detection (pass the
     * same string for already-English input). Every pick is random, by design.
     */
    fun planFor(
        originalText: String,
        translatedText: String,
        random: Random = Random.Default,
    ): RenderPlan {
        val isHebrew = Scripts.hasHebrew(originalText)
        val intent = resolveIntent(originalText, translatedText, random)
        val fonts = if (isHebrew) intent.hebrewFonts else intent.englishFonts
        val font = fonts.random(random)
        val gradient = intent.gradients.random(random)
        var emojis = EmojiLexicon.select(TextTokens.stemList(translatedText), translatedText)
        if (emojis == listOf(EmojiLexicon.DEFAULT)) {
            val hebrew = if (isHebrew) HebrewFallback.emojis(originalText) else emptyList()
            emojis = hebrew.ifEmpty { INTENT_EMOJIS.getValue(intent) }
        }
        return RenderPlan(
            fontAssetPath = font.assetPath,
            fontName = font.displayName,
            gradientStartHex = gradient.startHex,
            gradientEndHex = gradient.endHex,
            textColorHex = contrastColorFor(gradient),
            emojis = emojis,
        )
    }

    /**
     * A non-random [ButtonHint] previewing [translatedText]'s mood as the user types: the
     * detected intent, or — when none matched — the first intent for the text's sentiment
     * (unless that is NEUTRAL with nothing else to go on, in which case null keeps the
     * button's ink-drop default).
     */
    fun buttonHintFor(translatedText: String): ButtonHint? {
        val intent = detectIntent(translatedText)
            ?: hebrewButtonIntent(translatedText)
            ?: run {
                val sentiment = SentimentAnalyzer.analyze(translatedText)
                if (sentiment == Sentiment.NEUTRAL) return null
                SENTIMENT_INTENTS.getValue(sentiment).first()
            }
        return ButtonHint(intent.previewEmoji, intent.gradients.first().startHex)
    }

    /** Non-random Hebrew hint used only while translation hasn't landed yet. */
    private fun hebrewButtonIntent(text: String): Intent? {
        if (!Scripts.hasHebrew(text)) return null
        HebrewFallback.keywordIntent(text)?.let { return it }
        return HebrewFallback.sentiment(text)
            ?.takeIf { it != Sentiment.NEUTRAL }
            ?.let { SENTIMENT_INTENTS.getValue(it).first() }
    }

    /**
     * Black (#1E1E2E) or white text, whichever contrasts better with the midpoint of
     * [gradient]. Pure hex maths so it stays JVM-testable — no android.graphics.
     */
    internal fun contrastColorFor(gradient: Gradient): String {
        val l = (relativeLuminance(gradient.startHex) + relativeLuminance(gradient.endHex)) / 2.0
        return if (l > 0.5) "#1E1E2E" else "#FFFFFF"
    }

    private fun relativeLuminance(hex: String): Double {
        val v = hex.removePrefix("#")
        if (v.length < 6) return 0.5
        val r = v.substring(0, 2).toInt(16)
        val g = v.substring(2, 4).toInt(16)
        val b = v.substring(4, 6).toInt(16)
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    }
}
