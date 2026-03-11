package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MediaSessionActionsTest {

    private val playbackManager: PlaybackManager = mock()
    private val podcastManager: PodcastManager = mock()
    private val episodeManager: EpisodeManager = mock()
    private val playlistManager: PlaylistManager = mock()
    private val settings: Settings = mock()
    private val eventHorizon: EventHorizon = mock()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var actions: MediaSessionActions

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        actions = MediaSessionActions(
            playbackManager = playbackManager,
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            playlistManager = playlistManager,
            settings = settings,
            eventHorizon = eventHorizon,
            scope = testScope,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markAsPlayed marks current episode`() = testScope.runTest {
        val episode = PodcastEpisode(uuid = "ep1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.markAsPlayed()

        verify(episodeManager).markAsPlayedBlocking(episode, playbackManager, podcastManager)
        verify(eventHorizon).track(any())
    }

    @Test
    fun `starEpisode stars a podcast episode`() = testScope.runTest {
        val episode = PodcastEpisode(uuid = "ep1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.starEpisode()

        assertTrue(episode.isStarred)
        verify(episodeManager).starEpisode(episode = eq(episode), starred = eq(true), sourceView = any())
    }

    @Test
    fun `starEpisode does not star user episode`() = testScope.runTest {
        val episode: UserEpisode = mock()
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.starEpisode()

        verify(episodeManager, never()).starEpisode(any(), any(), any())
    }

    @Test
    fun `unstarEpisode unstars a podcast episode`() = testScope.runTest {
        val episode = PodcastEpisode(uuid = "ep1", publishedDate = Date(), isStarred = true)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.unstarEpisode()

        assertFalse(episode.isStarred)
        verify(episodeManager).starEpisode(episode = eq(episode), starred = eq(false), sourceView = any())
    }

    @Test
    fun `archive archives a podcast episode`() = testScope.runTest {
        val episode = PodcastEpisode(uuid = "ep1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.archive()

        assertTrue(episode.isArchived)
        verify(episodeManager).archiveBlocking(episode, playbackManager)
        verify(eventHorizon).track(any())
    }

    @Test
    fun `archive does nothing for user episode`() = testScope.runTest {
        val episode: UserEpisode = mock()
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.archive()

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `changePlaybackSpeed cycles through speeds`() = testScope.runTest {
        val podcast = Podcast(uuid = "pod1", overrideGlobalEffects = false)
        val episode = PodcastEpisode(uuid = "ep1", podcastUuid = "pod1", publishedDate = Date())
        val effects = au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects()
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(playbackManager.getPlaybackSpeed()).thenReturn(1.0)
        whenever(podcastManager.findPodcastByUuid("pod1")).thenReturn(podcast)

        val settingsProperty: au.com.shiftyjelly.pocketcasts.preferences.UserSetting<au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects> = mock()
        whenever(settingsProperty.value).thenReturn(effects)
        whenever(settings.globalPlaybackEffects).thenReturn(settingsProperty)

        actions.changePlaybackSpeed()

        assertEquals(1.2, effects.playbackSpeed, 0.01)
    }

    @Test
    fun `findPodcast returns podcast for episode`() {
        val podcast = Podcast(uuid = "pod1")
        whenever(podcastManager.findPodcastByUuidBlocking("pod1")).thenReturn(podcast)

        val episode = PodcastEpisode(uuid = "ep1", podcastUuid = "pod1", publishedDate = Date())
        val result = actions.findPodcast(episode)

        assertEquals(podcast, result)
    }
}
