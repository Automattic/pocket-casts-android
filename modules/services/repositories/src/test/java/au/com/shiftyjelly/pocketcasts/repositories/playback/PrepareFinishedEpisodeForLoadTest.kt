package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class PrepareFinishedEpisodeForLoadTest {

    private val episodeManager = mock<EpisodeManager>()

    @Test
    fun `marks a finished episode as not played when playback starts`() {
        val episode = finishedEpisode()

        prepareFinishedEpisodeForLoad(episode, play = true, episodeManager = episodeManager)

        verify(episodeManager).markAsNotPlayedBlocking(episode)
    }

    @Test
    fun `keeps a finished episode completed when only loading it`() {
        val episode = finishedEpisode()

        prepareFinishedEpisodeForLoad(episode, play = false, episodeManager = episodeManager)

        verify(episodeManager, never()).markAsNotPlayedBlocking(any())
        assertEquals(EpisodePlayingStatus.COMPLETED, episode.playingStatus)
    }

    @Test
    fun `loads a finished episode from the start without playing it`() {
        val episode = finishedEpisode()

        prepareFinishedEpisodeForLoad(episode, play = false, episodeManager = episodeManager)

        assertEquals(0.0, episode.playedUpTo, 0.0)
    }

    @Test
    fun `leaves an in progress episode alone`() {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = Date(), playingStatus = EpisodePlayingStatus.IN_PROGRESS, playedUpTo = 120.0)

        prepareFinishedEpisodeForLoad(episode, play = true, episodeManager = episodeManager)

        verify(episodeManager, never()).markAsNotPlayedBlocking(any())
        assertEquals(120.0, episode.playedUpTo, 0.0)
    }

    private fun finishedEpisode() = PodcastEpisode(
        uuid = "episode",
        publishedDate = Date(),
        playingStatus = EpisodePlayingStatus.COMPLETED,
        playedUpTo = 3600.0,
        duration = 3600.0,
    )
}
