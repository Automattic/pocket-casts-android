package au.com.shiftyjelly.pocketcasts.models.to

import com.automattic.eventhorizon.TranscriptType as EventHorizonTranscriptType

enum class TranscriptType(
    val analyticsValue: EventHorizonTranscriptType,
    private val associatedMimeTypes: Set<String>,
) {
    Vtt(
        analyticsValue = EventHorizonTranscriptType.Vtt,
        associatedMimeTypes = setOf("text/vtt"),
    ),
    Srt(
        analyticsValue = EventHorizonTranscriptType.Srt,
        associatedMimeTypes = setOf("application/srt", "application/x-subrip"),
    ),
    Json(
        analyticsValue = EventHorizonTranscriptType.Json,
        associatedMimeTypes = setOf("application/json"),
    ),
    Html(
        analyticsValue = EventHorizonTranscriptType.Html,
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
