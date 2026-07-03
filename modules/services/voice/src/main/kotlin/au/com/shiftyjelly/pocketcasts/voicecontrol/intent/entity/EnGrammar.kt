package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.entity

import javax.inject.Inject
import javax.inject.Singleton

/**
 * English grammar for extracting duration, number, ordinal, trim mode, and boolean
 * entities from voice command transcripts.
 *
 * Patterns are ordered by specificity — longer/more specific matches are attempted
 * before shorter/generic ones to avoid partial matches.
 */
@Singleton
class EnGrammar @Inject constructor() : LanguageGrammar {
    override val languageCode = "en"

    override fun canParse(text: String): Boolean {
        // English text is primarily ASCII. If the text contains CJK characters
        // or is predominantly non-ASCII, prefer another grammar.
        val latinCount = text.count { it in 'a'..'z' || it in 'A'..'Z' || it == ' ' }
        return latinCount.toDouble() / text.length.coerceAtLeast(1) > 0.5
    }

    // -- Duration ----------------------------------------------------------

    // Word → multiplier in seconds
    private val unitMultipliers = mapOf(
        "hour" to 3600,
        "hours" to 3600,
        "minute" to 60,
        "minutes" to 60,
        "min" to 60,
        "second" to 1,
        "seconds" to 1,
        "sec" to 1,
    )

