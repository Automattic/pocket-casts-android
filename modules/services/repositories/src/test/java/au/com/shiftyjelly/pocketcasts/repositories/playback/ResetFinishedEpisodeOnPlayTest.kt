package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ResetFinishedEpisodeOnPlayTest {

    private val episodeManager = mock<EpisodeManager>()

    @Test
    fun `marks the latest copy of a finished episode as not played`() = runTest {
        val upNextEpisode = finishedEpisode()
        val latestEpisode = finishedEpisode(isArchived = true)
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(latestEpisode)

        resetFinishedEpisodeOnPlay(upNextEpisode, episodeManager)

        verify(episodeManager).markAsNotPlayedBlocking(latestEpisode)
    }

    @Test
    fun `starts the Up Next copy of a finished episode from the beginning`() = runTest {
        val upNextEpisode = finishedEpisode()
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(finishedEpisode())

        resetFinishedEpisodeOnPlay(upNextEpisode, episodeManager)

        assertEquals(0, upNextEpisode.playedUpToMs)
        assertEquals(EpisodePlayingStatus.NOT_PLAYED, upNextEpisode.playingStatus)
    }

    @Test
    fun `leaves an episode alone when the latest copy is not finished`() = runTest {
        val upNextEpisode = PodcastEpisode(uuid = "episode", publishedDate = Date(), playingStatus = EpisodePlayingStatus.IN_PROGRESS, playedUpTo = 120.0)
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(upNextEpisode.copy())

        resetFinishedEpisodeOnPlay(upNextEpisode, episodeManager)

        verify(episodeManager, never()).markAsNotPlayedBlocking(any())
        assertEquals(120.0, upNextEpisode.playedUpTo, 0.0)
    }

    private fun finishedEpisode(isArchived: Boolean = false) = PodcastEpisode(
        uuid = "episode",
        publishedDate = Date(),
        playingStatus = EpisodePlayingStatus.COMPLETED,
        playedUpTo = 3600.0,
        duration = 3600.0,
        isArchived = isArchived,
    )
}
