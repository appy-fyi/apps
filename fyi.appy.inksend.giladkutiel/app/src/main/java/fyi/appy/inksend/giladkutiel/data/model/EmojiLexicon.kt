package fyi.appy.inksend.giladkutiel.data.model

import fyi.appy.inksend.giladkutiel.text.PorterStemmer
import fyi.appy.inksend.giladkutiel.text.TextTokens

/**
 * Maps the words a message uses to emojis for the rendered image. The pipeline picks **up to
 * three** emojis for the bottom strip: any emoji the user already typed (kept in the order
 * they appear), then one per matching keyword stem, in first-seen order. When nothing
 * matches at all it falls back to the single ink-drop [DEFAULT] — the app's namesake glyph.
 *
 * [STEM_TO_EMOJI] is keyed by [PorterStemmer] output: each human-readable keyword below is
 * run through the same stemmer [TextTokens] uses at query time, so "party", "partying" and
 * "parties" all resolve to the one 🎉 entry that "party" seeds. There are well over 100
 * distinct emojis here, spread across faces, feelings, symbols, nature, animals, food,
 * travel, activities and objects.
 */
object EmojiLexicon {

    /** Unicode 16 "splatter" (ink drop, 🫟) — the app's namesake, shown when nothing matches. */
    const val DEFAULT: String = "🫟"

