package au.com.shiftyjelly.pocketcasts.component

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeActionButtonsTest {

    @Test
    fun `podcast details shows played and archive toggles but no go to podcast`() {
        val buttons = episodeActionButtons(TvEpisodeActionContext.PodcastDetails, showGoToPodcast = false)

        assertEquals(
            listOf(
                EpisodeActionButton.Details,
                EpisodeActionButton.PlayNext,
                EpisodeActionButton.PlayLast,
                EpisodeActionButton.TogglePlayed,
                EpisodeActionButton.ToggleArchived,
            ),
            buttons,
        )
    }

    @Test
    fun `playlist shows go to podcast alongside played and archive toggles`() {
        val buttons = episodeActionButtons(TvEpisodeActionContext.Playlist, showGoToPodcast = true)

        assertEquals(
            listOf(
                EpisodeActionButton.Details,
                EpisodeActionButton.GoToPodcast,
                EpisodeActionButton.PlayNext,
                EpisodeActionButton.PlayLast,
                EpisodeActionButton.TogglePlayed,
                EpisodeActionButton.ToggleArchived,
            ),
            buttons,
        )
    }

    @Test
    fun `up next shows remove instead of played and archive toggles`() {
        val buttons = episodeActionButtons(TvEpisodeActionContext.UpNext, showGoToPodcast = true)

        assertEquals(
            listOf(
                EpisodeActionButton.Details,
                EpisodeActionButton.GoToPodcast,
                EpisodeActionButton.PlayNext,
                EpisodeActionButton.PlayLast,
                EpisodeActionButton.RemoveFromUpNext,
            ),
            buttons,
        )
    }

    @Test
    fun `go to podcast is hidden when no navigation is available`() {
        val buttons = episodeActionButtons(TvEpisodeActionContext.Playlist, showGoToPodcast = false)

        assertEquals(false, buttons.contains(EpisodeActionButton.GoToPodcast))
    }
}
