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
 * Note on languages: the original plan called for translating the text to English first. That
 * needs an on-device translation model (ML Kit) and a multi-MB language pack download, which
 * is a poor fit for an otherwise fully offline, no-network app. Instead the keyword tables
 * carry the common triggers directly in all six languages the app's UI supports — English,
 * Spanish, French, German, Portuguese, and Hindi — grouped per language in [WORD_KEYWORDS] /
 * [PHRASE_KEYWORDS] so each set is easy to extend. Latin-script entries include accent-folded
 * spellings (e.g. "drole" alongside "drôle") since many keyboards and users omit accents.
 * Emoji, being language-independent, cover everything else.
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

/** Flattens the per-language word groups passed for one intent into a single list. */
private fun langs(vararg groups: List<String>): List<String> = groups.flatMap { it }

/**
 * Single-word triggers, keyed by the lowercased word. A word is matched only when it appears
 * as a whole token in the text, so "informative" here never fires on "misinformative". Each
 * intent's list is grouped by language (en / es / fr / de / pt / hi) for easy extension.
 */
val WORD_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to langs(
        listOf("funny", "lol", "lmao", "lmfao", "rofl", "haha", "hahaha", "hehe", "joke", "joking", "hilarious", "comedy", "laugh", "laughing", "laughed", "meme", "silly", "ridiculous", "lolz", "giggle"),
        listOf("gracioso", "graciosa", "divertido", "divertida", "chistoso", "jaja", "jajaja", "jajajaja", "risa", "reír", "reir", "chiste", "cómico", "comico", "broma"),
        listOf("drôle", "drole", "mdr", "ptdr", "rigolo", "marrant", "marrante", "blague", "rire", "hilarant", "comique"),
        listOf("lustig", "witzig", "komisch", "lachen", "witz", "humor", "scherz"),
        listOf("engraçado", "engracado", "kkkk", "kkkkk", "rsrs", "piada", "rir", "hilário", "hilario", "brincadeira"),
        listOf("मज़ेदार", "मजेदार", "हँसी", "हंसी", "मजाक", "मज़ाक", "चुटकुला", "हाहा", "हास्य", "मज़ाकिया"),
    ),
    Intent.SAD to langs(
        listOf("sad", "sadness", "unhappy", "cry", "crying", "cried", "tears", "heartbroken", "depressed", "depressing", "miserable", "lonely", "grief", "grieving", "sorrow", "hurts", "hurting", "devastated", "gutted"),
        listOf("triste", "tristeza", "llorar", "llorando", "lágrimas", "lagrimas", "deprimido", "deprimida", "solo", "sola", "soledad", "dolido", "desanimado", "pena"),
        listOf("triste", "tristesse", "pleurer", "pleure", "larmes", "déprimé", "deprime", "seul", "seule", "solitude", "chagrin", "malheureux"),
        listOf("traurig", "traurigkeit", "weinen", "tränen", "tranen", "deprimiert", "einsam", "einsamkeit", "kummer", "trauer", "verzweifelt", "unglücklich", "unglucklich"),
        listOf("triste", "tristeza", "chorar", "chorando", "lágrimas", "lagrimas", "deprimido", "deprimida", "sozinho", "sozinha", "solidão", "solidao", "mágoa", "magoa", "chateado"),
        listOf("उदास", "दुखी", "दुःखी", "रोना", "आँसू", "आंसू", "अकेला", "अकेली", "तन्हा", "ग़म", "गम", "निराश", "दुख"),
    ),
    Intent.ROMANTIC to langs(
        listOf("love", "loved", "loving", "romantic", "romance", "darling", "sweetheart", "babe", "honey", "kiss", "kisses", "crush", "adore", "adorable", "forever", "valentine", "cutie", "beloved"),
        listOf("amor", "amo", "amar", "cariño", "carino", "querida", "querido", "beso", "besos", "enamorado", "enamorada", "corazón", "corazon", "novia", "novio"),
        listOf("amour", "aime", "chéri", "cheri", "chérie", "cherie", "bisou", "bisous", "coeur", "cœur", "amoureux", "amoureuse"),
        listOf("liebe", "liebling", "schatz", "kuss", "küsse", "kusse", "herz", "verliebt", "romantisch"),
        listOf("amor", "amo", "querida", "querido", "carinho", "beijo", "beijos", "coração", "coracao", "apaixonado", "apaixonada", "namorada", "namorado", "saudade"),
        listOf("प्यार", "प्रेम", "मोहब्बत", "इश्क़", "इश्क", "दिल", "जान", "प्रिय", "प्रेमिका", "प्रेमी", "रोमांटिक"),
    ),
    Intent.ANGRY to langs(
        listOf("angry", "mad", "furious", "hate", "rage", "raging", "pissed", "annoyed", "annoying", "unacceptable", "outrageous", "livid", "irritated", "ugh", "fuming"),
        listOf("enojado", "enojada", "enfadado", "enfadada", "enfado", "furioso", "furiosa", "rabia", "odio", "molesto", "molesta", "harto", "harta", "indignado"),
        listOf("colère", "colere", "fâché", "fache", "fâchée", "furieux", "furieuse", "déteste", "deteste", "énervé", "enerve", "agacé", "agace", "marre"),
        listOf("wütend", "wutend", "sauer", "zornig", "wut", "hass", "hasse", "verärgert", "verargert", "genervt", "empört", "emport"),
        listOf("raiva", "bravo", "brava", "irritado", "irritada", "furioso", "furiosa", "ódio", "odio", "revoltado", "revoltada", "chateado"),
        listOf("गुस्सा", "ग़ुस्सा", "क्रोध", "नाराज़", "नाराज", "चिढ़", "नफ़रत", "नफरत", "खफा", "झुंझलाहट"),
    ),
    Intent.INFORMATIVE to langs(
        listOf("fyi", "note", "notice", "update", "reminder", "info", "information", "details", "schedule", "meeting", "agenda", "report", "summary", "deadline", "instructions", "recap", "memo", "briefing", "announcement"),
        listOf("nota", "aviso", "información", "informacion", "recordatorio", "detalles", "reunión", "reunion", "informe", "resumen", "plazo", "instrucciones", "anuncio", "actualización", "actualizacion"),
        listOf("info", "information", "rappel", "détails", "details", "réunion", "reunion", "rapport", "résumé", "resume", "échéance", "echeance", "annonce", "compte-rendu"),
        listOf("hinweis", "notiz", "erinnerung", "besprechung", "sitzung", "tagesordnung", "bericht", "zusammenfassung", "frist", "anleitung", "ankündigung", "ankundigung"),
        listOf("nota", "aviso", "informação", "informacao", "lembrete", "detalhes", "reunião", "reuniao", "pauta", "relatório", "relatorio", "resumo", "prazo", "instruções", "instrucoes", "comunicado"),
        listOf("सूचना", "जानकारी", "नोट", "अनुस्मारक", "याद", "विवरण", "बैठक", "मीटिंग", "एजेंडा", "रिपोर्ट", "सारांश", "समयसीमा", "निर्देश", "घोषणा"),
    ),
    Intent.EXCITED to langs(
        listOf("excited", "exciting", "omg", "yay", "yayy", "thrilled", "pumped", "stoked", "hyped", "hype", "woah", "whoa", "eek", "wow"),
        listOf("emocionado", "emocionada", "emoción", "emocion", "ilusión", "ilusion", "ansioso", "ansiosa", "guau", "increíble", "increible"),
        listOf("excité", "excite", "excitée", "hâte", "impatient", "impatiente", "incroyable", "ouah", "waouh"),
        listOf("aufgeregt", "gespannt", "vorfreude", "unglaublich", "kribbeln"),
        listOf("animado", "animada", "empolgado", "empolgada", "empolgação", "empolgacao", "ansioso", "ansiosa", "uau", "incrível", "incrivel"),
        listOf("उत्साहित", "उत्साह", "बेसब्री", "बेताब", "वाह", "ज़बरदस्त", "जबरदस्त", "रोमांचित"),
    ),
    Intent.CELEBRATORY to langs(
        listOf("congrats", "congratulations", "anniversary", "celebrate", "celebration", "celebrating", "cheers", "woohoo", "woo", "party", "hooray", "hurrah"),
        listOf("felicidades", "felicitaciones", "enhorabuena", "aniversario", "celebrar", "celebración", "celebracion", "fiesta", "brindis"),
        listOf("félicitations", "felicitations", "bravo", "anniversaire", "fêter", "feter", "fête", "fete", "célébrer", "celebrer", "célébration", "celebration", "santé", "sante"),
        listOf("glückwunsch", "gluckwunsch", "glückwünsche", "gluckwunsche", "gratulation", "gratuliere", "jubiläum", "jubilaum", "feiern", "feier", "prost", "hurra"),
        listOf("parabéns", "parabens", "felicitações", "felicitacoes", "aniversário", "aniversario", "comemorar", "comemoração", "comemoracao", "festa", "brinde"),
        listOf("बधाई", "मुबारक", "मुबारकबाद", "जश्न", "उत्सव", "सालगिरह", "पार्टी", "वाहवाही"),
    ),
    Intent.CALM to langs(
        listOf("calm", "relax", "relaxing", "relaxed", "peace", "peaceful", "breathe", "chill", "chilling", "serene", "serenity", "quiet", "meditate", "meditation", "unwind", "mindful"),
        listOf("calma", "tranquilo", "tranquila", "tranquilidad", "relajarse", "relajado", "relajada", "paz", "respira", "respirar", "sereno", "serena", "meditar", "meditación", "meditacion"),
        listOf("calme", "tranquille", "détends", "detends", "détendu", "detendu", "paix", "respire", "respirer", "serein", "sereine", "méditer", "mediter", "méditation", "meditation", "zen"),
        listOf("ruhig", "ruhe", "gelassen", "entspann", "entspannen", "entspannt", "frieden", "atme", "atmen", "gelassenheit", "meditieren", "meditation"),
        listOf("calma", "tranquilo", "tranquila", "tranquilidade", "relaxar", "relaxado", "relaxada", "paz", "respira", "respirar", "sereno", "serena", "meditar", "meditação", "meditacao"),
        listOf("शांत", "शांति", "सुकून", "आराम", "चैन", "ध्यान", "विश्राम"),
    ),
    Intent.MOTIVATIONAL to langs(
        listOf("believe", "hustle", "grind", "focus", "focused", "discipline", "stronger", "motivation", "motivated", "motivate", "goals", "persevere", "unstoppable", "determined", "perseverance"),
        listOf("cree", "creer", "esfuerzo", "disciplina", "enfoque", "motivación", "motivacion", "motivado", "motivada", "metas", "objetivos", "imparable", "constancia", "ánimo", "animo"),
        listOf("crois", "croire", "effort", "discipline", "concentration", "motivation", "motivé", "motive", "motivée", "objectifs", "imbattable", "persévère", "persevere", "persévérance", "perseverance", "courage"),
        listOf("glaub", "glaube", "dranbleiben", "disziplin", "fokus", "stärker", "starker", "motivation", "motiviert", "ziele", "unaufhaltsam", "durchhalten"),
        listOf("acredite", "esforço", "esforco", "disciplina", "foco", "motivação", "motivacao", "motivado", "motivada", "metas", "objetivos", "imparável", "imparavel", "perseverança", "perseveranca", "força", "forca"),
        listOf("विश्वास", "मेहनत", "अनुशासन", "केंद्रित", "मज़बूत", "मजबूत", "प्रेरणा", "प्रेरित", "लक्ष्य", "हिम्मत", "हौसला"),
    ),
    Intent.GRATEFUL to langs(
        listOf("thanks", "thankful", "grateful", "gratitude", "appreciate", "appreciated", "blessed", "thx"),
        listOf("gracias", "agradecido", "agradecida", "agradezco", "gratitud", "aprecio", "bendecido", "bendecida"),
        listOf("merci", "remercie", "reconnaissant", "reconnaissante", "gratitude", "reconnaissance", "béni", "beni"),
        listOf("danke", "dank", "dankbar", "dankbarkeit", "gesegnet"),
        listOf("obrigado", "obrigada", "obrigadão", "obrigadao", "agradecido", "agradecida", "agradeço", "agradeco", "gratidão", "gratidao", "grato", "grata", "abençoado", "abencoado", "valeu"),
        listOf("धन्यवाद", "शुक्रिया", "आभार", "आभारी", "कृतज्ञ", "कृतज्ञता", "मेहरबानी"),
    ),
)

