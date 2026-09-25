package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import com.automattic.eventhorizon.BookmarkEnrichmentTriggerType
import com.automattic.eventhorizon.BookmarkTitleGeneratedEvent
import com.automattic.eventhorizon.BookmarkTitleGenerationFailedEvent
import com.automattic.eventhorizon.BookmarkTitleGeneratorType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SourceViewType
import javax.inject.Inject

class BookmarkGenerationAnalytics @Inject constructor(
    private val eventHorizon: EventHorizon,
) {
    fun report(
        generation: TitleGeneration,
        episodeUuid: String,
        podcastUuid: String?,
        trigger: BookmarkEnrichmentTriggerType,
        source: SourceViewType,
    ) {
        val reason = generation.failureReason
        if (reason == null) {
            eventHorizon.track(
                BookmarkTitleGeneratedEvent(
                    generator = BookmarkTitleGeneratorType.Server,
                    durationMs = generation.durationMs,
                    wordCount = countWords(generation.title.orEmpty()).toLong(),
                    trigger = trigger,
                    source = source,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
        } else {
            eventHorizon.track(
                BookmarkTitleGenerationFailedEvent(
                    reason = reason,
                    generator = BookmarkTitleGeneratorType.Server,
                    durationMs = generation.durationMs,
                    trigger = trigger,
                    source = source,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
        }
    }

    private fun countWords(text: String) = text.split(WORD_SEPARATOR).count { it.isNotBlank() }

    private companion object {
        private val WORD_SEPARATOR = Regex("\\s+")
    }
}
