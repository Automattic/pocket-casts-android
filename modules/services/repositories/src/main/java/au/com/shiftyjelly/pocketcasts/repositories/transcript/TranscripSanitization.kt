package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry

internal fun List<TranscriptEntry>.sanitize() = map(TranscriptEntry::compactWhiteSpace)
    .joinSplitSentences()
    .joinConsecutiveSpeakers()
    .removeRepeatedSpeakers()
    .map(TranscriptEntry::trim)
    .filter(TranscriptEntry::isNotEmpty)
    .map { if (it is TranscriptEntry.Text) it.recalculateWordOffsets() else it }

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
    val wordTimings = mutableListOf<TranscriptEntry.WordTiming>()
    val entries = mutableListOf<TranscriptEntry>()
    var accumulatedStartTimeMs = -1L
    var accumulatedEndTimeMs = -1L

    fun appendToAccumulator(text: String, startTimeMs: Long, endTimeMs: Long) {
        val trimmedText = text.trim()
        phraseAccumulator.append(' ').append(trimmedText)
        if (accumulatedStartTimeMs == -1L || (startTimeMs in 0..<accumulatedStartTimeMs)) {
            accumulatedStartTimeMs = startTimeMs
        }
        if (endTimeMs > accumulatedEndTimeMs) {
            accumulatedEndTimeMs = endTimeMs
        }
        if (trimmedText.isNotEmpty()) {
            wordTimings.add(
                TranscriptEntry.WordTiming(
                    text = trimmedText,
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs,
                    charOffsetStart = 0,
                    charOffsetEnd = 0,
                ),
            )
        }
    }

    fun buildFullSentence(text: String, startTimeMs: Long, endTimeMs: Long): TranscriptEntry {
        appendToAccumulator(text, startTimeMs, endTimeMs)
        val sentences = phraseAccumulator.toString()
        phraseAccumulator.clear()
        val resultStartTimeMs = accumulatedStartTimeMs
        val resultEndTimeMs = accumulatedEndTimeMs
        accumulatedStartTimeMs = -1L
        accumulatedEndTimeMs = -1L
        val resultWords = if (wordTimings.size > 1) wordTimings.toList() else emptyList()
        wordTimings.clear()
        return TranscriptEntry.Text(
            sentences,
            startTimeMs = resultStartTimeMs,
            endTimeMs = resultEndTimeMs,
            words = resultWords,
        )
    }

    fun buildMidSentence(text: String, startTimeMs: Long, endTimeMs: Long): TranscriptEntry? {
        val midSentence = text.findMidSentence()

        return if (midSentence != null) {
            val (index, punctuation) = midSentence

            val midSentenceText = text.substring(0, index + punctuation.length)
            val leftOverText = text.drop(midSentenceText.length)

            // Split the time range at the sentence boundary so the two halves don't overlap.
            val splitTimeMs = splitTimeMs(startTimeMs, endTimeMs, midSentenceText.length, text.length)
            val sentence = buildFullSentence(midSentenceText, startTimeMs, splitTimeMs)

            appendToAccumulator(leftOverText, splitTimeMs, endTimeMs)

            sentence
        } else {
            appendToAccumulator(text, startTimeMs, endTimeMs)
            null
        }
    }

    mapNotNullTo(entries) { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> entry

            is TranscriptEntry.Text -> {
                val text = entry.value
                if (text.endsAsSentence()) {
                    buildFullSentence(text, entry.startTimeMs, entry.endTimeMs)
                } else {
                    buildMidSentence(text, entry.startTimeMs, entry.endTimeMs)
                }
            }
        }
    }
    if (phraseAccumulator.isNotEmpty()) {
        entries += TranscriptEntry.Text(
            phraseAccumulator.toString(),
            startTimeMs = accumulatedStartTimeMs,
            endTimeMs = accumulatedEndTimeMs,
            words = if (wordTimings.size > 1) wordTimings.toList() else emptyList(),
        )
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

// Splits a cue's time range proportionally at a character position, falling back to [endTimeMs]
// for untimed/degenerate cues.
private fun splitTimeMs(startTimeMs: Long, endTimeMs: Long, splitIndex: Int, totalLength: Int): Long {
    if (startTimeMs < 0 || endTimeMs <= startTimeMs || totalLength <= 0) return endTimeMs
    val offset = (endTimeMs - startTimeMs) * splitIndex / totalLength
    return (startTimeMs + offset).coerceIn(startTimeMs, endTimeMs)
}

private fun TranscriptEntry.Text.recalculateWordOffsets(): TranscriptEntry.Text {
    if (words.isEmpty()) return this
    var searchFrom = 0
    val updated = words.mapNotNull { word ->
        val start = value.indexOf(word.text, searchFrom)
        if (start >= 0) {
            searchFrom = start + word.text.length
            word.copy(charOffsetStart = start, charOffsetEnd = start + word.text.length)
        } else {
            null
        }
    }
    return copy(words = updated)
}
