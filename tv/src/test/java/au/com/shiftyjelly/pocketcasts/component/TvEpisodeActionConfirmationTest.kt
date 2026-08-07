package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvEpisodeActionConfirmationTest {

    private val episode = PodcastEpisode(uuid = "episode", publishedDate = Date(0))
    private val finishedEpisode = episode.copy(playingStatus = EpisodePlayingStatus.COMPLETED)
    private val archivedEpisode = episode.copy(isArchived = true)

    @Test
    fun `marking the now playing episode as played requires confirmation`() {
        assertTrue(
            tvEpisodeActionRequiresConfirmation(TvEpisodeActionContext.NowPlaying, TvEpisodeActionType.TogglePlayed, episode),
        )
    }

    @Test
    fun `archiving the now playing episode requires confirmation`() {
        assertTrue(
            tvEpisodeActionRequiresConfirmation(TvEpisodeActionContext.NowPlaying, TvEpisodeActionType.ToggleArchived, episode),
        )
    }

    @Test
    fun `marking as unplayed does not require confirmation`() {
        assertFalse(
            tvEpisodeActionRequiresConfirmation(TvEpisodeActionContext.NowPlaying, TvEpisodeActionType.TogglePlayed, finishedEpisode),
        )
    }

    @Test
    fun `unarchiving does not require confirmation`() {
        assertFalse(
            tvEpisodeActionRequiresConfirmation(TvEpisodeActionContext.NowPlaying, TvEpisodeActionType.ToggleArchived, archivedEpisode),
        )
    }

    @Test
    fun `other actions do not require confirmation`() {
        val otherTypes = TvEpisodeActionType.entries - setOf(TvEpisodeActionType.TogglePlayed, TvEpisodeActionType.ToggleArchived)

        assertTrue(
            otherTypes.none { tvEpisodeActionRequiresConfirmation(TvEpisodeActionContext.NowPlaying, it, episode) },
        )
    }

    @Test
    fun `other contexts do not require confirmation`() {
        val otherContexts = TvEpisodeActionContext.entries - TvEpisodeActionContext.NowPlaying

        assertTrue(
            otherContexts.none { context ->
                TvEpisodeActionType.entries.any { tvEpisodeActionRequiresConfirmation(context, it, episode) }
            },
        )
    }
}