    private val KEYWORDS: List<Pair<String, String>> = listOf(
        // faces & feelings
        "happy" to "😊", "joy" to "😄", "glad" to "😁", "laugh" to "😂", "funny" to "🤣",
        "smile" to "🙂", "grin" to "😃", "wink" to "😉", "love" to "😍", "crush" to "🥰",
        "kiss" to "😘", "hug" to "🤗", "cry" to "😢", "sob" to "😭", "sad" to "😞",
        "tear" to "💧", "angry" to "😠", "rage" to "😡", "furious" to "🤬", "annoyed" to "😤",
        "wow" to "😮", "shock" to "😱", "scared" to "😨", "worried" to "😟", "nervous" to "😰",
        "tired" to "😩", "sleep" to "😴", "bored" to "😑", "sick" to "🤒", "cool" to "😎",
        "nerd" to "🤓", "think" to "🤔", "silly" to "🤪", "rich" to "🤑", "sunglasses" to "🕶️",
        "confused" to "😕", "relieved" to "😌", "smirk" to "😏", "yawn" to "🥱", "hot" to "🥵",
        "cold" to "🥶", "hungry" to "😋", "party" to "🥳", "angel" to "😇", "devil" to "😈",
        "ghost" to "👻", "alien" to "👽", "robot" to "🤖", "clown" to "🤡", "poop" to "💩",
        "skull" to "💀",
        // gestures & people
        "clap" to "👏", "pray" to "🙏", "thanks" to "🙏", "strong" to "💪", "muscle" to "💪",
        "ok" to "👌", "yes" to "👍", "no" to "👎", "wave" to "👋", "hi" to "👋",
        "point" to "👉", "fist" to "✊", "handshake" to "🤝", "write" to "✍️", "run" to "🏃",
        "walk" to "🚶", "dance" to "💃", "swim" to "🏊", "yoga" to "🧘", "baby" to "👶",
        "family" to "👨‍👩‍👧", "friend" to "🧑‍🤝‍🧑", "king" to "🤴", "queen" to "👸", "wedding" to "💒",
        // symbols
        "fire" to "🔥", "star" to "⭐", "sparkle" to "✨", "boom" to "💥", "warning" to "⚠️",
        "check" to "✅", "wrong" to "❌", "question" to "❓", "important" to "❗", "hundred" to "💯",
        "idea" to "💡", "bell" to "🔔", "lock" to "🔒", "key" to "🔑", "gift" to "🎁",
        "balloon" to "🎈", "crown" to "👑", "trophy" to "🏆", "win" to "🥇", "medal" to "🏅",
        "celebrate" to "🎉", "congrats" to "🎉", "cheers" to "🥂", "festival" to "🎊",
        "target" to "🎯", "goal" to "🎯", "money" to "💰", "cash" to "💵", "chart" to "📈",
        "growth" to "📈", "heart" to "❤️", "heartbeat" to "💓", "peace" to "☮️", "infinity" to "♾️",
        "recycle" to "♻️", "magic" to "🪄", "luck" to "🍀", "diamond" to "💎", "ring" to "💍",
        "crystal" to "🔮",
        // nature & weather
        "sun" to "☀️", "moon" to "🌙", "cloud" to "☁️", "rain" to "🌧️", "snow" to "❄️",
        "storm" to "⛈️", "rainbow" to "🌈", "flower" to "🌸", "rose" to "🌹", "tree" to "🌳",
        "leaf" to "🍃", "plant" to "🌱", "cactus" to "🌵", "mountain" to "⛰️", "ocean" to "🌊",
        "wave" to "🌊", "earth" to "🌍", "lightning" to "⚡", "wind" to "🌬️", "sunrise" to "🌅",
        "night" to "🌌", "sprout" to "🌿",
        // animals
        "dog" to "🐶", "cat" to "🐱", "bird" to "🐦", "fish" to "🐟", "lion" to "🦁",
        "tiger" to "🐯", "bear" to "🐻", "panda" to "🐼", "monkey" to "🐵", "rabbit" to "🐰",
        "horse" to "🐴", "unicorn" to "🦄", "butterfly" to "🦋", "bee" to "🐝", "snake" to "🐍",
        "dragon" to "🐉", "penguin" to "🐧", "owl" to "🦉", "turtle" to "🐢", "whale" to "🐳",
        // food & drink
        "coffee" to "☕", "tea" to "🍵", "beer" to "🍺", "wine" to "🍷", "cake" to "🍰",
        "birthday" to "🎂", "pizza" to "🍕", "burger" to "🍔", "apple" to "🍎", "banana" to "🍌",
        "bread" to "🍞", "icecream" to "🍦", "candy" to "🍬", "cookie" to "🍪", "chocolate" to "🍫",
        "taco" to "🌮", "sushi" to "🍣", "egg" to "🥚", "salad" to "🥗", "honey" to "🍯",
        // travel & places
        "car" to "🚗", "plane" to "✈️", "rocket" to "🚀", "train" to "🚆", "ship" to "🚢",
        "boat" to "⛵", "bike" to "🚴", "home" to "🏠", "house" to "🏠", "building" to "🏢",
        "school" to "🏫", "hospital" to "🏥", "church" to "⛪", "beach" to "🏖️", "tent" to "⛺",
        "map" to "🗺️", "flag" to "🚩", "world" to "🌐", "road" to "🛣️", "bridge" to "🌉",
        // objects, media & work
        "music" to "🎵", "song" to "🎶", "guitar" to "🎸", "piano" to "🎹", "drum" to "🥁",
        "mic" to "🎤", "sing" to "🎤", "paint" to "🎨", "art" to "🎨", "camera" to "📷",
        "photo" to "📸", "movie" to "🎬", "film" to "🎬", "book" to "📚", "read" to "📖",
        "pen" to "🖊️", "note" to "📝", "email" to "📧", "message" to "💬", "chat" to "💬",
        "news" to "📰", "phone" to "📱", "call" to "📞", "computer" to "💻", "code" to "💻",
        "time" to "⏰", "clock" to "⏰", "calendar" to "📅", "schedule" to "🗓️", "meeting" to "📅",
        "deadline" to "⏳", "work" to "💼", "study" to "📚", "science" to "🔬", "rocketship" to "🚀",
        "tool" to "🛠️", "search" to "🔍", "info" to "ℹ️", "pin" to "📌", "link" to "🔗",
        "battery" to "🔋", "bulb" to "💡", "puzzle" to "🧩", "game" to "🎮", "dice" to "🎲",
        "ball" to "⚽", "medal2" to "🎖️", "flower2" to "🌼", "candle" to "🕯️",
        // acknowledgement, reactions & judgement — the everyday glue words that used to miss
        "nice" to "👍", "great" to "🙌", "awesome" to "🤩", "amazing" to "🤩", "perfect" to "👌",
        "wonderful" to "✨", "excellent" to "🌟", "brilliant" to "💡", "fantastic" to "🎉",
        "okay" to "👌", "sure" to "👍", "fine" to "🙂", "agree" to "🤝", "correct" to "✅",
        "exactly" to "💯", "true" to "✅", "false" to "❌", "maybe" to "🤔", "sorry" to "😔",
        "oops" to "😬", "ugh" to "😮‍💨", "meh" to "😐", "finally" to "🙌", "welcome" to "🤗",
        "please" to "🙏", "help" to "🆘", "problem" to "⚠️", "issue" to "⚠️", "fix" to "🔧",
        "broken" to "🔨", "lost" to "😟", "found" to "🔎", "ready" to "✅", "busy" to "😵‍💫",
        "done" to "✔️", "start" to "▶️", "stop" to "⏹️", "finish" to "🏁", "wow" to "😮",
        "yikes" to "😬", "phew" to "😅", "hooray" to "🎉", "congrats2" to "🎊",
        // time & logistics
        "late" to "⏰", "early" to "🌅", "wait" to "⏳", "soon" to "⏳", "hurry" to "🏃",
        "today" to "📅", "tomorrow" to "📆", "yesterday" to "🗓️", "morning" to "🌅",
        "evening" to "🌆", "tonight" to "🌙", "weekend" to "🎉", "minute" to "⏱️",
        "hour" to "⏰", "week" to "📅", "month" to "🗓️", "year" to "📆", "holiday" to "🎉",
        "vacation" to "🏝️", "trip" to "🧳", "drive" to "🚗", "arrive" to "📍", "leave" to "👋",
        "route" to "🗺️", "traffic" to "🚦", "airport" to "🛫", "station" to "🚉",
        // communication
        "ask" to "❓", "answer" to "💬", "reply" to "↩️", "tell" to "🗣️", "talk" to "🗣️",
        "speak" to "🗣️", "listen" to "👂", "hear" to "👂", "invite" to "💌", "plan" to "🗓️",
        "promise" to "🤞", "agreement" to "🤝", "voice" to "🎙️", "text2" to "💬",
        // food & drink (more)
        "water" to "💧", "milk" to "🥛", "juice" to "🧃", "rice" to "🍚", "soup" to "🍲",
        "sandwich" to "🥪", "breakfast" to "🍳", "lunch" to "🍱", "dinner" to "🍽️",
        "snack" to "🍿", "fruit" to "🍓", "vegetable" to "🥦", "grocery" to "🛒",
        "cook" to "🍳", "bake" to "🧁", "spicy" to "🌶️", "hungry2" to "🍽️", "thirsty" to "🥤",
        "noodles" to "🍜", "donut" to "🍩", "popcorn" to "🍿", "pancake" to "🥞",
        // shopping & money (more)
        "shop" to "🛍️", "store" to "🏬", "market" to "🏪", "mall" to "🏬", "buy" to "🛒",
        "sell" to "🏷️", "pay" to "💳", "card2" to "💳", "wallet" to "👛", "bill" to "🧾",
        "price" to "🏷️", "sale" to "🏷️", "discount" to "🏷️", "bank" to "🏦", "salary" to "💰",
        "invest" to "📈", "budget" to "📊", "coin" to "🪙", "receipt" to "🧾",
        // home & objects (more)
        "door" to "🚪", "window" to "🪟", "bed" to "🛏️", "chair" to "🪑", "table" to "🪑",
        "kitchen" to "🍳", "bathroom" to "🚿", "shower" to "🚿", "light" to "💡", "lamp" to "💡",
        "tv" to "📺", "bag" to "👜", "box" to "📦", "umbrella" to "☂️", "glasses" to "👓",
        "watch2" to "⌚", "shoe" to "👟", "shirt" to "👕", "dress" to "👗", "hat" to "🎩",
        "coat" to "🧥", "sock" to "🧦", "towel" to "🧻", "soap" to "🧼", "mirror" to "🪞",
        "clothes" to "👕", "laundry" to "🧺", "trash" to "🗑️", "broom" to "🧹",
        // health & body (more)
        "doctor" to "🩺", "nurse" to "💉", "medicine" to "💊", "pill" to "💊", "pain" to "🤕",
        "headache" to "🤕", "fever" to "🤒", "cough" to "😷", "mask" to "😷", "rest" to "😌",
        "gym" to "🏋️", "workout" to "🏋️", "exercise" to "🤸", "health" to "🩺",
        "dentist" to "🦷", "tooth" to "🦷", "eye" to "👁️", "hand" to "✋", "foot" to "🦶",
        "brain" to "🧠", "bone" to "🦴", "blood" to "🩸", "bandage" to "🩹",
        // feelings (more)
        "proud" to "🥲", "stress" to "😫", "anxious" to "😰", "afraid" to "😨", "brave" to "🦁",
        "shy" to "😳", "jealous" to "😒", "embarrassed" to "😳", "surprised" to "😲",
        "amazed" to "🤩", "curious" to "🧐", "hopeful" to "🤞", "disappointed" to "😞",
        "frustrated" to "😤", "exhausted" to "😩", "overwhelmed" to "😵", "grumpy" to "😠",
        "calm2" to "😌", "excited2" to "🤗", "content" to "😌", "lonely" to "🥺",
        // weather & nature (more)
        "warm" to "🌤️", "humid" to "💦", "fog" to "🌫️", "thunder" to "⛈️", "ice" to "🧊",
        "heat" to "🥵", "breeze" to "🍃", "garden" to "🪴", "grass" to "🌱", "forest" to "🌲",
        "river" to "🏞️", "lake" to "🏞️", "sky" to "🌤️", "hill" to "⛰️", "desert" to "🏜️",
        "island" to "🏝️", "park" to "🏞️", "hike" to "🥾", "climb" to "🧗", "sunset" to "🌇",
        "meadow" to "🌾", "field" to "🌾", "volcano" to "🌋", "cave" to "🕳️",
        // activities & misc
        "travel" to "🧳", "jog" to "🏃", "nap" to "😴", "draw" to "✏️", "play" to "🎮",
        "lose" to "😞", "practice" to "🎯", "win2" to "🏆", "team" to "🧑‍🤝‍🧑", "match" to "🏟️",
        "race" to "🏁", "chess" to "♟️", "cards" to "🃏", "gardening" to "🪴", "fishing" to "🎣",
        "camping" to "⛺", "picnic" to "🧺", "concert" to "🎫", "ticket" to "🎫",
        "festival2" to "🎪", "parade" to "🎏", "fireworks" to "🎆",
        // tech (more)
        "wifi" to "📶", "app" to "📱", "website" to "🌐", "download" to "⬇️", "upload" to "⬆️",
        "charger" to "🔌", "screen" to "🖥️", "keyboard" to "⌨️", "password" to "🔒",
        "login" to "🔑", "click" to "🖱️", "video" to "📹", "stream" to "📡", "post" to "📮",
        "share" to "🔗", "follow" to "➡️", "notification" to "🔔", "wifi2" to "📶",
        "printer" to "🖨️", "mouse2" to "🖱️", "usb" to "🔌", "signal" to "📶",
        // quantities & qualities
        "new" to "🆕", "fast" to "💨", "slow" to "🐌", "free" to "🆓", "full" to "💯",
        "top" to "🔝", "first" to "🥇", "third" to "🥉", "huge" to "🐘", "tiny" to "🐜",
    )

