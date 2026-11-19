package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import com.automattic.pocketcasts.playback.PlaybackAnalyticsEvent
import com.automattic.pocketcasts.playback.PlaybackAnalyticsEvent.Story
import org.junit.Assert.assertEquals
import org.junit.Test

class Playback2026AnalyticsTest {
    private val events = listOf(
        PlaybackAnalyticsEvent.ArchetypeAssigned(PlaybackAnalyticsEvent.Archetype.Flaneur),
        PlaybackAnalyticsEvent.StoryShown(Story.Cover, index = 0, count = 12, revisit = false),
        PlaybackAnalyticsEvent.QuizRevealed(Story.Top1Show, PlaybackAnalyticsEvent.QuizResult.NoGuess, PlaybackAnalyticsEvent.RevealReason.Timeout),
        PlaybackAnalyticsEvent.StoryShare(Story.Summary, PlaybackAnalyticsEvent.SummaryVariant.Bubbles),
        PlaybackAnalyticsEvent.StoryShared(Story.Top5Shows, PlaybackAnalyticsEvent.ShareSource.Screenshot, activity = null, variant = null),
        PlaybackAnalyticsEvent.ReplayButtonTapped,
        PlaybackAnalyticsEvent.StoriesDismissed(PlaybackAnalyticsEvent.DismissSource.BackButton, Story.Ending, finished = true),
        PlaybackAnalyticsEvent.StoriesFailedToLoad,
    )

    @Test
    fun `every event goes out under its schema name with current_year`() {
        events.forEach { event ->
            val trackable = event.toTrackable()

            assertEquals(event.name, trackable.analyticsName)
            assertEquals(event.properties.keys + "current_year", trackable.analyticsProperties.keys)
            assertEquals(2026L, trackable.analyticsProperties["current_year"])
        }
    }

    @Test
    fun `story shown has its position as numbers and revisit as a boolean`() {
        val trackable = PlaybackAnalyticsEvent.StoryShown(Story.YearVsYear, index = 6, count = 12, revisit = true).toTrackable()

        assertEquals("end_of_year_story_shown", trackable.analyticsName)
        assertEquals(
            mapOf("story" to "year_vs_year", "index" to 6L, "count" to 12L, "revisit" to true, "current_year" to 2026L),
            trackable.analyticsProperties,
        )
    }

    @Test
    fun `a share through the share sheet carries the app and the summary card`() {
        val trackable = PlaybackAnalyticsEvent.StoryShared(
            Story.Summary,
            PlaybackAnalyticsEvent.ShareSource.Button,
            activity = "com.google.android.apps.messaging.ui.conversationlist.ShareIntentActivity",
            variant = PlaybackAnalyticsEvent.SummaryVariant.Grid,
        ).toTrackable()

        assertEquals("end_of_year_story_shared", trackable.analyticsName)
        assertEquals(
            mapOf(
                "story" to "summary",
                "from" to "button",
                "activity" to "com.google.android.apps.messaging.ui.conversationlist.ShareIntentActivity",
                "variant" to "a",
                "current_year" to 2026L,
            ),
            trackable.analyticsProperties,
        )
    }

    @Test
    fun `dismissed says whether the last story was reached`() {
        val trackable = PlaybackAnalyticsEvent.StoriesDismissed(PlaybackAnalyticsEvent.DismissSource.CloseButton, Story.Ending, finished = true).toTrackable()

        assertEquals(
            mapOf("source" to "close_button", "story" to "ending", "finished" to true, "current_year" to 2026L),
            trackable.analyticsProperties,
        )
    }
}
