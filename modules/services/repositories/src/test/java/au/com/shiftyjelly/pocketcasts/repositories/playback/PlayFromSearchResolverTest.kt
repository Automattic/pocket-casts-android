package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import java.util.Date
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayFromSearchResolverTest {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var podcastManager: PodcastManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var playlistManager: PlaylistManager
    private lateinit var resolver: PlayFromSearchResolver

    private val source = SourceView.UNKNOWN

    @Before
    fun setUp() {
        playbackManager = mock()
        podcastManager = mock()
        episodeManager = mock()
        playlistManager = mock()
        resolver = PlayFromSearchResolver(playbackManager, podcastManager, episodeManager, playlistManager)
    }

    @Test
    fun `up next resumes the queue`() = runTest {
        val result = resolver.playFromSearch("Up Next", source)

        assertEquals(PlayFromSearchResult.UpNext, result)
        verify(playbackManager).playQueue(sourceView = source)
    }

    @Test
    fun `podcast match plays its latest unfinished episode`() = runTest {
        val podcast = Podcast(uuid = "pod-1", title = "Tech News")
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking("tech news")).thenReturn(podcast)
        whenever(episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)).thenReturn(episode)

        val result = resolver.playFromSearch("Tech News", source)

        assertEquals(PlayFromSearchResult.Episode(episode), result)
        verify(playbackManager).playNow(
            episode = eq(episode),
            forceStream = eq(false),
            showedStreamWarning = eq(false),
            sourceView = eq(source),
        )
    }

    @Test
    fun `podcast match with every episode finished plays nothing`() = runTest {
        val podcast = Podcast(uuid = "pod-1", title = "Tech News")
        whenever(podcastManager.searchPodcastByTitleBlocking("tech news")).thenReturn(podcast)
        whenever(episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)).thenReturn(null)

        val result = resolver.playFromSearch("Tech News", source)

        assertEquals(PlayFromSearchResult.PodcastWithNothingToPlay(podcast), result)
        verify(playbackManager, never()).playNow(any(), any(), any(), any())
    }

    @Test
    fun `falls back to shorter queries`() = runTest {
        val podcast = Podcast(uuid = "pod-1", title = "Tech News")
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking(any())).thenReturn(null)
        whenever(podcastManager.searchPodcastByTitleBlocking("tech news")).thenReturn(podcast)
        whenever(episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)).thenReturn(episode)

        val result = resolver.playFromSearch("tech news podcast", source)

        assertEquals(PlayFromSearchResult.Episode(episode), result)
    }

    @Test
    fun `episode title match plays the episode`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking(any())).thenReturn(null)
        whenever(episodeManager.findFirstBySearchQuery("my episode")).thenReturn(episode)

        val result = resolver.playFromSearch("My Episode", source)

        assertEquals(PlayFromSearchResult.Episode(episode), result)
    }

    @Test
    fun `no match returns not found`() = runTest {
        whenever(podcastManager.searchPodcastByTitleBlocking(any())).thenReturn(null)
        whenever(episodeManager.findFirstBySearchQuery(any())).thenReturn(null)
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(emptyList()))

        val result = resolver.playFromSearch("nonexistent query", source)

        assertEquals(PlayFromSearchResult.NotFound, result)
    }

    private fun createPodcastEpisode(uuid: String) = PodcastEpisode(
        uuid = uuid,
        title = "Test Episode",
        publishedDate = Date(),
        podcastUuid = "podcast-uuid",
    )
}
