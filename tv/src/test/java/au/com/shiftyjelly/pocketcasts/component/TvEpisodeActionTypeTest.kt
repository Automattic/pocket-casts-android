package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TvEpisodeActionTypeTest {

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
    fun `go to podcast is hidden when no navigation is available`() {
        val actions = tvEpisodeActionTypes(TvEpisodeActionContext.Playlist, showGoToPodcast = false)

        assertFalse(actions.contains(TvEpisodeActionType.GoToPodcast))
    }
}
