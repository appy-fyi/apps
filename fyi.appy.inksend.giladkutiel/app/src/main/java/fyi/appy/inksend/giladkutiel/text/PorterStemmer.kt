package fyi.appy.inksend.giladkutiel.text

/**
 * The classic Porter (1980) stemmer, ported from Martin Porter's own reference Java
 * implementation (https://tartarus.org/martin/PorterStemmer/). Pure JVM — no Android
 * dependencies — so it is unit-testable directly.
 *
 * [stem] lower-cases its input and drops every non a–z character itself, so a raw token can
 * be passed straight in. A single instance keeps mutable buffers between calls and is **not**
 * thread-safe; [fyi.appy.inksend.giladkutiel.text.TextTokens] confines one per thread.
 *
 * The whole point of stemming here: the mood dictionaries in
 * [fyi.appy.inksend.giladkutiel.data.model.AutoStyle] and the keyword→emoji table in
 * [fyi.appy.inksend.giladkutiel.data.model.EmojiLexicon] are keyed by the stemmer's own
 * output (their human-readable keywords are run through this same class at construction), so
 * "laughing", "laughed" and "laughs" all collapse onto the one key "laugh" that "laugh"
 * itself produces.
 */
class PorterStemmer {

    private var b = CharArray(64)
    private var iEnd = 0 // count of live chars in b
    private var j = 0
    private var k = 0

    private fun push(ch: Char) {
        if (iEnd == b.size) b = b.copyOf(b.size * 2)
        b[iEnd++] = ch
    }

    fun stem(word: String): String {
        iEnd = 0
        for (c in word) {
            val lc = c.lowercaseChar()
            if (lc in 'a'..'z') push(lc)
        }
        if (iEnd <= 2) return String(b, 0, iEnd)
        k = iEnd - 1
        step1ab()
        step1c()
        step2()
        step3()
        step4()
        step5()
        return String(b, 0, k + 1)
    }

    /** True when b[i] is a consonant. 'y' is a consonant only when preceded by a vowel. */
    private fun cons(i: Int): Boolean = when (b[i]) {
        'a', 'e', 'i', 'o', 'u' -> false
        'y' -> if (i == 0) true else !cons(i - 1)
        else -> true
    }

    /** The measure: the number of vowel-consonant sequences in b[0..j]. */
    private fun measure(): Int {
        var n = 0
        var i = 0
        while (true) {
            if (i > j) return n
            if (!cons(i)) break
            i++
        }
        i++
        while (true) {
            while (true) {
                if (i > j) return n
                if (cons(i)) break
                i++
            }
            i++
            n++
            while (true) {
                if (i > j) return n
                if (!cons(i)) break
                i++
            }
            i++
        }
    }

    private fun vowelInStem(): Boolean {
        for (i in 0..j) if (!cons(i)) return true
        return false
    }

    private fun doublec(i: Int): Boolean = i >= 1 && b[i] == b[i - 1] && cons(i)

    /**
     * True when b[i-2..i] is consonant-vowel-consonant and the final consonant is not w, x or
     * y — the shape a short word ends in, used to decide whether to restore a trailing 'e'.
     */
    private fun cvc(i: Int): Boolean {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false
        val ch = b[i]
        return ch != 'w' && ch != 'x' && ch != 'y'
    }

    private fun ends(s: String): Boolean {
        val len = s.length
        val off = k - len + 1
        if (off < 0) return false
        for (x in 0 until len) if (b[off + x] != s[x]) return false
        j = k - len
        return true
    }

    /** Replaces b[j+1..k] with [s]. */
    private fun setTo(s: String) {
        for (x in s.indices) b[j + 1 + x] = s[x]
        k = j + s.length
    }

    private fun replace(s: String) {
        if (measure() > 0) setTo(s)
    }

    /** Strips plurals and -ed / -ing, tidying the stump (step 1a + 1b). */
    private fun step1ab() {
        if (b[k] == 's') {
            when {
                ends("sses") -> k -= 2
                ends("ies") -> setTo("i")
                b[k - 1] != 's' -> k--
            }
        }
        if (ends("eed")) {
            if (measure() > 0) k--
        } else if ((ends("ed") || ends("ing")) && vowelInStem()) {
            k = j
            when {
                ends("at") -> setTo("ate")
                ends("bl") -> setTo("ble")
                ends("iz") -> setTo("ize")
                doublec(k) -> {
                    val ch = b[k]
                    if (ch != 'l' && ch != 's' && ch != 'z') k--
                }
                measure() == 1 && cvc(k) -> setTo("e")
            }
        }
    }

    /** Turns a terminal 'y' into 'i' when there is another vowel in the stem (step 1c). */
    private fun step1c() {
        if (ends("y") && vowelInStem()) b[k] = 'i'
    }

    /** Maps double suffixes to single ones (step 2). */
    private fun step2() {
        if (k == 0) return
        when (b[k - 1]) {
            'a' -> when {
                ends("ational") -> replace("ate")
                ends("tional") -> replace("tion")
            }
            'c' -> when {
                ends("enci") -> replace("ence")
                ends("anci") -> replace("ance")
            }
            'e' -> if (ends("izer")) replace("ize")
            'l' -> when {
                ends("bli") -> replace("ble")
                ends("alli") -> replace("al")
                ends("entli") -> replace("ent")
                ends("eli") -> replace("e")
                ends("ousli") -> replace("ous")
            }
            'o' -> when {
                ends("ization") -> replace("ize")
                ends("ation") -> replace("ate")
                ends("ator") -> replace("ate")
            }
            's' -> when {
                ends("alism") -> replace("al")
                ends("iveness") -> replace("ive")
                ends("fulness") -> replace("ful")
                ends("ousness") -> replace("ous")
            }
            't' -> when {
                ends("aliti") -> replace("al")
                ends("iviti") -> replace("ive")
                ends("biliti") -> replace("ble")
            }
            'g' -> if (ends("logi")) replace("log")
        }
    }

    /** Deals with -ic-, -full, -ness etc. (step 3). */
    private fun step3() {
        when (b[k]) {
            'e' -> when {
                ends("icate") -> replace("ic")
                ends("ative") -> replace("")
                ends("alize") -> replace("al")
            }
            'i' -> if (ends("iciti")) replace("ic")
            'l' -> when {
                ends("ical") -> replace("ic")
                ends("ful") -> replace("")
            }
            's' -> if (ends("ness")) replace("")
        }
    }

    /** Takes off -ant, -ence etc. in context <c>vcvc<v> (step 4). */
    private fun step4() {
        if (k == 0) return
        val matched = when (b[k - 1]) {
            'a' -> ends("al")
            'c' -> ends("ance") || ends("ence")
            'e' -> ends("er")
            'i' -> ends("ic")
            'l' -> ends("able") || ends("ible")
            'n' -> ends("ant") || ends("ement") || ends("ment") || ends("ent")
            'o' -> (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) || ends("ou")
            's' -> ends("ism")
            't' -> ends("ate") || ends("iti")
            'u' -> ends("ous")
            'v' -> ends("ive")
            'z' -> ends("ize")
            else -> false
        }
        if (matched && measure() > 1) k = j
    }

    /** Removes a final -e and reduces a final -ll (step 5). */
    private fun step5() {
        j = k
        if (b[k] == 'e') {
            val a = measure()
            if (a > 1 || (a == 1 && !cvc(k - 1))) k--
        }
        if (b[k] == 'l' && doublec(k) && measure() > 1) k--
    }
}
