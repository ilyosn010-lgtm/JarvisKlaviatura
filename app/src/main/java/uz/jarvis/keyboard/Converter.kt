package uz.jarvis.keyboard

/**
 * Lotin (eski: oʻ gʻ sh ch), lotin (yangi: ö ğ ş ç) va kirill o'rtasida o'girish.
 * Qoidalar taxminiy: oddiy matn uchun mo'ljallangan.
 */
object Converter {
    const val TO_NEW = 0
    const val TO_OLD = 1
    const val TO_CYR = 2

    private const val APOS_CHARS = "‘ʻ'’`´ʼ′"
    private const val TURNED = 'ʻ' // oʻ, gʻ
    private const val TUTUQ = 'ʼ'  // tutuq belgisi
    private const val CYR_VOWELS = "аеёиоуўэюя"

    fun apply(mode: Int, s: String): String = when (mode) {
        TO_NEW -> toNew(s)
        TO_OLD -> toOld(s)
        else -> toCyr(s)
    }

    /** Harfdan keyingi apostrof: o/g dan keyin -> ʻ, boshqa harfdan keyin -> ʼ (tutuq). */
    private fun normApos(s: String): String {
        val sb = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            if (APOS_CHARS.indexOf(c) >= 0 && i > 0 && s[i - 1].isLetter()) {
                val p = s[i - 1].lowercaseChar()
                sb.append(if (p == 'o' || p == 'g') TURNED else TUTUQ)
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun neighborUpper(s: String, i: Int): Boolean =
        (i > 0 && s[i - 1].isUpperCase()) || (i + 1 < s.length && s[i + 1].isUpperCase())

    /** Yangi harflar (ö ğ ş ç) -> eski (oʻ gʻ sh ch). */
    fun toOld(src: String): String {
        val sb = StringBuilder(src.length + 8)
        for (i in src.indices) {
            val c = src[i]
            when (c) {
                'ö' -> sb.append('o').append(TURNED)
                'Ö' -> sb.append('O').append(TURNED)
                'ğ' -> sb.append('g').append(TURNED)
                'Ğ' -> sb.append('G').append(TURNED)
                'ş' -> sb.append("sh")
                'Ş' -> sb.append(if (neighborUpper(src, i)) "SH" else "Sh")
                'ç' -> sb.append("ch")
                'Ç' -> sb.append(if (neighborUpper(src, i)) "CH" else "Ch")
                else -> sb.append(c)
            }
        }
        return normApos(sb.toString())
    }

    /** Eski harflar (oʻ gʻ sh ch) -> yangi (ö ğ ş ç). */
    fun toNew(src: String): String {
        val s = normApos(src)
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val n = if (i + 1 < s.length) s[i + 1] else '\u0000'
            val lc = c.lowercaseChar()
            val ln = n.lowercaseChar()
            val up = c.isUpperCase()
            if ((lc == 'o' || lc == 'g') && n == TURNED) {
                val r = if (lc == 'o') 'ö' else 'ğ'
                sb.append(if (up) r.uppercaseChar() else r)
                i += 2
                continue
            }
            if ((lc == 's' || lc == 'c') && ln == 'h') {
                val r = if (lc == 's') 'ş' else 'ç'
                sb.append(if (up) r.uppercaseChar() else r)
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Lotin (eski yoki yangi) -> kirill. */
    fun toCyr(src: String): String {
        val t = toOld(src)
        val sb = StringBuilder(t.length)
        var i = 0
        while (i < t.length) {
            val c = t[i]
            val n = if (i + 1 < t.length) t[i + 1] else '\u0000'
            val lc = c.lowercaseChar()
            val ln = n.lowercaseChar()
            val up = c.isUpperCase()
            var out: String? = null
            var used = 1
            if (lc == 's' && ln == 'h') {
                out = "ш"; used = 2
            } else if (lc == 'c' && ln == 'h') {
                out = "ч"; used = 2
            } else if (lc == 'o' && n == TURNED) {
                out = "ў"; used = 2
            } else if (lc == 'g' && n == TURNED) {
                out = "ғ"; used = 2
            } else if (lc == 'y' && ln == 'o') {
                out = "ё"; used = 2
            } else if (lc == 'y' && ln == 'u') {
                out = "ю"; used = 2
            } else if (lc == 'y' && ln == 'a') {
                out = "я"; used = 2
            } else if (lc == 'y' && ln == 'e') {
                out = "е"; used = 2
            } else {
                out = when (lc) {
                    'a' -> "а"
                    'b' -> "б"
                    'c' -> "ц"
                    'd' -> "д"
                    'e' -> {
                        val prev = if (sb.isNotEmpty()) sb[sb.length - 1].lowercaseChar() else ' '
                        if (!prev.isLetter() || CYR_VOWELS.indexOf(prev) >= 0) "э" else "е"
                    }
                    'f' -> "ф"
                    'g' -> "г"
                    'h' -> "ҳ"
                    'i' -> "и"
                    'j' -> "ж"
                    'k' -> "к"
                    'l' -> "л"
                    'm' -> "м"
                    'n' -> "н"
                    'o' -> "о"
                    'p' -> "п"
                    'q' -> "қ"
                    'r' -> "р"
                    's' -> "с"
                    't' -> "т"
                    'u' -> "у"
                    'v' -> "в"
                    'w' -> "в"
                    'x' -> "х"
                    'y' -> "й"
                    'z' -> "з"
                    TUTUQ -> "ъ"
                    TURNED -> "ъ"
                    else -> null
                }
            }
            if (out != null) {
                sb.append(if (up) out.uppercase() else out)
            } else {
                sb.append(c)
            }
            i += used
        }
        return sb.toString()
    }
}