    /** Stem → emoji, first entry wins on stem collisions. */
    val STEM_TO_EMOJI: Map<String, String> = buildMap {
        val stemmer = PorterStemmer()
        for ((word, emoji) in KEYWORDS) putIfAbsent(stemmer.stem(word), emoji)
    }

    /** Every distinct emoji this lexicon can produce — 100+, used for "already typed" hits. */
    val ALL_EMOJIS: List<String> = KEYWORDS.map { it.second }.distinct()

    /**
     * Up to [max] emojis for [translatedText] given its precomputed [stems]: emojis the user
     * already typed first (in the order they appear), then one per matching keyword stem in
     * first-seen order, de-duplicated. Falls back to [DEFAULT] when the result would be empty.
     */
    fun select(stems: List<String>, translatedText: String, max: Int = 3): List<String> {
        val picked = LinkedHashSet<String>()

        ALL_EMOJIS.asSequence()
            .filter { translatedText.contains(it) }
            .sortedBy { translatedText.indexOf(it) }
            .forEach { if (picked.size < max) picked.add(it) }

        for (stem in stems) {
            if (picked.size >= max) break
            STEM_TO_EMOJI[stem]?.let { picked.add(it) }
        }

        return if (picked.isEmpty()) listOf(DEFAULT) else picked.take(max)
    }

    /** Convenience overload that tokenizes [translatedText] itself. */
    fun select(translatedText: String, max: Int = 3): List<String> =
        select(TextTokens.stemList(translatedText), translatedText, max)
}
