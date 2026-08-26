package fyi.appy.inksend.giladkutiel.data.model

import fyi.appy.inksend.giladkutiel.text.Sentiment
import fyi.appy.inksend.giladkutiel.text.nfc

/**
 * A small Hebrew safety net for when on-device translation did not run — the model is still
 * downloading, the device is offline, or ML Kit misreports the language. Without it, raw
 * Hebrew reaches [AutoStyle.detectIntent] as zero stems and no English keyword, every message
 * classifies as neutral, and the styling collapses to a random CALM / INFORMATIVE look.
 *
 * Hebrew is not stemmable with Porter and takes clitic prefixes (ו/ה/ב/ל/כ/מ/ש) and
 * pronominal suffixes, so matching here is deliberately **substring** on the NFC-normalised
 * text rather than whole-token: "אוהב" still fires inside "שאוהב" or "אוהבת".
 */
object HebrewFallback {

    private val KEYWORDS: List<Pair<Intent, List<String>>> = listOf(
        Intent.ROMANTIC to listOf("אוהב", "אהבה", "אהוב", "מתגעג", "נשיק", "חמוד", "יקירי", "מותק", "חיים שלי", "נשמה שלי"),
        Intent.CALM to listOf("לילה טוב", "לילהטוב", "חלומות", "שינה", "נרג", "רגוע", "שלווה", "בשקט", "נשום", "לאט"),
        Intent.FUNNY to listOf("מצחיק", "חחח", "בדיח", "צחוק", "מגניב", "מגחיך"),
        Intent.SAD to listOf("עצוב", "בוכה", "בכי", "דואב", "מדוכא", "געגוע", "כואב", "שבור"),
        Intent.ANGRY to listOf("כועס", "עצבני", "זועם", "שונא", "מעצבן", "נמאס", "מטורף מכעס"),
        Intent.GRATEFUL to listOf("תודה", "מודה", "אסיר תודה", "מעריך", "רוב תודות"),
        Intent.CELEBRATORY to listOf("מזל טוב", "מזלטוב", "חוגג", "לחגוג", "נחגוג", "חגיג", "מסיב", "יום הולדת", "הצלח"),
        Intent.EXCITED to listOf("מתרגש", "וואו", "מתלהב", "לא מאמין", "אין עליך"),
        Intent.MOTIVATIONAL to listOf("כל הכבוד", "אתה יכול", "את יכולה", "תמשיך", "אל תוותר", "קדימה", "חזק"),
        Intent.INFORMATIVE to listOf("לידיעת", "תזכורת", "עדכון", "פגישה", "לתשומת", "הודעה", "סדר יום", "דדליין"),
    )

    private val POSITIVE_WORDS = listOf(
        "טוב", "אוהב", "שמח", "מדהים", "נהדר", "כיף", "תודה", "יפה", "מעולה", "מאושר",
        "אהבה", "כל הכבוד", "מזל טוב", "מתרגש", "נפלא", "חמוד", "מבורך",
    )
    private val NEGATIVE_WORDS = listOf(
        "רע", "שונא", "עצוב", "כועס", "נורא", "גרוע", "בוכה", "מפחיד", "נמאס", "דואב",
        "מדוכא", "כואב", "שבור", "מאוכזב", "זועם", "מעצבן",
    )

    /** A representative emoji per intent, for the Hebrew keyword path (parity with the English lexicon). */
    private val INTENT_EMOJI: Map<Intent, String> = mapOf(
        Intent.ROMANTIC to "❤️", Intent.CALM to "🌙", Intent.FUNNY to "😂", Intent.SAD to "😢",
        Intent.ANGRY to "😠", Intent.GRATEFUL to "🙏", Intent.CELEBRATORY to "🎉",
        Intent.EXCITED to "🤩", Intent.MOTIVATIONAL to "💪", Intent.INFORMATIVE to "📌",
    )

