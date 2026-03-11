package au.com.shiftyjelly.pocketcasts.models.to

import com.automattic.eventhorizon.TranscriptType as EventHorizonTranscriptType

enum class TranscriptType(
    val eventHorizonValue: EventHorizonTranscriptType,
    private val associatedMimeTypes: Set<String>,
) {
    Vtt(
        eventHorizonValue = EventHorizonTranscriptType.Vtt,
        associatedMimeTypes = setOf("text/vtt"),
    ),
    Srt(
        eventHorizonValue = EventHorizonTranscriptType.Srt,
        associatedMimeTypes = setOf("application/srt", "application/x-subrip"),
    ),
    Json(
        eventHorizonValue = EventHorizonTranscriptType.Json,
        associatedMimeTypes = setOf("application/json"),
    ),
    Html(
        eventHorizonValue = EventHorizonTranscriptType.Html,
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
