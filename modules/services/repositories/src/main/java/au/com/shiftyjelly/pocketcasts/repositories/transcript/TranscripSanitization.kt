package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry

internal fun List<TranscriptEntry>.sanitize() = map(TranscriptEntry::compactWhiteSpace)
    .joinSplitSentences()
    .joinConsecutiveSpeakers()
    .removeRepeatedSpeakers()
    .map(TranscriptEntry::trim)
    .filter(TranscriptEntry::isNotEmpty)

private fun TranscriptEntry.compactWhiteSpace() = when (this) {
    is TranscriptEntry.Speaker -> {
        val newName = name.replace(AnyWhiteSpace, " ")
        copy(name = newName)
    }

    is TranscriptEntry.Text -> {
        val newValue = value
            .replace(TwoOrMoreEmptySpaces, " ")
            .replace(ThreeOrMoreNewLines, "\n\n")
        copy(value = newValue)
    }
}

private val AnyWhiteSpace = """\s+""".toRegex()
private val TwoOrMoreEmptySpaces = """[ \t]{2,}""".toRegex()
private val ThreeOrMoreNewLines = """\n{3,}""".toRegex()

private fun List<TranscriptEntry>.joinSplitSentences(): List<TranscriptEntry> {
    val phraseAccumulator = StringBuilder()
    val entries = mutableListOf<TranscriptEntry>()

    fun appendToAccumulator(text: String) {
        phraseAccumulator.append(' ').append(text.trimStart())
    }

    fun buildFullSentence(text: String): TranscriptEntry {
        appendToAccumulator(text)
        val sentences = phraseAccumulator.toString()
        phraseAccumulator.clear()
        return TranscriptEntry.Text(sentences)
    }

    fun buildMidSentence(text: String): TranscriptEntry? {
        val midSentence = text.findMidSentence()

        return if (midSentence != null) {
            val (index, punctuation) = midSentence

            val midSentenceText = text.substring(0, index + punctuation.length)
            val sentence = buildFullSentence(midSentenceText)

            val leftOverText = text.drop(midSentenceText.length)
            appendToAccumulator(leftOverText)

            sentence
        } else {
            appendToAccumulator(text)
            null
        }
    }

    mapNotNullTo(entries) { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> entry
            is TranscriptEntry.Text -> {
                val text = entry.value
                if (text.endsAsSentence()) {
                    buildFullSentence(text)
                } else {
                    buildMidSentence(text)
                }
            }
        }
    }
    if (phraseAccumulator.isNotEmpty()) {
        entries += TranscriptEntry.Text(phraseAccumulator.toString())
    }
    return entries
}

private fun List<TranscriptEntry>.joinConsecutiveSpeakers(): List<TranscriptEntry> {
    val namesAccumulator = mutableSetOf<String>()
    val entries = mutableListOf<TranscriptEntry>()
    flatMapTo(entries) { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> {
                val names = entry.name.takeIf(String::isNotEmpty)?.split(", ")
                if (names != null) {
                    namesAccumulator += names
                }
                emptyList<TranscriptEntry>()
            }

            is TranscriptEntry.Text -> {
                buildList {
                    if (namesAccumulator.isNotEmpty()) {
                        val names = namesAccumulator.sorted().joinToString(separator = ", ")
                        namesAccumulator.clear()
                        add(TranscriptEntry.Speaker(names))
                    }
                    add(entry)
                }
            }
        }
    }
    if (namesAccumulator.isNotEmpty()) {
        val names = namesAccumulator.sorted().joinToString(separator = ", ")
        entries += TranscriptEntry.Speaker(names)
    }
    return entries
}

private fun List<TranscriptEntry>.removeRepeatedSpeakers(): List<TranscriptEntry> {
    var lastSpeaker: String? = null
    return mapNotNull { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> {
                if (lastSpeaker != entry.name) {
                    lastSpeaker = entry.name
                    entry
                } else {
                    null
                }
            }

            is TranscriptEntry.Text -> entry
        }
    }
}

private fun TranscriptEntry.trim() = when (this) {
    is TranscriptEntry.Speaker -> copy(name = name.trim())
    is TranscriptEntry.Text -> copy(value = value.trim())
}

private fun TranscriptEntry.isNotEmpty() = when (this) {
    is TranscriptEntry.Speaker -> name.isNotEmpty()
    is TranscriptEntry.Text -> value.isNotEmpty()
}

fun String.endsAsSentence(): Boolean {
    return EndOfSentencePunctuation.any { punctuation -> endsWith(punctuation) }
}

private val EndOfSentencePunctuation = listOf(
    // Sentences
    ".", "!", "?", "…",
    // Interrupted sentences
    "-",
    // Brackets
    ")", "]", ">", "}",
    // Quotation marks
    "\"", "”", "'", "’",
)

private fun String.findMidSentence(): Pair<Int, String>? {
    // We first search for punctuation followed by quotation marks (e.g., '."') before looking for standalone punctuation.
    //
    // This prevents cases where findLastAnyOf() would match only the punctuation character,
    // causing quotation marks to be incorrectly moved to the next line.
    //
    // For example, in: `This is "sentence." And another one`,
    // matching '.' would split the text before the closing quote, producing: `"And another one`.
    return findLastAnyOf(MidSentenceQuotationPunctuation) ?: findLastAnyOf(MidSentencePunctuation)
}

private val MidSentencePunctuation = listOf(
    ".",
    "!",
    "?",
    "…",
)

private val MidSentenceQuotationPunctuation = MidSentencePunctuation.flatMap { punctuation ->
    val quotationMarks = listOf("\"", "”", "'", "’")
    quotationMarks.map { quotationMark -> "$punctuation$quotationMark" }
}
