package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import com.automattic.eventhorizon.EpisodeViewSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TvEpisodeActionTypeTest {

    @Test
    fun `every action context maps to the matching analytics and event sources`() {
        val expected = mapOf(
            TvEpisodeActionContext.PodcastDetails to (SourceView.PODCAST_SCREEN to EpisodeViewSourceType.PodcastScreen),
            TvEpisodeActionContext.SearchResults to (SourceView.SEARCH_RESULTS to EpisodeViewSourceType.Search),
            TvEpisodeActionContext.Playlist to (SourceView.FILTERS to EpisodeViewSourceType.Filters),
            TvEpisodeActionContext.UpNext to (SourceView.UP_NEXT to EpisodeViewSourceType.UpNext),
            TvEpisodeActionContext.NowPlaying to (SourceView.PLAYER to EpisodeViewSourceType.NowPlaying),
            TvEpisodeActionContext.Starred to (SourceView.STARRED to EpisodeViewSourceType.Starred),
            TvEpisodeActionContext.ListeningHistory to (SourceView.LISTENING_HISTORY to EpisodeViewSourceType.ListeningHistory),
        )

        assertEquals(TvEpisodeActionContext.entries.toSet(), expected.keys)
        TvEpisodeActionContext.entries.forEach { context ->
            val (source, episodeViewSource) = expected.getValue(context)
            assertEquals(source, context.source)
            assertEquals(episodeViewSource, context.episodeViewSource)
        }
    }

    @Test
    fun `podcast details shows played and archive toggles but no go to podcast`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.PodcastDetails, showGoToPodcast = false)

        assertEquals(
            listOf(
                TvEpisodeActionType.Play,
                TvEpisodeActionType.Details,
                TvEpisodeActionType.PlayNext,
                TvEpisodeActionType.PlayLast,
                TvEpisodeActionType.TogglePlayed,
                TvEpisodeActionType.ToggleArchived,
            ),
            actions,
        )
    }

    @Test
    fun `search results uses the search results analytics source`() {
        assertEquals(SourceView.SEARCH_RESULTS, TvEpisodeActionContext.SearchResults.source)
    }

    @Test
    fun `playlist shows go to podcast alongside played and archive toggles`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.Playlist, showGoToPodcast = true)

        assertEquals(
            listOf(
                TvEpisodeActionType.Play,
                TvEpisodeActionType.Details,
                TvEpisodeActionType.GoToPodcast,
                TvEpisodeActionType.PlayNext,
                TvEpisodeActionType.PlayLast,
                TvEpisodeActionType.TogglePlayed,
                TvEpisodeActionType.ToggleArchived,
            ),
            actions,
        )
    }

    @Test
    fun `up next shows remove instead of played and archive toggles`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.UpNext, showGoToPodcast = true)

        assertEquals(
            listOf(
                TvEpisodeActionType.Play,
                TvEpisodeActionType.Details,
                TvEpisodeActionType.GoToPodcast,
                TvEpisodeActionType.PlayNext,
                TvEpisodeActionType.PlayLast,
                TvEpisodeActionType.RemoveFromUpNext,
            ),
            actions,
        )
    }

    @Test
    fun `now playing shows the current episode actions without play or queue actions`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.NowPlaying, showGoToPodcast = true)

        assertEquals(
            listOf(
                TvEpisodeActionType.TogglePlayed,
                TvEpisodeActionType.ToggleArchived,
                TvEpisodeActionType.GoToPodcast,
            ),
            actions,
        )
    }

    @Test
    fun `listening history shows played and archive toggles but no go to podcast`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.ListeningHistory, showGoToPodcast = false)

        assertEquals(
            listOf(
                TvEpisodeActionType.Play,
                TvEpisodeActionType.Details,
                TvEpisodeActionType.PlayNext,
                TvEpisodeActionType.PlayLast,
                TvEpisodeActionType.TogglePlayed,
                TvEpisodeActionType.ToggleArchived,
            ),
            actions,
        )
    }

    @Test
    fun `go to podcast is hidden when no navigation is available`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.Playlist, showGoToPodcast = false)

        assertFalse(actions.contains(TvEpisodeActionType.GoToPodcast))
    }
}
