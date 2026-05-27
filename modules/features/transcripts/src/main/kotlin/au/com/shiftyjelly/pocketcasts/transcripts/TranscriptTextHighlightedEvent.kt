package au.com.shiftyjelly.pocketcasts.transcripts

import com.automattic.eventhorizon.Trackable
import com.automattic.eventhorizon.TranscriptSourceType

data class TranscriptTextHighlightedEvent(
    val podcastUuid: String,
    val episodeUuid: String,
    val source: TranscriptSourceType,
) : Trackable {
    override val analyticsName: String
        get() = "transcript_text_highlighted"

    override val analyticsProperties: Map<String, Any>
        get() = mapOf(
            "podcast_uuid" to podcastUuid,
            "episode_uuid" to episodeUuid,
            "source" to source.analyticsValue,
        )
}