    /**
     * Hebrew word → emoji, matched as a **substring** of the NFC text so clitic prefixes
     * (ב/ה/ל/כ/מ/ש/ו) and suffixes don't block a hit ("בבית" still matches "בית"). Multi-word
     * and more-specific needles come first so "בית חולים" resolves to 🏥, not 🏠. This is the
     * Hebrew counterpart of [EmojiLexicon]'s concept table — it keeps an untranslated Hebrew
     * message from collapsing to the ink drop.
     */
    private val WORD_EMOJI: List<Pair<String, String>> = listOf(
        // multi-word / specific first
        "בוקר טוב" to "☀️", "לילה טוב" to "🌙", "יום הולדת" to "🎂", "בית ספר" to "🏫",
        "בית חולים" to "🏥", "כל הכבוד" to "👏", "עבודה טובה" to "👏", "מזל טוב" to "🎉",
        "יום כיף" to "🎈", "סוף שבוע" to "🎉", "שבת שלום" to "🕯️", "חג שמח" to "🎊",
        // greetings / social
        "שלום" to "👋", "להתראות" to "👋", "ביי" to "👋", "נתראה" to "👋", "תודה" to "🙏",
        "בבקשה" to "🙏", "סליחה" to "🙏", "מצטער" to "🙏", "ברכות" to "🎉",
        // love / people / animals
        "אוהב" to "❤️", "אהבה" to "❤️", "מתגעג" to "🥺", "נשיק" to "😘", "חיבוק" to "🤗",
        "מתוק" to "🥰", "יקיר" to "❤️", "אמא" to "👩", "אבא" to "👨", "ילד" to "🧒",
        "תינוק" to "👶", "משפחה" to "👨‍👩‍👧", "חבר" to "🧑‍🤝‍🧑", "חתונה" to "💒", "כלב" to "🐶",
        "חתול" to "🐱", "ציפור" to "🐦",
        // feelings
        "שמח" to "😊", "מאושר" to "😄", "עצוב" to "😢", "בוכה" to "😭", "בכי" to "😭",
        "כועס" to "😠", "עצבני" to "😤", "עייף" to "😴", "מתרגש" to "🤩", "מפחד" to "😨",
        "גאה" to "🥲", "מאוכזב" to "😞", "צוחק" to "😂", "מצחיק" to "🤣", "חחח" to "🤣",
        "משעמם" to "😑", "מודאג" to "😟", "בלחץ" to "😰", "רגוע" to "😌", "נרגש" to "🤩",
        // food / drink
        "אוכל" to "🍽️", "ארוח" to "🍽️", "מסעד" to "🍽️", "קפה" to "☕", "תה" to "🍵",
        "בירה" to "🍺", "יין" to "🍷", "עוגה" to "🍰", "פיצה" to "🍕", "לחם" to "🍞",
        "גלידה" to "🍦", "שוקולד" to "🍫", "בשר" to "🍖", "סלט" to "🥗", "פירות" to "🍎",
        "מים" to "💧", "בוקר" to "🌅",
        // places / travel
        "בית" to "🏠", "עבוד" to "💼", "משרד" to "🏢", "אוניברסיט" to "🎓", "מכונית" to "🚗",
        "אוטו" to "🚗", "רכבת" to "🚆", "מטוס" to "✈️", "טיס" to "✈️", "אוטובוס" to "🚌",
        "אופניים" to "🚴", "חוף" to "🏖️", "חופש" to "🏝️", "טיול" to "🧳", "מלון" to "🏨",
        "הים" to "🌊", "לפארק" to "🌳",
        // time / logistics
        "מחר" to "📅", "היום" to "📅", "אתמול" to "📅", "ערב" to "🌆", "לילה" to "🌙",
        "עכשיו" to "⏰", "מאוחר" to "⏳", "מוקדם" to "⏰", "מהר" to "🏃", "בדרך" to "🚗",
        "פגיש" to "📅", "תור" to "📅", "שעה" to "⏰", "זמן" to "⏳", "דקה" to "⏱️",
        "דקות" to "⏱️",
        // weather / nature
        "שמש" to "☀️", "גשם" to "🌧️", "שלג" to "❄️", "ענן" to "☁️", "רוח" to "🌬️",
        "חם לי" to "🥵", "קר לי" to "🥶", "פרח" to "🌸", "עץ" to "🌳", "כוכב" to "⭐",
        "ירח" to "🌙", "קשת בענן" to "🌈",
        // work / study / tech / money
        "טלפון" to "📱", "מחשב" to "💻", "מייל" to "📧", "הודע" to "💬", "שיח" to "📞",
        "מבחן" to "📝", "לימוד" to "📚", "ספר" to "📖", "כסף" to "💰", "קניות" to "🛒",
        "חנות" to "🏬", "פרויקט" to "📊", "דוח" to "📊", "תזכור" to "📌", "עדכון" to "📣",
        // activities / health / misc
        "מוזיק" to "🎵", "שיר" to "🎶", "סרט" to "🎬", "משחק" to "🎮", "כדורגל" to "⚽",
        "ספורט" to "🏃", "ריצ" to "🏃", "יוגה" to "🧘", "בריא" to "💪", "חול" to "🤒",
        "רופא" to "🩺", "מתנה" to "🎁", "מסיב" to "🥳", "הצלח" to "🏆", "ניצחון" to "🥇",
        "בהצלחה" to "🍀", "חזק" to "💪", "נהדר" to "✨", "מדהים" to "🤩", "מושלם" to "👌",
        "מגניב" to "😎", "וואו" to "😮", "בסדר גמור" to "👌", "אולי" to "🤔", "בול" to "🎯",
    )

    /** An intent for raw Hebrew [text] by substring keyword hit, most-specific list first, or null. */
    fun keywordIntent(text: String): Intent? {
        val hay = text.nfc()
        for ((intent, needles) in KEYWORDS) {
            if (needles.any { hay.contains(it) }) return intent
        }
        return null
    }

    /**
     * Up to [max] emojis for raw Hebrew [text]: concept words from [WORD_EMOJI] first (in list
     * order, so the more-specific needles win), then the coarser intent-keyword emojis — so an
     * untranslated Hebrew message still gets a fitting emoji strip instead of only the ink drop.
     * Empty only when nothing at all matched (the caller keeps its own default).
     */
    fun emojis(text: String, max: Int = 3): List<String> {
        val hay = text.nfc()
        val out = LinkedHashSet<String>()
        for ((needle, emoji) in WORD_EMOJI) {
            if (out.size >= max) break
            if (hay.contains(needle)) out.add(emoji)
        }
        for ((intent, needles) in KEYWORDS) {
            if (out.size >= max) break
            if (needles.any { hay.contains(it) }) INTENT_EMOJI[intent]?.let { out.add(it) }
        }
        return out.toList()
    }

    /** Coarse polarity for raw Hebrew [text] by counting valence-word hits, or null when none hit. */
    fun sentiment(text: String): Sentiment? {
        val hay = text.nfc()
        val pos = POSITIVE_WORDS.count { hay.contains(it) }
        val neg = NEGATIVE_WORDS.count { hay.contains(it) }
        return when {
            pos == 0 && neg == 0 -> null
            neg > pos -> Sentiment.NEGATIVE
            pos > neg -> Sentiment.POSITIVE
            else -> Sentiment.NEUTRAL
        }
    }
}
