package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BrowseTreeProviderTest {

    private lateinit var podcastManager: PodcastManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var folderManager: FolderManager
    private lateinit var userEpisodeManager: UserEpisodeManager
    private lateinit var playlistManager: PlaylistManager
    private lateinit var upNextQueue: UpNextQueue
    private lateinit var settings: Settings
    private lateinit var serviceManager: ServiceManager
    private lateinit var podcastCacheServiceManager: PodcastCacheServiceManager
    private lateinit var provider: BrowseTreeProvider
    private lateinit var context: Context

    @Before
    fun setUp() {
        podcastManager = mock()
        episodeManager = mock()
        folderManager = mock()
        userEpisodeManager = mock()
        playlistManager = mock()
        upNextQueue = mock()
        settings = mock()
        serviceManager = mock()
        podcastCacheServiceManager = mock()
        context = mock()

        provider = BrowseTreeProvider(
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            folderManager = folderManager,
            userEpisodeManager = userEpisodeManager,
            playlistManager = playlistManager,
            upNextQueue = upNextQueue,
            settings = settings,
            serviceManager = serviceManager,
            podcastCacheServiceManager = podcastCacheServiceManager,
        )
    }

    // --- getRootId ---

    @Test
    fun `getRootId returns RECENT_ROOT when isRecent and has current episode`() {
        val result = provider.getRootId(isRecent = true, isSuggested = false, hasCurrentEpisode = true)
        assertEquals(RECENT_ROOT, result)
    }

    @Test
    fun `getRootId returns null when isRecent but no current episode`() {
        val result = provider.getRootId(isRecent = true, isSuggested = false, hasCurrentEpisode = false)
        assertNull(result)
    }

    @Test
    fun `getRootId returns SUGGESTED_ROOT when isSuggested`() {
        val result = provider.getRootId(isRecent = false, isSuggested = true, hasCurrentEpisode = false)
        assertEquals(SUGGESTED_ROOT, result)
    }

    @Test
    fun `getRootId returns MEDIA_ID_ROOT by default`() {
        val result = provider.getRootId(isRecent = false, isSuggested = false, hasCurrentEpisode = false)
        assertEquals(MEDIA_ID_ROOT, result)
    }

    @Test
    fun `getRootId prefers recent over suggested`() {
        val result = provider.getRootId(isRecent = true, isSuggested = true, hasCurrentEpisode = true)
        assertEquals(RECENT_ROOT, result)
    }

    // --- loadRecentChildren ---

    @Test
    fun `loadRecentChildren returns empty when no current episode`() = runTest {
        whenever(upNextQueue.currentEpisode).thenReturn(null)

        val result = provider.loadRecentChildren(context)
        assertEquals(emptyList<MediaBrowserCompat.MediaItem>(), result)
    }

    // --- loadSuggestedChildren ---

    @Test
    fun `loadSuggestedChildren returns empty when queue is empty and no latest episode`() = runTest {
        whenever(upNextQueue.currentEpisode).thenReturn(null)
        whenever(upNextQueue.queueEpisodes).thenReturn(emptyList())
        mockAutoShowPlayed(false)
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(emptyList()))
        whenever(episodeManager.findLatestEpisodeToPlayBlocking()).thenReturn(null)

        val result = provider.loadSuggestedChildren(context)
        assertEquals(emptyList<MediaBrowserCompat.MediaItem>(), result)
    }

    private fun mockAutoShowPlayed(value: Boolean) {
        val mockAutoShowPlayed = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<Boolean>>()
        whenever(mockAutoShowPlayed.value).thenReturn(value)
        whenever(settings.autoShowPlayed).thenReturn(mockAutoShowPlayed)
    }
}
