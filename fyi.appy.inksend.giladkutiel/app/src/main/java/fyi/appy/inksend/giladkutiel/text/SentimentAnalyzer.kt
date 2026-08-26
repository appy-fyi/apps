package fyi.appy.inksend.giladkutiel.text

import kotlin.math.sqrt

/** Coarse polarity of a piece of text. */
enum class Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

/**
 * A compact rules-based sentiment analyser in the style of VADER
 * (Hutto & Gilbert, 2014): a valence lexicon plus a few heuristics — negation flips the
 * sign, degree words ("very", "slightly") scale the magnitude, and trailing "!" adds
 * emphasis. Scores are combined and squashed to a [-1, 1] "compound" the same way VADER
 * does, with the same ±0.05 cutoffs.
 *
 * It is the **fallback** in the pipeline: [fyi.appy.inksend.giladkutiel.data.model.AutoStyle]
 * only reaches for it when no mood keyword, phrase or emoji matched, and then maps the
 * POSITIVE / NEGATIVE / NEUTRAL result loosely onto a set of intents. Runs on the
 * English-translated text. Pure JVM.
 */
object SentimentAnalyzer {

    private const val B_INCR = 0.293 // degree word, intensifying
    private const val B_DECR = -0.293 // degree word, dampening
    private const val C_INCR = 0.733 // ALL-CAPS emphasis
    private const val NEG_SCALAR = -0.74 // negation flips and slightly dampens
    private const val EXCLAIM_INCR = 0.292 // per '!', up to 3
    private const val CUTOFF = 0.05

    private val TOKEN = Regex("[^\\p{L}\\p{N}']+")

    private val NEGATIONS = hashSetOf(
        "not", "no", "never", "none", "nobody", "nothing", "neither", "nor", "nowhere",
        "cannot", "cant", "can't", "wont", "won't", "dont", "don't", "doesnt", "doesn't",
        "didnt", "didn't", "isnt", "isn't", "arent", "aren't", "wasnt", "wasn't", "werent",
        "weren't", "without", "lack", "lacks", "lacking", "hardly", "barely", "scarcely",
    )

    private val BOOSTERS: Map<String, Double> = buildMap {
        listOf(
            "absolutely", "amazingly", "completely", "considerably", "decidedly", "deeply",
            "enormously", "entirely", "especially", "exceptionally", "extremely", "fabulously",
            "fully", "greatly", "highly", "hugely", "incredibly", "intensely", "majorly",
            "more", "particularly", "purely", "quite", "really", "remarkably", "so",
            "substantially", "thoroughly", "totally", "tremendously", "uber", "unbelievably",
            "utterly", "very", "super", "way", "too",
        ).forEach { put(it, B_INCR) }
        listOf(
            "almost", "barely", "hardly", "kinda", "kindof", "less", "little", "marginally",
            "occasionally", "partly", "scarcely", "slightly", "somewhat", "sort", "sortof",
        ).forEach { put(it, B_DECR) }
    }

