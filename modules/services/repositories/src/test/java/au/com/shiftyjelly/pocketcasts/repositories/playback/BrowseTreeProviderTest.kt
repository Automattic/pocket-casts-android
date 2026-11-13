package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.MediaItem
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
    private lateinit var showNotesManager: ShowNotesManager
    private lateinit var provider: BrowseTreeProvider
    private val clock = MutableClock()

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
        showNotesManager = mock()
        provider = BrowseTreeProvider(
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            folderManager = folderManager,
            userEpisodeManager = userEpisodeManager,
            playlistManager = playlistManager,
            upNextQueue = upNextQueue,
            settings = settings,
            serviceManager = serviceManager,
            listRepository = mock(),
            showNotesManager = showNotesManager,
            clock = clock,
        )
    }

    // --- getRootId ---

    @Test
    fun `getRootId returns RECENT_ROOT when isRecent and has current episode`() {
        val result = provider.getRootId(
            isRecent = true,
            isSuggested = false,
            hasCurrentEpisode = true,
            isAutomotive = false,
        )
        assertEquals(RECENT_ROOT, result)
    }

    @Test
    fun `getRootId returns null when isRecent but no current episode`() {
        val result = provider.getRootId(
            isRecent = true,
            isSuggested = false,
            hasCurrentEpisode = false,
            isAutomotive = false,
        )
        assertNull(result)
    }

    @Test
    fun `getRootId returns SUGGESTED_ROOT when isSuggested on automotive`() {
        val result = provider.getRootId(
            isRecent = false,
            isSuggested = true,
            hasCurrentEpisode = false,
            isAutomotive = true,
        )
        assertEquals(SUGGESTED_ROOT, result)
    }

    @Test
    fun `getRootId returns MEDIA_ID_ROOT when isSuggested on phone`() {
        val result = provider.getRootId(
            isRecent = false,
            isSuggested = true,
            hasCurrentEpisode = true,
            isAutomotive = false,
        )
        assertEquals(MEDIA_ID_ROOT, result)
    }

    @Test
    fun `getRootId returns MEDIA_ID_ROOT by default`() {
        val result = provider.getRootId(
            isRecent = false,
            isSuggested = false,
            hasCurrentEpisode = false,
            isAutomotive = false,
        )
        assertEquals(MEDIA_ID_ROOT, result)
    }

    @Test
    fun `getRootId prefers recent over suggested`() {
        val result = provider.getRootId(
            isRecent = true,
            isSuggested = true,
            hasCurrentEpisode = true,
            isAutomotive = true,
        )
        assertEquals(RECENT_ROOT, result)
    }

    // --- loadRecentChildren ---

    @Test
    fun `loadRecentChildren returns empty when no current episode`() = runTest {
        whenever(upNextQueue.currentEpisode).thenReturn(null)

        val result = provider.loadRecentChildren(mock())
        assertEquals(emptyList<MediaItem>(), result)
    }

    @Test
    fun `loadRecentChildren returns current episode`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val podcastOne = Podcast(uuid = UUID.randomUUID().toString())
        val currentEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastOne.uuid,
            publishedDate = Date(),
            title = "Episode 1",
        )
        whenever(upNextQueue.currentEpisode).thenReturn(currentEpisode)
        whenever(podcastManager.findPodcastByUuid(podcastOne.uuid)).thenReturn(podcastOne)
        mockArtworkConfiguration()

        val result = provider.loadRecentChildren(context)

        assertEquals(1, result.size)
        assertEquals(
            AutoMediaId(currentEpisode.uuid, podcastOne.uuid).toMediaId(),
            result[0].mediaId,
        )
    }

    @Test
    fun `loadUpNextChildren returns empty when queue is empty`() = runTest {
        whenever(upNextQueue.currentEpisode).thenReturn(null)
        whenever(upNextQueue.queueEpisodes).thenReturn(emptyList())

        val result = provider.loadUpNextChildren(mock())
        assertEquals(emptyList<MediaItem>(), result)
    }

    @Test
    fun `loadUpNextChildren returns current episode and queue`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val podcastOne = Podcast(uuid = UUID.randomUUID().toString())
        val podcastTwo = Podcast(uuid = UUID.randomUUID().toString())

        val currentEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastOne.uuid,
            publishedDate = Date(),
            title = "Current",
        )
        val queueEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastTwo.uuid,
            publishedDate = Date(),
            title = "Queued",
        )

        whenever(upNextQueue.currentEpisode).thenReturn(currentEpisode)
        whenever(upNextQueue.queueEpisodes).thenReturn(listOf(queueEpisode))
        whenever(podcastManager.findPodcastByUuid(podcastOne.uuid)).thenReturn(podcastOne)
        whenever(podcastManager.findPodcastByUuid(podcastTwo.uuid)).thenReturn(podcastTwo)
        mockArtworkConfiguration()

        val result = provider.loadUpNextChildren(context)

        assertEquals(2, result.size)
        assertEquals(
            AutoMediaId(currentEpisode.uuid, podcastOne.uuid).toMediaId(),
            result[0].mediaId,
        )
        assertEquals(
            AutoMediaId(queueEpisode.uuid, podcastTwo.uuid).toMediaId(),
            result[1].mediaId,
        )
    }

    // --- loadSuggestedChildren ---

    @Test
    fun `loadSuggestedChildren returns empty when queue is empty and no latest episode`() = runTest {
        whenever(upNextQueue.currentEpisode).thenReturn(null)
        whenever(upNextQueue.queueEpisodes).thenReturn(emptyList())
        mockAutoShowPlayed(false)
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(emptyList()))
        whenever(episodeManager.findLatestEpisodeToPlayBlocking()).thenReturn(null)

        val result = provider.loadSuggestedChildren(mock())
        assertEquals(emptyList<MediaItem>(), result)
    }

    @Test
    fun `loadSuggestedChildren returns up next, playlist, and latest episodes deduplicated`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val podcastOne = Podcast(uuid = UUID.randomUUID().toString())
        val podcastTwo = Podcast(uuid = UUID.randomUUID().toString())

        val currentEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastOne.uuid,
            publishedDate = Date(),
            title = "Episode 1",
        )
        val queueEpisode1 = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastTwo.uuid,
            publishedDate = Date(),
            title = "Episode 2",
        )
        val queueEpisode2 = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastOne.uuid,
            publishedDate = Date(),
            title = "Episode 3",
        )
        val playlistOnlyEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastOne.uuid,
            publishedDate = Date(),
            title = "Episode 4",
        )
        val latestEpisode = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            podcastUuid = podcastTwo.uuid,
            publishedDate = Date(),
            title = "Episode 5",
        )

        whenever(upNextQueue.currentEpisode).thenReturn(currentEpisode)
        whenever(upNextQueue.queueEpisodes).thenReturn(listOf(queueEpisode1, queueEpisode2))
        mockAutoShowPlayed(false)

        val playlistPreview = ManualPlaylistPreview(
            uuid = UUID.randomUUID().toString(),
            title = "Playlist title",
            settings = Playlist.Settings.ForPreview,
            icon = PlaylistIcon(0),
        )
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(listOf(playlistPreview)))
        whenever(playlistManager.smartPlaylistFlow(playlistPreview.uuid)).thenReturn(flowOf(null))
        whenever(playlistManager.manualPlaylistFlow(playlistPreview.uuid)).thenReturn(
            flowOf(
                ManualPlaylist(
                    uuid = playlistPreview.uuid,
                    title = playlistPreview.title,
                    episodes = listOf(
                        PlaylistEpisode.Available(playlistOnlyEpisode),
                        // duplicate of queueEpisode2 — should be filtered out
                        PlaylistEpisode.Available(queueEpisode2),
                    ),
                    settings = playlistPreview.settings,
                    metadata = Playlist.Metadata(
                        playbackDurationLeft = 0.seconds,
                        artworkUuids = emptyList(),
                        isShowingArchived = true,
                        totalEpisodeCount = 0,
                        displayedEpisodeCount = 0,
                        displayedAvailableEpisodeCount = 0,
                        archivedEpisodeCount = 0,
                    ),
                ),
            ),
        )

        whenever(episodeManager.findLatestEpisodeToPlayBlocking()).thenReturn(latestEpisode)
        whenever(podcastManager.findPodcastByUuid(podcastOne.uuid)).thenReturn(podcastOne)
        whenever(podcastManager.findPodcastByUuid(podcastTwo.uuid)).thenReturn(podcastTwo)
        mockArtworkConfiguration()

        val mediaItems = provider.loadSuggestedChildren(context)

        assertEquals(5, mediaItems.size)
        assertEquals(AutoMediaId(currentEpisode.uuid, podcastOne.uuid).toMediaId(), mediaItems[0].mediaId)
        assertEquals(AutoMediaId(queueEpisode1.uuid, podcastTwo.uuid).toMediaId(), mediaItems[1].mediaId)
        assertEquals(AutoMediaId(queueEpisode2.uuid, podcastOne.uuid).toMediaId(), mediaItems[2].mediaId)
        assertEquals(AutoMediaId(playlistOnlyEpisode.uuid, podcastOne.uuid).toMediaId(), mediaItems[3].mediaId)
        assertEquals(AutoMediaId(latestEpisode.uuid, podcastTwo.uuid).toMediaId(), mediaItems[4].mediaId)
    }

    // --- fetchEpisodeArtwork ---

    @Test
    fun `fetchEpisodeArtwork does nothing when episode artwork is off`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        mockArtworkConfiguration(useEpisodeArtwork = false)

        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, never()).updateEpisodeImageUrls(any())
    }

    @Test
    fun `fetchEpisodeArtwork ignores lists that are not podcasts`() = runTest {
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertFalse(provider.fetchEpisodeArtwork("playlist-uuid"))
        assertFalse(provider.fetchEpisodeArtwork(MEDIA_ID_ROOT))
        verify(podcastManager, never()).findPodcastByUuid(MEDIA_ID_ROOT)
        verify(showNotesManager, never()).updateEpisodeImageUrls(any())
    }

    @Test
    fun `fetchEpisodeArtwork fetches image URLs once and reports the change`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        whenever(showNotesManager.updateEpisodeImageUrls(podcast.uuid)).thenReturn(true)
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertTrue(provider.fetchEpisodeArtwork(podcast.uuid))
        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(1)).updateEpisodeImageUrls(podcast.uuid)
    }

    @Test
    fun `fetchEpisodeArtwork reports no change when no image URL changed`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        whenever(showNotesManager.updateEpisodeImageUrls(podcast.uuid)).thenReturn(false)
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager).updateEpisodeImageUrls(podcast.uuid)
    }

    @Test
    fun `fetchEpisodeArtwork tries again after a failed fetch`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        whenever(showNotesManager.updateEpisodeImageUrls(podcast.uuid))
            .thenThrow(RuntimeException("offline"))
            .thenReturn(true)
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        clock += EPISODE_ARTWORK_RETRY_INTERVAL - 1.seconds
        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(1)).updateEpisodeImageUrls(podcast.uuid)

        clock += 1.seconds
        assertTrue(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(2)).updateEpisodeImageUrls(podcast.uuid)
    }

    @Test
    fun `fetchEpisodeArtwork counts the retry interval from when a slow fetch fails`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        whenever(showNotesManager.updateEpisodeImageUrls(podcast.uuid))
            .thenAnswer {
                clock += 1.minutes
                throw RuntimeException("timed out")
            }
            .thenReturn(true)
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        clock += EPISODE_ARTWORK_RETRY_INTERVAL - 1.seconds
        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(1)).updateEpisodeImageUrls(podcast.uuid)

        clock += 1.seconds
        assertTrue(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(2)).updateEpisodeImageUrls(podcast.uuid)
    }

    @Test
    fun `fetchEpisodeArtwork fetches again after the refetch interval`() = runTest {
        val podcast = Podcast(uuid = UUID.randomUUID().toString())
        whenever(podcastManager.findPodcastByUuid(podcast.uuid)).thenReturn(podcast)
        whenever(showNotesManager.updateEpisodeImageUrls(podcast.uuid)).thenReturn(false, true)
        mockArtworkConfiguration(useEpisodeArtwork = true)

        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        clock += EPISODE_ARTWORK_REFETCH_INTERVAL - 1.seconds
        assertFalse(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(1)).updateEpisodeImageUrls(podcast.uuid)

        clock += 1.seconds
        assertTrue(provider.fetchEpisodeArtwork(podcast.uuid))
        verify(showNotesManager, times(2)).updateEpisodeImageUrls(podcast.uuid)
    }

    private fun mockAutoShowPlayed(value: Boolean) {
        val mockAutoShowPlayed = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<Boolean>>()
        whenever(mockAutoShowPlayed.value).thenReturn(value)
        whenever(settings.autoShowPlayed).thenReturn(mockAutoShowPlayed)
    }

    private fun mockArtworkConfiguration(useEpisodeArtwork: Boolean = false) {
        val mockArtworkConfig = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<ArtworkConfiguration>>()
        whenever(mockArtworkConfig.value).thenReturn(ArtworkConfiguration(useEpisodeArtwork = useEpisodeArtwork))
        whenever(settings.artworkConfiguration).thenReturn(mockArtworkConfig)
    }
}
