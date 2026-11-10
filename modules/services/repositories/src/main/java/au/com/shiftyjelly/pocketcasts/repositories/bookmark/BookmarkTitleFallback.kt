package au.com.shiftyjelly.pocketcasts.repositories.bookmark

object BookmarkTitleFallback {
    private const val WORD_COUNT = 6
    private const val MAX_LENGTH = 100
    private val WORD_SEPARATOR = Regex("\\s+")
    private val TRAILING_SEPARATORS = setOf(',', ';', ':', '—', '–', '-')

    fun fromPassage(passage: String?): String? {
        val words = passage
            ?.split(WORD_SEPARATOR)
            ?.filter { word -> word.any { it.isLetterOrDigit() } }
            ?.take(WORD_COUNT)
            ?: return null
        return capLength(words.joinToString(" "))
            .trimEnd { it.isWhitespace() || it in TRAILING_SEPARATORS }
            .takeIf { it.isNotEmpty() }
    }

    private fun capLength(title: String): String {
        if (title.length <= MAX_LENGTH) return title
        val lastSpace = title.lastIndexOf(' ', startIndex = MAX_LENGTH)
        if (lastSpace > 0) return title.substring(0, lastSpace)
        val end = if (title[MAX_LENGTH - 1].isHighSurrogate()) MAX_LENGTH - 1 else MAX_LENGTH
        return title.substring(0, end)
    }
}
