package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.entity

/**
 * Per-language grammar for extracting typed entities from voice command transcripts.
 * Each supported language provides an implementation with regex-based patterns
 * for duration, number, ordinal, trim mode, and boolean detection.
 */
interface LanguageGrammar {
    /** ISO 639-1 language code. */
    val languageCode: String

    /** Return true if this grammar can parse the given text (script/heuristic detection). */
    fun canParse(text: String): Boolean

    /** Extract duration expressions and return values in seconds. */
    fun extractDuration(text: String): List<Int>

    /** Extract cardinal numbers (for speed, volume). */
    fun extractNumber(text: String): List<Double>

    /** Extract ordinal numbers → 0-based index ("first" → 0, "third" → 2). */
    fun extractOrdinal(text: String): List<Int>

    /** Extract trim mode keywords → "off"|"low"|"medium"|"high". */
    fun extractTrimMode(text: String): String?

    /** Extract boolean (affirmative/negative) → true/false, or null if ambiguous. */
    fun extractBoolean(text: String): Boolean?
}
