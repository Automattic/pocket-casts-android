package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import java.util.Date
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Media3LibrarySessionCallbackTest {

    private lateinit var sessionCallback: Media3SessionCallback
    private lateinit var browseTreeProvider: BrowseTreeProvider
    private lateinit var playbackManager: PlaybackManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var podcastManager: PodcastManager
    private lateinit var mockSettings: Settings
    private lateinit var callback: Media3LibrarySessionCallback
    private lateinit var mockSession: MediaLibraryService.MediaLibrarySession
    private lateinit var mockController: MediaSession.ControllerInfo
    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        sessionCallback = mock()
        browseTreeProvider = mock()
        playbackManager = mock()
        episodeManager = mock()
        podcastManager = mock()
        mockSettings = mock()
        mockSession = mock()
        mockController = mock()
        whenever(mockController.packageName).thenReturn("au.com.shiftyjelly.pocketcasts.debug")
        whenever(mockController.uid).thenReturn(1000)
        mockContext = mock()
        val mockPackageManager: PackageManager = mock()
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(ApplicationInfo())
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn("au.com.shiftyjelly.pocketcasts.debug")
        testScope = TestScope(UnconfinedTestDispatcher())

        callback = Media3LibrarySessionCallback(
            sessionCallback = sessionCallback,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = mockSettings,
            packageValidator = null,
            scopeProvider = { testScope },
            contextProvider = { mockContext },
        )
    }

    @Test
    fun `onGetLibraryRoot returns root media item for default params`() {
        val episode = PodcastEpisode(uuid = "ep-1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(browseTreeProvider.getRootId(isRecent = false, isSuggested = false, hasCurrentEpisode = true))
            .thenReturn(MEDIA_ID_ROOT)

        val result = callback.onGetLibraryRoot(mockSession, mockController, null)

        val libraryResult = result.get()
        assertEquals(MEDIA_ID_ROOT, libraryResult.value?.mediaId)
    }

    @Test
    fun `onGetLibraryRoot returns suggested root when params isSuggested`() {
        val episode = PodcastEpisode(uuid = "ep-1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(browseTreeProvider.getRootId(isRecent = false, isSuggested = true, hasCurrentEpisode = true))
            .thenReturn(SUGGESTED_ROOT)

        val params = MediaLibraryService.LibraryParams.Builder()
            .setSuggested(true)
            .build()

        val result = callback.onGetLibraryRoot(mockSession, mockController, params)

        val libraryResult = result.get()
        assertEquals(SUGGESTED_ROOT, libraryResult.value?.mediaId)
    }

    @Test
    fun `onGetLibraryRoot returns recent root when params isRecent`() {
        val episode = PodcastEpisode(uuid = "ep-1", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(browseTreeProvider.getRootId(isRecent = true, isSuggested = false, hasCurrentEpisode = true))
            .thenReturn(RECENT_ROOT)

        val params = MediaLibraryService.LibraryParams.Builder()
            .setRecent(true)
            .build()

        val result = callback.onGetLibraryRoot(mockSession, mockController, params)

        val libraryResult = result.get()
        assertEquals(RECENT_ROOT, libraryResult.value?.mediaId)
    }

    @Test
    fun `onGetLibraryRoot passes hasCurrentEpisode false when no current episode`() {
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)
        whenever(browseTreeProvider.getRootId(isRecent = false, isSuggested = false, hasCurrentEpisode = false))
            .thenReturn(MEDIA_ID_ROOT)

        val result = callback.onGetLibraryRoot(mockSession, mockController, null)

        val libraryResult = result.get()
        assertEquals(MEDIA_ID_ROOT, libraryResult.value?.mediaId)
        verify(browseTreeProvider).getRootId(isRecent = false, isSuggested = false, hasCurrentEpisode = false)
    }

    @Test
    fun `onGetChildren returns browse items from provider`() = runTest {
        val items = listOf(
            createMediaItem("id1", "Title 1", browsable = true),
            createMediaItem("id2", "Title 2", playable = true),
        )
        whenever(browseTreeProvider.loadChildren(eq(MEDIA_ID_ROOT), any())).thenReturn(items)

        val result = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        val resultItems = libraryResult.value!!
        assertEquals(2, resultItems.size)
        assertEquals("id1", resultItems[0].mediaId)
        assertEquals("id2", resultItems[1].mediaId)
    }

    @Test
    fun `onGetChildren paginates correctly`() = runTest {
        val items = (1..10).map { createMediaItem("id$it", "Title $it", browsable = true) }
        whenever(browseTreeProvider.loadChildren(eq(MEDIA_ID_ROOT), any())).thenReturn(items)

        val page0 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 0, 3, null).get()
        assertEquals(3, page0.value!!.size)
        assertEquals("id1", page0.value!![0].mediaId)

        val page1 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 1, 3, null).get()
        assertEquals(3, page1.value!!.size)
        assertEquals("id4", page1.value!![0].mediaId)

        val page3 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 3, 3, null).get()
        assertEquals(1, page3.value!!.size)
        assertEquals("id10", page3.value!![0].mediaId)
    }

    @Test
    fun `onGetSearchResult returns results from provider`() = runTest {
        val items = listOf(
            createMediaItem("podcast1", "My Podcast", browsable = true),
        )
        whenever(browseTreeProvider.search(eq("test"), any())).thenReturn(items)

        val result = callback.onGetSearchResult(mockSession, mockController, "test", 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        assertEquals(1, libraryResult.value!!.size)
        assertEquals("podcast1", libraryResult.value!![0].mediaId)
    }

    @Test
    fun `onGetSearchResult returns error when search returns null`() = runTest {
        whenever(browseTreeProvider.search(eq("fail"), any())).thenReturn(null)

        val result = callback.onGetSearchResult(mockSession, mockController, "fail", 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        assertEquals(androidx.media3.session.SessionError.ERROR_UNKNOWN, libraryResult.resultCode)
    }

    @Test
    fun `onConnect delegates to sessionCallback`() {
        val connectionResult = MediaSession.ConnectionResult.accept(
            androidx.media3.session.SessionCommands.Builder().build(),
            androidx.media3.common.Player.Commands.Builder().build(),
        )
        whenever(sessionCallback.onConnect(any(), any())).thenReturn(connectionResult)

        val result = callback.onConnect(mockSession, mockController)

        verify(sessionCallback).onConnect(mockSession, mockController)
        assertTrue(result.isAccepted)
    }

    @Test
    fun `onConnect accepts unknown caller with transport-only commands`() {
        val packageValidator: PackageValidator = mock()
        whenever(mockController.packageName).thenReturn("com.unknown.app")
        whenever(mockController.uid).thenReturn(12345)
        whenever(packageValidator.isKnownCaller("com.unknown.app", 12345)).thenReturn(false)

        val callbackWithValidator = Media3LibrarySessionCallback(
            sessionCallback = sessionCallback,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = mockSettings,
            packageValidator = packageValidator,
            scopeProvider = { testScope },
            contextProvider = { mockContext },
        )

        val result = callbackWithValidator.onConnect(mockSession, mockController)

        assertTrue(result.isAccepted)
        // Unknown callers get transport controls but no session commands
        assertEquals(SessionCommands.EMPTY, result.availableSessionCommands)
        val playerCommands = result.availablePlayerCommands
        assertTrue(playerCommands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(playerCommands.contains(Player.COMMAND_STOP))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        // Should NOT delegate to sessionCallback (which adds full session commands)
        verify(sessionCallback, never()).onConnect(any(), any())
    }

    @Test
    fun `onConnect accepts known caller when packageValidator accepts`() {
        val packageValidator: PackageValidator = mock()
        whenever(mockController.packageName).thenReturn("au.com.shiftyjelly.pocketcasts.known")
        whenever(mockController.uid).thenReturn(12345)
        whenever(packageValidator.isKnownCaller("au.com.shiftyjelly.pocketcasts.known", 12345)).thenReturn(true)

        val connectionResult = MediaSession.ConnectionResult.accept(
            androidx.media3.session.SessionCommands.Builder().build(),
            androidx.media3.common.Player.Commands.Builder().build(),
        )
        whenever(sessionCallback.onConnect(any(), any())).thenReturn(connectionResult)

        val callbackWithValidator = Media3LibrarySessionCallback(
            sessionCallback = sessionCallback,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = mockSettings,
            packageValidator = packageValidator,
            scopeProvider = { testScope },
            contextProvider = { mockContext },
        )

        callbackWithValidator.onConnect(mockSession, mockController)

        verify(sessionCallback).onConnect(mockSession, mockController)
    }

    @Test
    fun `onConnect accepts any caller when packageValidator is null`() {
        val connectionResult = MediaSession.ConnectionResult.accept(
            androidx.media3.session.SessionCommands.Builder().build(),
            androidx.media3.common.Player.Commands.Builder().build(),
        )
        whenever(sessionCallback.onConnect(any(), any())).thenReturn(connectionResult)

        callback.onConnect(mockSession, mockController)

        verify(sessionCallback).onConnect(mockSession, mockController)
    }

    @Test
    fun `onConnect delegates for external client`() {
        whenever(mockController.packageName).thenReturn("com.external.app")
        whenever(mockController.uid).thenReturn(99)
        whenever(mockSettings.automotiveConnectedToMediaSession()).thenReturn(false)
        val connectionResult = MediaSession.ConnectionResult.accept(
            androidx.media3.session.SessionCommands.Builder().build(),
            androidx.media3.common.Player.Commands.Builder().build(),
        )
        whenever(sessionCallback.onConnect(any(), any())).thenReturn(connectionResult)

        callback.onConnect(mockSession, mockController)

        verify(sessionCallback).onConnect(mockSession, mockController)
    }

    @Test
    fun `onCustomCommand delegates to sessionCallback`() {
        val command = SessionCommand(APP_ACTION_STAR, Bundle.EMPTY)

        callback.onCustomCommand(mockSession, mockController, command, Bundle.EMPTY)

        verify(sessionCallback).onCustomCommand(mockSession, mockController, command, Bundle.EMPTY)
    }

    @Test
    fun `onPlaybackResumption returns current episode with position and metadata`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid", title = "My Podcast")
        val episode = PodcastEpisode(
            uuid = "resume-ep",
            title = "Resume Episode",
            publishedDate = Date(),
            podcastUuid = "podcast-uuid",
        ).apply { playedUpTo = 42.5 }
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(podcastManager.findPodcastByUuid("podcast-uuid")).thenReturn(podcast)

        val mockMediaSession: MediaSession = mock()
        val result = callback.onPlaybackResumption(mockMediaSession, mockController, false)

        val itemsWithPosition = result.get()
        assertEquals(1, itemsWithPosition.mediaItems.size)
        val mediaItem = itemsWithPosition.mediaItems[0]
        assertEquals("resume-ep", mediaItem.mediaId)
        assertEquals("Resume Episode", mediaItem.mediaMetadata.title)
        assertEquals("My Podcast", mediaItem.mediaMetadata.artist)
        assertEquals(0, itemsWithPosition.startIndex)
        assertEquals(42_500L, itemsWithPosition.startPositionMs)
    }

    @Test
    fun `onPlaybackResumption fails when no current episode`() = runTest {
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)

        val mockMediaSession: MediaSession = mock()
        val result = callback.onPlaybackResumption(mockMediaSession, mockController, false)

        val exception = assertThrows(ExecutionException::class.java) { result.get() }
        assertTrue(exception.cause is UnsupportedOperationException)
    }

    @Test
    fun `onPlaybackResumption sets automotive connected flag on automotive`() = runTest {
        val automotiveContext: Context = mock()
        val automotivePackageManager: PackageManager = mock()
        val metaData = Bundle().apply { putBoolean("pocketcasts_automotive", true) }
        val appInfo = ApplicationInfo().apply { this.metaData = metaData }
        whenever(automotivePackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)
        whenever(automotiveContext.packageManager).thenReturn(automotivePackageManager)
        whenever(automotiveContext.packageName).thenReturn("au.com.shiftyjelly.pocketcasts.debug")

        val automotiveCallback = Media3LibrarySessionCallback(
            sessionCallback = sessionCallback,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = mockSettings,
            packageValidator = null,
            scopeProvider = { testScope },
            contextProvider = { automotiveContext },
        )

        val episode = PodcastEpisode(
            uuid = "ep-1",
            title = "Episode",
            publishedDate = Date(),
            podcastUuid = "pod-1",
        )
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        whenever(podcastManager.findPodcastByUuid("pod-1")).thenReturn(null)

        val mockMediaSession: MediaSession = mock()
        automotiveCallback.onPlaybackResumption(mockMediaSession, mockController, false)

        verify(mockSettings).setAutomotiveConnectedToMediaSession(true)
    }

    @Test
    fun `onSearch notifies with actual result count`() = runTest {
        val items = listOf(
            createMediaItem("podcast1", "My Podcast", browsable = true),
            createMediaItem("podcast2", "Other Podcast", browsable = true),
        )
        whenever(browseTreeProvider.search(eq("test"), any())).thenReturn(items)

        callback.onSearch(mockSession, mockController, "test", null)

        verify(mockSession).notifySearchResultChanged(eq(mockController), eq("test"), eq(2), anyOrNull())
    }

    @Test
    fun `onSearch notifies with zero when search returns null`() = runTest {
        whenever(browseTreeProvider.search(eq("empty"), any())).thenReturn(null)

        callback.onSearch(mockSession, mockController, "empty", null)

        verify(mockSession).notifySearchResultChanged(eq(mockController), eq("empty"), eq(0), anyOrNull())
    }

    private fun createMediaItem(
        mediaId: String,
        title: String,
        browsable: Boolean = false,
        playable: Boolean = false,
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(browsable)
            .setIsPlayable(playable)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }
}
