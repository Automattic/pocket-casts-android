package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BookmarkEpisodeResolverTest {

    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val resolver = BookmarkEpisodeResolver(episodeManager, podcastManager)

    private val bookmark = Bookmark("uuid", episodeUuid = "episode", podcastUuid = "podcast", timeSecs = 0)

    @Test
    fun `returns the local episode when it exists`() = runTest {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(episode)

        assertEquals(episode, resolver.resolve(bookmark))
    }

    @Test
    fun `fetches a missing episode for an unsubscribed podcast`() = runTest {
        val fetched = PodcastEpisode(uuid = "episode", podcastUuid = "podcast", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(null)
        whenever(podcastManager.findOrDownloadPodcast("podcast")).thenReturn(Podcast(uuid = "podcast", isSubscribed = false))
        whenever(episodeManager.downloadMissingPodcastEpisode("episode", "podcast")).thenReturn(fetched)

        assertEquals(fetched, resolver.resolve(bookmark))
    }

    @Test
    fun `skips the server fetch when adding the podcast inserts the episode`() = runTest {
        val inserted = PodcastEpisode(uuid = "episode", podcastUuid = "podcast", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(null, inserted)
        whenever(podcastManager.findOrDownloadPodcast("podcast")).thenReturn(Podcast(uuid = "podcast", isSubscribed = false))

        assertEquals(inserted, resolver.resolve(bookmark))
        verify(episodeManager, never()).downloadMissingPodcastEpisode(any(), any())
    }

    @Test
    fun `returns null when the podcast cannot be fetched`() = runTest {
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(null)
        whenever(podcastManager.findOrDownloadPodcast("podcast")).thenThrow(RuntimeException("no podcast"))

        assertNull(resolver.resolve(bookmark))
    }

    @Test
    fun `returns null when the missing episode fetch fails`() = runTest {
        whenever(episodeManager.findEpisodeByUuid("episode")).thenReturn(null)
        whenever(podcastManager.findOrDownloadPodcast("podcast")).thenReturn(Podcast(uuid = "podcast", isSubscribed = false))
        whenever(episodeManager.downloadMissingPodcastEpisode("episode", "podcast")).thenThrow(RuntimeException("offline"))

        assertNull(resolver.resolve(bookmark))
    }
}
