package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.Trackable
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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

    private lateinit var playbackManager: PlaybackManager
    private lateinit var podcastManager: PodcastManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var playlistManager: PlaylistManager
    private lateinit var settings: Settings
    private lateinit var eventHorizon: EventHorizon
    private lateinit var testScope: TestScope
    private var searchFailedMessage: String? = null
    private lateinit var actions: MediaSessionActions

    @Before
    fun setUp() {
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        playbackManager = mock()
        podcastManager = mock()
        episodeManager = mock()
        playlistManager = mock()
        settings = mock()
        eventHorizon = mock()
        searchFailedMessage = null
        testScope = TestScope(testDispatcher)

        actions = MediaSessionActions(
            playbackManager = playbackManager,
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            playlistManager = playlistManager,
            settings = settings,
            eventHorizon = eventHorizon,
            scopeProvider = { testScope },
            onSearchFailed = { searchFailedMessage = it },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- performPlayFromSearch ---

    @Test
    fun `performPlayFromSearch with null query does nothing`() {
        actions.performPlayFromSearch(null)

        verifyNoInteractions(playbackManager)
    }

    @Test
    fun `performPlayFromSearch with up next plays queue`() = runTest {
        actions.performPlayFromSearch("up next")

        verify(playbackManager).playQueue(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION)
    }

    @Test
    fun `performPlayFromSearch with Up Next prefix plays queue`() = runTest {
        actions.performPlayFromSearch("Up Next please")

        verify(playbackManager).playQueue(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION)
    }

    @Test
    fun `performPlayFromSearch matches podcast by title`() = runTest {
        val podcast = Podcast(uuid = "pod-1", title = "Tech News")
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking("tech news")).thenReturn(podcast)
        whenever(episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)).thenReturn(episode)

        actions.performPlayFromSearch("Tech News")
        // playPodcast uses withContext(Dispatchers.Default) which runs on a real thread pool
        Thread.sleep(100)

        verify(playbackManager).playNow(
            episode = eq(episode),
            forceStream = eq(false),
            showedStreamWarning = eq(false),
            sourceView = eq(SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION),
        )
    }

    @Test
    fun `performPlayFromSearch matches episode by title when podcast not found`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking(any())).thenReturn(null)
        whenever(episodeManager.findFirstBySearchQuery("my episode")).thenReturn(episode)

        actions.performPlayFromSearch("My Episode")

        verify(playbackManager).playNow(
            episode = eq(episode),
            forceStream = eq(false),
            showedStreamWarning = eq(false),
            sourceView = eq(SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION),
        )
    }

    @Test
    fun `performPlayFromSearch calls onSearchFailed when no results`() = runTest {
        whenever(podcastManager.searchPodcastByTitleBlocking(any())).thenReturn(null)
        whenever(episodeManager.findFirstBySearchQuery(any())).thenReturn(null)
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(emptyList()))

        actions.performPlayFromSearch("nonexistent query")

        assertEquals("No search results", searchFailedMessage)
    }

    @Test
    fun `performPlayFromSearch does not call onSearchFailed when podcast found`() = runTest {
        val podcast = Podcast(uuid = "pod-1", title = "Found")
        val episode = createPodcastEpisode("ep-1")
        whenever(podcastManager.searchPodcastByTitleBlocking("found")).thenReturn(podcast)
        whenever(episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)).thenReturn(episode)

        actions.performPlayFromSearch("Found")

        assertNull(searchFailedMessage)
    }

    // --- changePlaybackSpeed ---

    @Test
    fun `changePlaybackSpeed cycles from 1x to 1_2x`() = runTest {
        whenever(playbackManager.getPlaybackSpeed()).thenReturn(1.0)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(createPodcastEpisode("ep-1"))
        whenever(podcastManager.findPodcastByUuid(any())).thenReturn(null)
        val effects = mock<au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects>()
        val globalEffects =
            mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects>>()
        whenever(globalEffects.value).thenReturn(effects)
        whenever(settings.globalPlaybackEffects).thenReturn(globalEffects)

        actions.changePlaybackSpeed()

        verify(effects).playbackSpeed = 1.2
    }

    @Test
    fun `changePlaybackSpeed cycles from 3x back to 0_6x`() = runTest {
        whenever(playbackManager.getPlaybackSpeed()).thenReturn(3.0)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(createPodcastEpisode("ep-1"))
        whenever(podcastManager.findPodcastByUuid(any())).thenReturn(null)
        val effects = mock<au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects>()
        val globalEffects =
            mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects>>()
        whenever(globalEffects.value).thenReturn(effects)
        whenever(settings.globalPlaybackEffects).thenReturn(globalEffects)

        actions.changePlaybackSpeed()

        verify(effects).playbackSpeed = 0.6
    }

    // --- markAsPlayed ---

    @Test
    fun `markAsPlayed calls episodeManager and tracks analytics`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.markAsPlayed()

        verify(episodeManager).markAsPlayedBlocking(eq(episode), eq(playbackManager), eq(podcastManager), eq(false))
        verify(eventHorizon).track(
            argThat<Trackable> { event ->
                event is EpisodeMarkedAsPlayedEvent && event.episodeUuid == episode.uuid
            },
        )
    }

    @Test
    fun `markAsPlayed with null episode does not track analytics`() = runTest {
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)

        actions.markAsPlayed()

        verify(episodeManager).markAsPlayedBlocking(eq(null), eq(playbackManager), eq(podcastManager), eq(false))
        verify(eventHorizon, never()).track(any())
    }

    // --- starEpisode ---

    @Test
    fun `starEpisode sets isStarred and calls episodeManager`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.starEpisode()

        assertTrue(episode.isStarred)
        verify(episodeManager).starEpisode(
            episode = eq(episode),
            starred = eq(true),
            sourceView = eq(SourceView.MEDIA_BUTTON_BROADCAST_ACTION),
        )
    }

    @Test
    fun `starEpisode ignores UserEpisode`() = runTest {
        val userEpisode = UserEpisode(uuid = "ue-1", publishedDate = Date(), title = "User Ep")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(userEpisode)

        actions.starEpisode()

        verify(episodeManager, never()).starEpisode(any(), any(), any())
    }

    // --- unstarEpisode ---

    @Test
    fun `unstarEpisode sets isStarred false and calls episodeManager`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        episode.isStarred = true
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.unstarEpisode()

        assertFalse(episode.isStarred)
        verify(episodeManager).starEpisode(
            episode = eq(episode),
            starred = eq(false),
            sourceView = eq(SourceView.MEDIA_BUTTON_BROADCAST_ACTION),
        )
    }

    // --- archive ---

    @Test
    fun `archive sets isArchived and tracks analytics`() = runTest {
        val episode = createPodcastEpisode("ep-1")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        actions.archive()

        assertTrue(episode.isArchived)
        verify(episodeManager).archiveBlocking(eq(episode), eq(playbackManager), eq(true), eq(false))
        verify(eventHorizon).track(
            argThat<Trackable> { event ->
                event is EpisodeArchivedEvent && event.episodeUuid == episode.uuid
            },
        )
    }

    @Test
    fun `archive ignores UserEpisode`() = runTest {
        val userEpisode = UserEpisode(uuid = "ue-1", publishedDate = Date(), title = "User Ep")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(userEpisode)

        actions.archive()

        verify(episodeManager, never()).archiveBlocking(any(), any(), any(), any())
        verify(eventHorizon, never()).track(any())
    }

    private fun createPodcastEpisode(uuid: String): PodcastEpisode {
        return PodcastEpisode(
            uuid = uuid,
            title = "Test Episode",
            publishedDate = Date(),
            podcastUuid = "podcast-uuid",
        )
    }
}