/**
 * Multi-word triggers, matched as a lowercased substring of the whole text. Phrases score
 * higher than single words since they carry more signal. Grouped by language as above.
 */
val PHRASE_KEYWORDS: Map<String, Intent> = buildKeywordMap(
    Intent.FUNNY to langs(
        listOf("so funny", "made me laugh", "can't stop laughing", "cracking up"),
        listOf("me hace reír", "me hace reir", "qué gracioso", "que gracioso", "muero de risa"),
        listOf("trop drôle", "trop drole", "j'en peux plus"),
        listOf("so lustig"),
        listOf("morrendo de rir", "que engraçado", "que engracado"),
    ),
    Intent.SAD to langs(
        listOf("i miss you", "miss you", "feeling down", "broke my heart", "so sad"),
        listOf("te extraño", "te extrano", "estoy triste", "me rompió el corazón", "me rompio el corazon"),
        listOf("tu me manques", "je suis triste", "j'ai le cœur brisé", "j'ai le coeur brise"),
        listOf("ich vermisse dich", "so traurig"),
        listOf("sinto sua falta", "estou triste", "partiu meu coração", "partiu meu coracao"),
        listOf("तुम्हारी याद आती है", "बहुत दुख हुआ"),
    ),
    Intent.ROMANTIC to langs(
        listOf("i love you", "love you", "my heart", "my love", "be mine"),
        listOf("te amo", "te quiero", "mi amor", "mi vida"),
        listOf("je t'aime", "mon amour", "mon cœur", "mon coeur"),
        listOf("ich liebe dich", "mein schatz"),
        listOf("te amo", "meu amor", "minha vida", "amo você", "amo voce"),
        listOf("मैं तुमसे प्यार करता हूँ", "मैं तुमसे प्यार करती हूँ"),
    ),
    Intent.ANGRY to langs(
        listOf("fed up", "had enough", "so mad", "this is unacceptable", "sick of"),
        listOf("estoy harto", "estoy harta", "esto es inaceptable"),
        listOf("j'en ai marre", "c'est inacceptable"),
        listOf("ich habe genug", "das ist inakzeptabel"),
        listOf("estou de saco cheio", "isso é inaceitável", "isso e inaceitavel"),
    ),
    Intent.INFORMATIVE to langs(
        listOf("please note", "heads up", "for your information", "just so you know", "action items"),
        listOf("para tu información", "para tu informacion", "toma nota"),
        listOf("pour information", "à noter", "a noter"),
        listOf("zur info", "bitte beachten"),
        listOf("para sua informação", "para sua informacao", "fica a saber"),
    ),
    Intent.EXCITED to langs(
        listOf("can't wait", "cannot wait", "so ready", "let's go", "lets go", "here we go"),
        listOf("no puedo esperar", "qué emoción", "que emocion", "vamos"),
        listOf("j'ai trop hâte", "j'ai trop hate", "on y va", "c'est parti", "c est parti"),
        listOf("ich kann es kaum erwarten", "auf geht's", "auf gehts"),
        listOf("mal posso esperar", "bora", "vamos lá", "vamos la"),
    ),
    Intent.CELEBRATORY to langs(
        listOf("happy birthday", "we did it", "way to go", "you nailed it", "job well done"),
        listOf("feliz cumpleaños", "feliz cumpleanos", "lo logramos"),
        listOf("joyeux anniversaire", "on l'a fait"),
        listOf("alles gute zum geburtstag", "wir haben es geschafft"),
        listOf("feliz aniversário", "feliz aniversario", "conseguimos"),
        listOf("जन्मदिन मुबारक", "हमने कर दिखाया"),
    ),
    Intent.CALM to langs(
        listOf("take it easy", "no rush", "deep breath", "it's okay", "all good"),
        listOf("con calma", "sin prisa", "respira hondo"),
        listOf("prends ton temps", "pas de stress", "respire un coup"),
        listOf("immer mit der ruhe", "kein stress"),
        listOf("com calma", "sem pressa", "respira fundo"),
    ),
    Intent.MOTIVATIONAL to langs(
        listOf("you got this", "keep going", "never give up", "push through", "don't quit", "dont quit", "one step at a time"),
        listOf("tú puedes", "tu puedes", "no te rindas", "sigue adelante"),
        listOf("tu peux le faire", "n'abandonne pas", "continue comme ça", "continue comme ca"),
        listOf("du schaffst das", "gib nicht auf", "mach weiter"),
        listOf("você consegue", "voce consegue", "não desista", "nao desista", "continue assim"),
        listOf("तुम कर सकते हो", "आप कर सकते हैं", "हार मत मानो"),
    ),
    Intent.GRATEFUL to langs(
        listOf("thank you", "thank you so much", "means a lot", "means the world", "i appreciate"),
        listOf("muchas gracias", "mil gracias", "te lo agradezco"),
        listOf("merci beaucoup", "je te remercie"),
        listOf("vielen dank", "danke schön", "danke schon"),
        listOf("muito obrigado", "muito obrigada"),
        listOf("बहुत धन्यवाद", "बहुत शुक्रिया"),
    ),
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
