package com.example.digitscan.domain

/**
 * Вся бизнес-логика обработки текста с фотографий. Не зависит от Android — легко тестируется.
 */
object TextProcessing {
    /** Максимальная длина текста на одной фотографии (по ТЗ). */
    const val MAX_LEN = 20

    /** Чистит результат распознавания / ручного ввода: убирает пробелы и переносы, унифицирует «/». */
    fun normalize(raw: String): String = buildString {
        for (c in raw) {
            when {
                c.isWhitespace() -> Unit
                c == '／' || c == '⁄' || c == '∕' -> append('/')
                else -> append(c)
            }
        }
    }

    fun isValid(text: String?): Boolean =
        text != null && text.isNotEmpty() && text.length <= MAX_LEN

    /** Каждый символ «/» заменяется на «00». */
    fun replaceSlashes(text: String): String = text.replace("/", "00")

    /**
     * Сортировка по возрастанию — строковая (посимвольная): "10" идёт раньше "2", цифры раньше букв.
     * Текст нигде не приводится к числу.
     */
    val ascending: Comparator<String> = naturalOrder()

    /** Итог сессии: замена «/» → «00», затем сортировка по возрастанию. */
    fun buildResult(texts: List<String>): List<String> =
        texts.map(::replaceSlashes).sortedWith(ascending)
}