    /**
     * Valence lexicon: word → sentiment intensity on VADER's roughly [-4, 4] scale. A hand
     * picked subset — the common mood words a short chat message actually uses — is enough
     * for a three-way split; the full VADER lexicon is ~7.5k entries.
     */
    private val LEXICON: Map<String, Double> = buildMap {
        val positive = mapOf(
            "love" to 3.2, "loved" to 3.0, "loves" to 2.8, "adore" to 3.0, "like" to 1.5,
            "liked" to 1.4, "happy" to 2.7, "happiness" to 2.9, "glad" to 2.1, "joy" to 2.8,
            "joyful" to 2.7, "great" to 3.1, "good" to 1.9, "nice" to 1.8, "wonderful" to 3.0,
            "amazing" to 3.2, "awesome" to 3.1, "excellent" to 3.2, "fantastic" to 3.2,
            "perfect" to 3.0, "beautiful" to 2.9, "brilliant" to 2.8, "delight" to 2.8,
            "delighted" to 2.9, "excited" to 2.6, "exciting" to 2.5, "thrilled" to 2.9,
            "grateful" to 2.6, "thankful" to 2.4, "thanks" to 1.9, "thank" to 1.9,
            "appreciate" to 2.3, "blessed" to 2.5, "proud" to 2.3, "celebrate" to 2.6,
            "congrats" to 2.6, "congratulations" to 2.7, "win" to 2.4, "won" to 2.3,
            "success" to 2.5, "hope" to 1.9, "hopeful" to 2.1, "fun" to 2.3, "funny" to 1.9,
            "laugh" to 1.9, "smile" to 2.0, "yay" to 2.3, "hooray" to 2.6, "cheers" to 1.7,
            "safe" to 1.3, "calm" to 1.3, "peace" to 1.9, "peaceful" to 2.0, "relax" to 1.6,
            "relaxed" to 1.8, "cozy" to 1.6, "kind" to 2.0, "sweet" to 1.9,
            "best" to 3.0, "better" to 1.9, "enjoy" to 2.2, "enjoyed" to 2.2, "pleasure" to 2.5,
            "gorgeous" to 2.8, "lovely" to 2.6, "cool" to 1.4, "yes" to 1.0, "cute" to 2.0,
        )
        val negative = mapOf(
            "hate" to -3.2, "hated" to -3.1, "hates" to -3.0, "dislike" to -1.8, "angry" to -2.7,
            "anger" to -2.7, "mad" to -2.1, "furious" to -3.0, "rage" to -2.9, "annoyed" to -1.8,
            "annoying" to -1.9, "irritated" to -1.9, "sad" to -2.1, "sadness" to -2.3,
            "unhappy" to -2.1, "depressed" to -2.7, "depressing" to -2.5, "miserable" to -2.6,
            "lonely" to -2.2, "cry" to -1.7, "crying" to -1.8, "tears" to -1.4, "grief" to -2.6,
            "hurt" to -2.0, "hurts" to -2.0, "pain" to -2.2, "painful" to -2.3, "awful" to -2.8,
            "terrible" to -3.0, "horrible" to -3.0, "worst" to -3.1, "bad" to -2.5,
            "disappointed" to -2.3, "disappointing" to -2.2, "sorry" to -1.1, "afraid" to -1.9,
            "scared" to -2.0, "fear" to -2.2, "worried" to -1.8, "anxious" to -1.8,
            "stressed" to -1.9, "tired" to -1.2, "exhausted" to -1.6, "sick" to -1.6,
            "broke" to -1.4, "broken" to -1.9, "fail" to -2.3, "failed" to -2.4,
            "failure" to -2.5, "lost" to -1.5, "lose" to -1.6, "losing" to -1.6, "ugh" to -1.5,
            "hopeless" to -2.7, "useless" to -2.3, "hard" to -0.8, "difficult" to -1.2,
            "no" to -1.2, "never" to -1.0, "sucks" to -2.3, "suck" to -2.0, "damn" to -1.6,
            "gutted" to -2.4, "devastated" to -3.0, "unacceptable" to -2.0, "disgusting" to -2.9,
        )
        putAll(positive)
        putAll(negative)
    }

    fun analyze(text: String): Sentiment {
        if (text.isBlank()) return Sentiment.NEUTRAL

        val rawTokens = text.split(TOKEN).filter { it.isNotEmpty() }
        if (rawTokens.isEmpty()) return Sentiment.NEUTRAL
        val lower = rawTokens.map { it.lowercase() }

        var sum = 0.0
        for (i in lower.indices) {
            val word = lower[i].trim('\'')
            var valence = LEXICON[word] ?: continue

            // ALL-CAPS (in a sentence that has lower-case elsewhere) intensifies.
            if (rawTokens[i].length > 1 && rawTokens[i].all { it.isUpperCase() || !it.isLetter() } &&
                rawTokens.any { t -> t.any { it.isLowerCase() } }
            ) {
                valence += if (valence > 0) C_INCR else -C_INCR
            }

            // Degree words in the three tokens before it scale the magnitude.
            for (step in 1..3) {
                val prev = lower.getOrNull(i - step)?.trim('\'') ?: break
                val boost = BOOSTERS[prev] ?: continue
                val scaled = when (step) { 1 -> boost; 2 -> boost * 0.95; else -> boost * 0.9 }
                valence += if (valence > 0) scaled else -scaled
            }

            // Negation anywhere in the three preceding tokens flips the sign.
            val negated = (1..3).any { step -> lower.getOrNull(i - step)?.trim('\'') in NEGATIONS }
            if (negated) valence *= NEG_SCALAR

            sum += valence
        }

        if (sum == 0.0) return Sentiment.NEUTRAL

        // Trailing exclamation marks add emphasis in the direction of the current sum.
        val exclaims = text.count { it == '!' }.coerceAtMost(3)
        if (exclaims > 0) sum += exclaims * EXCLAIM_INCR * if (sum > 0) 1 else -1

        val compound = sum / sqrt(sum * sum + 15.0)
        return when {
            compound >= CUTOFF -> Sentiment.POSITIVE
            compound <= -CUTOFF -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }
}
