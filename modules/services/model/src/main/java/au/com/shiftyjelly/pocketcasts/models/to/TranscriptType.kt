package au.com.shiftyjelly.pocketcasts.models.to

enum class TranscriptType(
    val analyticsValue: String,
    private val associatedMimeTypes: Set<String>,
) {
    Vtt(
        analyticsValue = "vtt",
        associatedMimeTypes = setOf("text/vtt"),
    ),
    Srt(
        analyticsValue = "srt",
        associatedMimeTypes = setOf("application/srt", "application/x-subrip"),
    ),
    Json(
        analyticsValue = "json",
        associatedMimeTypes = setOf("application/json"),
    ),
    Html(
        analyticsValue = "html",
        associatedMimeTypes = setOf("text/html"),
    ),
    ;

    companion object {
        fun fromMimeType(value: String): TranscriptType? {
            val lowercaseValue = value.lowercase()
            return entries.firstOrNull { lowercaseValue in it.associatedMimeTypes }
        }
    }
}
