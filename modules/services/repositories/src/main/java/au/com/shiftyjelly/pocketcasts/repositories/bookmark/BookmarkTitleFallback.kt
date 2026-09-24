package au.com.shiftyjelly.pocketcasts.repositories.bookmark

object BookmarkTitleFallback {
    private const val WORD_COUNT = 6
    private const val MAX_LENGTH = 100
    private val WORD_SEPARATOR = Regex("\\s+")
    private val TRAILING_PUNCTUATION = charArrayOf(',', ';', ':', '—')

    fun fromPassage(passage: String?): String? {
        return passage
            ?.split(WORD_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.take(WORD_COUNT)
            ?.joinToString(" ")
            ?.trimEnd(*TRAILING_PUNCTUATION)
            ?.trim()
            ?.take(MAX_LENGTH)
            ?.takeIf { it.isNotEmpty() }
    }
}
