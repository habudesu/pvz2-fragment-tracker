package com.pvz2tool.domain

/**
 * 汉字拼音首字母表（打包在 assets/pinyin_initials.bin）。
 * 只覆盖 U+4E00–U+9FFF 区段，一个汉字一个字节，0 表示没有读音数据。
 * 这样查表即可离线得到首字母，不需要引入第三方拼音库。
 */
class PinyinInitials(private val table: ByteArray) {

    /**
     * 取首字母串：汉字取拼音首字母，字母数字原样（转小写），
     * 标点、空格等全部跳过，所以「阿.开.木.木.」得到 "akmm"。
     * 多音字按词组优先取音，比如「弹簧」按 tán 取 "th" 而不是 d。
     */
    fun of(text: String): String = buildString {
        val lower = text.lowercase()
        var i = 0
        while (i < lower.length) {
            val phrase = PHRASE_OVERRIDES.firstOrNull { lower.startsWith(it.first, i) }
            if (phrase != null) {
                append(phrase.second)
                i += phrase.first.length
                continue
            }
            val ch = lower[i]
            when {
                isAsciiLetterOrDigit(ch) -> append(ch)
                isHan(ch) -> {
                    val letter = table.getOrNull(ch.code - FIRST_HAN)
                    if (letter != null && letter.toInt() != 0) append(letter.toInt().toChar())
                }
            }
            i++
        }
    }

    /** 去掉标点后的检索文本：汉字、字母、数字保留，其余忽略。 */
    fun normalize(text: String): String = buildString {
        text.lowercase().forEach { ch ->
            if (isAsciiLetterOrDigit(ch) || isHan(ch)) append(ch)
        }
    }

    private fun isAsciiLetterOrDigit(ch: Char): Boolean =
        ch in 'a'..'z' || ch in '0'..'9'

    private fun isHan(ch: Char): Boolean = ch.code in FIRST_HAN..LAST_HAN

    private companion object {
        const val FIRST_HAN = 0x4E00
        const val LAST_HAN = 0x9FFF

        /**
         * 多音字按词定音。查表只能给出最常见读音，这里按词组覆盖；
         * 「藤蔓」按 téng màn 取 m，与表里默认读音一致，不需要覆盖。
         */
        val PHRASE_OVERRIDES: List<Pair<String, String>> = listOf(
            "弹簧" to "th",
            "番茄" to "fq",
            "茄子" to "qz",
            "双重" to "sc",
            "三重" to "sc",
            "桑葚" to "ss",
            "荸荠" to "bq",
            "粘液" to "ny",
            "长枪" to "cq",
            "杓兰" to "sl"
        ).sortedByDescending { it.first.length }
    }
}
