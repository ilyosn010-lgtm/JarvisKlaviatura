package uz.jarvis.keyboard

enum class KType {
    CHAR, DIGIT, SHIFT, DEL, ENTER, SPACE, LANG, SYM, ABC, EMOJI, EMOJI_CHAR,
    SETTINGS, CONV_NEW, CONV_OLD, CONV_CYR, PASTE
}

enum class KStyle { NORMAL, DIGIT, SPECIAL, TEAL, NEWLETTER, YELLOW, CHIP }

enum class Lang(val title: String) {
    LOTIN("lotin"), YANGI("yangi"), KIRILL("кирилл")
}

class Key(
    val label: String,
    val type: KType,
    val text: String = label,
    val weight: Float = 1f,
    val style: KStyle = KStyle.NORMAL
)

class Row(val keys: List<Key>, val heightDp: Float)

object Layouts {
    private const val T = 'ʻ'

    private fun ch(label: String, text: String = label.lowercase(), style: KStyle = KStyle.NORMAL): Key =
        Key(label, KType.CHAR, text, 1f, style)

    private fun lt(chars: String): List<Key> = chars.map { ch(it.toString().uppercase()) }

    private fun shiftKey() = Key("⇧", KType.SHIFT, "", 1f, KStyle.TEAL)
    private fun delKey() = Key("⌫", KType.DEL, "", 1f, KStyle.SPECIAL)

    private fun digits() = Row(
        "1234567890".map { Key(it.toString(), KType.DIGIT, it.toString(), 1f, KStyle.DIGIT) },
        46f
    )

    private fun strip() = Row(
        listOf(
            Key("→ ÖŞ yangi", KType.CONV_NEW, "", 3f, KStyle.SPECIAL),
            Key("→ O${T}Sh eski", KType.CONV_OLD, "", 3f, KStyle.SPECIAL),
            Key("→ ЎШ kirill", KType.CONV_CYR, "", 3f, KStyle.SPECIAL),
            Key("", KType.SETTINGS, "", 1f, KStyle.SPECIAL)
        ),
        44f
    )

    private fun clipRow(clip: String): Row {
        val one = clip.replace('\n', ' ').trim()
        val pv = if (one.length > 34) one.take(34) + "…" else one
        return Row(listOf(Key("Joylash:  $pv", KType.PASTE, clip, 1f, KStyle.CHIP)), 38f)
    }

    private fun bottom(lang: Lang): Row {
        val name = if (lang == Lang.KIRILL) "Ўзбек · кирилл" else "Oʻzbek · ${lang.title}"
        return Row(
            listOf(
                Key("?123", KType.SYM, "", 1.3f, KStyle.SPECIAL),
                Key("", KType.LANG, "", 1f, KStyle.SPECIAL),
                Key("", KType.EMOJI, "", 1.1f, KStyle.YELLOW),
                Key(name, KType.SPACE, " ", 4.2f, KStyle.NORMAL),
                Key(".", KType.CHAR, ".", 0.9f, KStyle.NORMAL),
                Key("", KType.ENTER, "\n", 1.3f, KStyle.TEAL)
            ),
            52f
        )
    }

    fun letters(lang: Lang, clip: String?, secure: Boolean): List<Row> {
        val rows = ArrayList<Row>()
        if (!secure) rows.add(strip())
        if (!secure && clip != null) rows.add(clipRow(clip))
        rows.add(digits())
        when (lang) {
            Lang.LOTIN -> {
                rows.add(Row(lt("qertyuiop") + ch("G$T", "g$T"), 52f))
                rows.add(Row(lt("asdfghjkl") + ch("SH", "sh"), 52f))
                rows.add(
                    Row(
                        listOf(shiftKey()) + lt("zxvbnm") + ch("O$T", "o$T") + ch("CH", "ch") + delKey(),
                        52f
                    )
                )
            }
            Lang.YANGI -> {
                rows.add(Row(lt("qertyuiop") + ch("Ğ", "ğ", KStyle.NEWLETTER), 52f))
                rows.add(Row(lt("asdfghjkl") + ch("Ş", "ş", KStyle.NEWLETTER), 52f))
                rows.add(
                    Row(
                        listOf(shiftKey()) + lt("zxvbnm") + ch("Ö", "ö", KStyle.NEWLETTER) +
                            ch("Ç", "ç", KStyle.NEWLETTER) + delKey(),
                        52f
                    )
                )
            }
            Lang.KIRILL -> {
                rows.add(Row(lt("йцукенгшўзхъ"), 52f))
                rows.add(Row(lt("фқвапролджэғ"), 52f))
                rows.add(Row(listOf(shiftKey()) + lt("ячсмитьбюҳ") + delKey(), 52f))
            }
        }
        rows.add(bottom(lang))
        return rows
    }

    private fun sk(s: String) = Key(s, KType.CHAR, s, 1f, KStyle.NORMAL)

    fun symbols(): List<Row> = listOf(
        digits(),
        Row(listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")").map { sk(it) }, 52f),
        Row(listOf("!", "\"", "'", "ʼ", "?", ",", ".", ":", ";", "/").map { sk(it) }, 52f),
        Row(listOf("=", "_", "~", "<", ">", "[", "]", "{", "}").map { sk(it) } + delKey(), 52f),
        Row(
            listOf(
                Key("ABC", KType.ABC, "", 1.5f, KStyle.SPECIAL),
                Key("Bo'sh joy", KType.SPACE, " ", 6f, KStyle.NORMAL),
                Key("", KType.ENTER, "\n", 1.5f, KStyle.TEAL)
            ),
            52f
        )
    )

    private fun em(s: String) = Key(s, KType.EMOJI_CHAR, s, 1f, KStyle.NORMAL)

    fun emoji(): List<Row> = listOf(
        Row(listOf("😊", "😂", "😍", "😭", "😘", "🥰", "😎", "🤔").map { em(it) }, 52f),
        Row(listOf("👍", "🙏", "👏", "💪", "🔥", "✨", "🎉", "❤️").map { em(it) }, 52f),
        Row(listOf("😁", "😢", "😡", "😴", "🤝", "🌹", "☀️", "🇺🇿").map { em(it) }, 52f),
        Row(
            listOf(
                Key("ABC", KType.ABC, "", 1.5f, KStyle.SPECIAL),
                Key("Bo'sh joy", KType.SPACE, " ", 5f, KStyle.NORMAL),
                delKey()
            ),
            52f
        )
    )

    /** Kod / telefon / raqam maydonlari uchun: faqat raqamlar. */
    fun numeric(): List<Row> {
        fun d(s: String) = Key(s, KType.DIGIT, s, 1f, KStyle.DIGIT)
        return listOf(
            Row(listOf(d("1"), d("2"), d("3")), 64f),
            Row(listOf(d("4"), d("5"), d("6")), 64f),
            Row(listOf(d("7"), d("8"), d("9")), 64f),
            Row(listOf(delKey(), d("0"), Key("", KType.ENTER, "\n", 1f, KStyle.TEAL)), 64f)
        )
    }
}
