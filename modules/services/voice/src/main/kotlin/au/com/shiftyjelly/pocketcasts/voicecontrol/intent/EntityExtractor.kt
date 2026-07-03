package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

/**
 * Extracts typed entity values from a transcript for a given intent.
 * Implementations handle per-language grammar-based extraction.
 */
interface EntityExtractor {
    /**
     * Extract entities from [text] relevant to [intentType].
     * Returns [EntityResult] with extracted values (nulls for unfound entities).
     */
    fun extract(text: String, intentType: String): EntityResult
}

/**
 * Typed entity values extracted from a transcript.
 * All fields are nullable — the caller applies defaults for unfound entities.
 */
data class EntityResult(
    val deltaSeconds: Int? = null,
    val positionSeconds: Int? = null,
    val speed: Double? = null,
    val speedDelta: Double? = null,
    val volume: Int? = null,
    val volumeDelta: Int? = null,
    val sleepMinutes: Int? = null,
    val chapterIndex: Int? = null,
    val chapterTitle: String? = null,
    val trimMode: String? = null,
    val boostEnabled: Boolean? = null,
    val bookmarkTitle: String? = null,
)
