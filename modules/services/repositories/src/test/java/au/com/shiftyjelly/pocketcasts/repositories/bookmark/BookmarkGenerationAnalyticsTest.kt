package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import com.automattic.eventhorizon.BookmarkEnrichmentTriggerType
import com.automattic.eventhorizon.BookmarkTitleGeneratedEvent
import com.automattic.eventhorizon.BookmarkTitleGenerationFailedEvent
import com.automattic.eventhorizon.BookmarkTitleGeneratorType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SourceViewType
import com.automattic.eventhorizon.Trackable
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkGenerationAnalyticsTest {

    private val events = mutableListOf<Trackable>()
    private val analytics = BookmarkGenerationAnalytics(EventHorizon { events += it })

    @Test
    fun `reports a generated title with its word count`() {
        analytics.report(
            generation = TitleGeneration(title = "A great moment", durationMs = 42, failureReason = null),
            episodeUuid = "episode-id",
            podcastUuid = "podcast-id",
            trigger = BookmarkEnrichmentTriggerType.Background,
            source = SourceViewType.Headphones,
        )

        val event = events.single() as BookmarkTitleGeneratedEvent
        assertEquals(BookmarkTitleGeneratorType.Server, event.generator)
        assertEquals(42L, event.durationMs)
        assertEquals(3L, event.wordCount)
        assertEquals(BookmarkEnrichmentTriggerType.Background, event.trigger)
        assertEquals("podcast-id", event.podcastUuid)
    }

    @Test
    fun `reports a generation failure with its reason`() {
        analytics.report(
            generation = TitleGeneration(title = null, durationMs = 10, failureReason = "server_error"),
            episodeUuid = "episode-id",
            podcastUuid = null,
            trigger = BookmarkEnrichmentTriggerType.EditSheet,
            source = SourceViewType.Player,
        )

        val event = events.single() as BookmarkTitleGenerationFailedEvent
        assertEquals("server_error", event.reason)
        assertEquals(BookmarkTitleGeneratorType.Server, event.generator)
        assertEquals(10L, event.durationMs)
    }
}