    // Word → numeric value (cardinal)
    private val wordNumbers = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12,
        "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16,
        "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    private val numericParts = mapOf(
        "half" to 0.5,
        "quarter" to 0.25,
    )

    override fun extractDuration(text: String): List<Int> {
        val results = mutableListOf<Int>()
        val lower = text.lowercase().trim()

        // "X and a half Y" pattern: "2 and a half minutes", "one and a half hours"
        val andHalfRegex = Regex(
            """(\d+(?:\.\d+)?|[a-z]+)\s*and\s*a?\s*half\s*(hour|minute|second|min|sec)s?""",
        )
        andHalfRegex.find(lower)?.let { match ->
            val base = parseNumber(match.groupValues[1])
            val unit = match.groupValues[2]
            val multiplier = unitMultipliers[unit] ?: unitMultipliers["${unit}s"] ?: 1
            if (base != null) {
                results.add(((base + 0.5) * multiplier).toInt())
            }
        }

        // "half a Y" pattern: "half a minute", "half an hour"
        val halfARegex = Regex("""half\s+an?\s+(hour|minute|second|min|sec)s?""")
        halfARegex.find(lower)?.let { match ->
            val unit = match.groupValues[1]
            val multiplier = unitMultipliers[unit] ?: unitMultipliers["${unit}s"] ?: 1
            results.add((multiplier / 2))
        }

        // "X hours Y minutes Z seconds" (compound)
        val compoundRegex = Regex(
            """(\d+(?:\.\d+)?)\s*(hour|hr)s?\s*(?:and\s*)?(\d+(?:\.\d+)?)?\s*(minute|min)s?\s*(?:and\s*)?(\d+(?:\.\d+)?)?\s*(second|sec)s?""",
        )
        compoundRegex.find(lower)?.let { match ->
            var total = 0
            val h = match.groupValues[1].toIntOrNull()
            if (h != null) total += h * 3600
            val m = match.groupValues[3].toIntOrNull()
            if (m != null) total += m * 60
            val s = match.groupValues[5].toIntOrNull()
            if (s != null) total += s
            results.add(total)
        }

        // "X minutes Y seconds" (two-part)
        val twoPartRegex = Regex(
            """(\d+(?:\.\d+)?)\s*(minute|min)s?\s*(?:and\s*)?(\d+(?:\.\d+)?)?\s*(second|sec)s?""",
        )
        twoPartRegex.find(lower)?.let { match ->
            var total = 0
            val m = match.groupValues[1].toIntOrNull()
            if (m != null) total += m * 60
            val s = match.groupValues[3].toIntOrNull()
            if (s != null) total += s
            results.add(total)
        }

        // "X seconds/minutes/hours" (simple, with optional word number)
        val simpleRegex = Regex(
            """(\d+(?:\.\d+)?|[a-z]+)\s*(hour|minute|min|second|sec)s?""",
        )
        for (match in simpleRegex.findAll(lower)) {
            val value = parseNumber(match.groupValues[1])
            val unit = match.groupValues[2]
            val multiplier = unitMultipliers[unit] ?: unitMultipliers["${unit}s"] ?: 1
            if (value != null) {
                results.add((value * multiplier).toInt())
            }
        }

        // "a minute", "a second"
        val aUnitRegex = Regex("""\ba\s+(hour|minute|min|second|sec)s?""")
        aUnitRegex.find(lower)?.let { match ->
            val unit = match.groupValues[1]
            val multiplier = unitMultipliers[unit] ?: unitMultipliers["${unit}s"] ?: 1
            results.add(multiplier)
        }

        return results.distinct()
    }

    // -- Number ------------------------------------------------------------

    override fun extractNumber(text: String): List<Double> {
        val results = mutableListOf<Double>()
        val lower = text.lowercase().trim()

        // Decimal: "1.5", "2.0"
        Regex("""\b(\d+\.\d+)\b""").findAll(lower).forEach { match ->
            match.groupValues[1].toDoubleOrNull()?.let { results.add(it) }
        }

        // Integer: "30", "100"
        Regex("""\b(\d+)\b""").findAll(lower).forEach { match ->
            match.groupValues[1].toDoubleOrNull()?.let { results.add(it) }
        }

        // Word numbers: "one", "thirty", "two" — only if no digit numbers found
        if (results.isEmpty()) {
            for ((word, value) in wordNumbers.entries.sortedByDescending { it.key.length }) {
                val regex = Regex("""\b$word\b""")
                if (regex.containsMatchIn(lower)) {
                    results.add(value.toDouble())
                }
            }
        }

        // "double" = 2.0x
        if (Regex("""\bdouble\b""").containsMatchIn(lower)) results.add(2.0)

        // "half" = 0.5 (when standalone, not part of duration)
        if (Regex("""\ba?\s*half\b""").containsMatchIn(lower) && results.isEmpty()) {
            results.add(0.5)
        }

        // Multiplier suffix: "2x" → 2.0
        Regex("""(\d+\.?\d*)\s*x\b""").find(lower)?.let { match ->
            match.groupValues[1].toDoubleOrNull()?.let { results.add(it) }
        }

        return results.distinct()
    }

    // -- Ordinal -----------------------------------------------------------

    private val ordinalWords = mapOf(
        "first" to 0, "second" to 1, "third" to 2, "fourth" to 3,
        "fifth" to 4, "sixth" to 5, "seventh" to 6, "eighth" to 7,
        "ninth" to 8, "tenth" to 9, "last" to -1,
    )

    override fun extractOrdinal(text: String): List<Int> {
        val results = mutableListOf<Int>()
        val lower = text.lowercase().trim()

        // Numeric ordinal: "3rd", "1st", "2nd", "4th"
        Regex("""\b(\d+)(?:st|nd|rd|th)\b""").findAll(lower).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let { results.add(it - 1) }
        }

        // Word ordinal: "first" → 0, "third" → 2, "last" → -1
        for ((word, index) in ordinalWords.entries.sortedByDescending { it.key.length }) {
            if (Regex("""\b$word\b""").containsMatchIn(lower)) {
                results.add(index)
                break
            }
        }

        return results.distinct()
    }

    // -- Trim mode ---------------------------------------------------------

    private val trimModeKeywords = mapOf(
        "off" to "off", "none" to "off", "no trim" to "off",
        "low" to "low", "mild" to "low",
        "medium" to "medium", "moderate" to "medium", "normal" to "medium",
        "high" to "high", "max" to "high", "aggressive" to "high", "maximum" to "high",
    )

    override fun extractTrimMode(text: String): String? {
        val lower = text.lowercase().trim()
        for ((keyword, mode) in trimModeKeywords.entries.sortedByDescending { it.key.length }) {
            if (lower.contains(keyword)) return mode
        }
        return null
    }

    // -- Boolean -----------------------------------------------------------

    override fun extractBoolean(text: String): Boolean? {
        val lower = text.lowercase().trim()

        // Affirmative
        if (Regex("""\b(on|enable|turn on|enabled|activate|yes)\b""").containsMatchIn(lower)) return true

        // Negative
        if (Regex("""\b(off|disable|turn off|disabled|deactivate|no\b)\b""").containsMatchIn(lower)) return false

        return null
    }

    // -- Helpers -----------------------------------------------------------

    /** Parse a numeric string or word number to Double. */
    private fun parseNumber(raw: String): Double? {
        raw.toDoubleOrNull()?.let { return it }
        val lower = raw.lowercase()
        wordNumbers[lower]?.let { return it.toDouble() }
        numericParts[lower]?.let { return it }
        return null
    }
}
