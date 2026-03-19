package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Media3SessionCallbackTest {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var podcastManager: PodcastManager
    private lateinit var settings: Settings
    private lateinit var actions: MediaSessionActions
    private lateinit var bookmarkHelper: BookmarkHelper
    private lateinit var callback: Media3SessionCallback
    private lateinit var mockSession: MediaSession
    private lateinit var mockController: MediaSession.ControllerInfo
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        playbackManager = mock()
        episodeManager = mock()
        podcastManager = mock()
        settings = mock()
        actions = mock()
        bookmarkHelper = mock()
        mockSession = mock()
        mockController = mock()
        testScope = TestScope(UnconfinedTestDispatcher())

        callback = Media3SessionCallback(
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = settings,
            actions = actions,
            bookmarkHelper = bookmarkHelper,
            scopeProvider = { testScope },
            contextProvider = { RuntimeEnvironment.getApplication() },
        )
    }

    @Test
    fun `onConnect returns accepted connection result with custom session commands`() {
        val result = callback.onConnect(mockSession, mockController)

        assertTrue(result.isAccepted)

        val sessionCommands = result.availableSessionCommands
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_STAR, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY)))
        assertTrue(sessionCommands.contains(SessionCommand(APP_ACTION_PLAY_NEXT, Bundle.EMPTY)))
    }

    @Test
    fun `onConnect returns accepted connection result with player commands`() {
        val result = callback.onConnect(mockSession, mockController)

        val playerCommands = result.availablePlayerCommands
        assertTrue(playerCommands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_FORWARD))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_BACK))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(playerCommands.contains(Player.COMMAND_STOP))
        assertTrue(playerCommands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(playerCommands.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `onCustomCommand routes mark as played`() = runTest {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        verify(actions).markAsPlayedSuspend()
    }

    @Test
    fun `onCustomCommand routes star`() = runTest {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_STAR, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        verify(actions).starEpisodeSuspend()
    }

    @Test
    fun `onCustomCommand routes unstar`() = runTest {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        verify(actions).unstarEpisodeSuspend()
    }

    @Test
    fun `onCustomCommand routes change speed`() = runTest {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        verify(actions).changePlaybackSpeedSuspend()
    }

    @Test
    fun `onCustomCommand routes archive`() = runTest {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        verify(actions).archiveSuspend()
    }

    @Test
    fun `onCustomCommand returns success after action completes`() = runTest {
        val result = callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY),
            Bundle.EMPTY,
        )
        testScope.advanceUntilIdle()

        assertEquals(SessionResult.RESULT_SUCCESS, result.get().resultCode)
    }

    @Test
    fun `onCustomCommand returns error for unknown command`() {
        val result = callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand("unknown_action", Bundle.EMPTY),
            Bundle.EMPTY,
        )

        assertEquals(SessionError.ERROR_NOT_SUPPORTED, result.get().resultCode)
    }

    @Test
    fun `onSetRating stars episode when heart is true`() = runTest {
        callback.onSetRating(
            mockSession,
            mockController,
            HeartRating(true),
        )
        testScope.advanceUntilIdle()

        verify(actions).starEpisodeSuspend()
    }

    @Test
    fun `onSetRating unstars episode when heart is false`() = runTest {
        callback.onSetRating(
            mockSession,
            mockController,
            HeartRating(false),
        )
        testScope.advanceUntilIdle()

        verify(actions).unstarEpisodeSuspend()
    }

    @Test
    fun `onSetRating returns error for non-HeartRating`() {
        val thumbRating = androidx.media3.common.ThumbRating(true)
        val result = callback.onSetRating(mockSession, mockController, thumbRating)
        assertEquals(SessionResult.RESULT_ERROR_NOT_SUPPORTED, result.get().resultCode)
    }

    @Test
    fun `onAddMediaItems with search query calls performPlayFromSearchSuspend`() = runTest {
        val mediaItem = MediaItem.Builder()
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setSearchQuery("test podcast")
                    .build(),
            )
            .build()

        callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem))
        testScope.advanceUntilIdle()

        verify(actions).performPlayFromSearchSuspend("test podcast")
    }

    @Test
    fun `onMediaButtonEvent returns false for non-media-button action`() {
        val intent = Intent("some.other.action")
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertFalse(result)
    }

    @Test
    fun `onMediaButtonEvent ignores ACTION_UP key events`() {
        val keyEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result) // returns true to consume the event
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_MEDIA_PLAY directly`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_MEDIA_NEXT as double tap`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_MEDIA_PREVIOUS as triple tap`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_MEDIA_PAUSE directly`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_MEDIA_PLAY_PAUSE as single tap`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent handles KEYCODE_HEADSETHOOK as single tap`() = runTest {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertTrue(result)
    }

    @Test
    fun `onMediaButtonEvent returns false for unrecognized keycode`() {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        val result = callback.onMediaButtonEvent(mockSession, mockController, intent)
        assertFalse(result)
    }

    // --- Headphone action handler tests ---

    @Test
    fun `KEYCODE_MEDIA_PLAY routes through multi-tap as single tap`() = runTest {
        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
        testScope.advanceUntilIdle()

        // Routed through MediaEventQueue — single tap resolves as play/pause
        verify(playbackManager).playPause(sourceView = any())
    }

    @Test
    fun `KEYCODE_MEDIA_PAUSE calls pauseSuspend`() = runTest {
        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
        testScope.advanceUntilIdle()

        verify(playbackManager).pauseSuspend(
            transientLoss = any(),
            sourceView = any(),
        )
    }

    @Test
    fun `KEYCODE_MEDIA_PLAY_PAUSE single tap calls playPause`() {
        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        testScope.advanceUntilIdle()

        verify(playbackManager).playPause(sourceView = any())
    }

    @Test
    fun `double tap with SKIP_FORWARD setting calls skipForwardSuspend`() = runTest {
        mockHeadphoneNextAction(HeadphoneAction.SKIP_FORWARD)
        mockSkipSettings()
        whenever(playbackManager.isPlaying()).thenReturn(true)

        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        testScope.advanceUntilIdle()

        verify(playbackManager).skipForwardSuspend(
            sourceView = any(),
            jumpAmountSeconds = eq(30),
        )
    }

    @Test
    fun `double tap with ADD_BOOKMARK setting calls bookmarkHelper`() = runTest {
        mockHeadphoneNextAction(HeadphoneAction.ADD_BOOKMARK)

        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        testScope.advanceUntilIdle()
        // The bookmark action dispatches on Dispatchers.Main, so idle the main looper
        shadowOf(android.os.Looper.getMainLooper()).idle()

        verify(bookmarkHelper).handleAddBookmarkAction(any(), any())
    }

    @Test
    fun `triple tap with SKIP_BACK setting calls skipBackwardSuspend`() = runTest {
        mockHeadphonePreviousAction(HeadphoneAction.SKIP_BACK)
        mockSkipSettings()

        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        testScope.advanceUntilIdle()

        verify(playbackManager).skipBackwardSuspend(
            sourceView = any(),
            jumpAmountSeconds = eq(10),
        )
    }

    // --- PiP skip button tests ---

    @Test
    fun `KEYCODE_MEDIA_SKIP_FORWARD calls skipForwardSuspend directly`() = runTest {
        mockSkipSettings()

        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)
        testScope.advanceUntilIdle()

        verify(playbackManager).skipForwardSuspend(
            sourceView = any(),
            jumpAmountSeconds = eq(30),
        )
    }

    @Test
    fun `KEYCODE_MEDIA_SKIP_BACKWARD calls skipBackwardSuspend directly`() = runTest {
        mockSkipSettings()

        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
        testScope.advanceUntilIdle()

        verify(playbackManager).skipBackwardSuspend(
            sourceView = any(),
            jumpAmountSeconds = eq(10),
        )
    }

    // --- onAddMediaItems resolved item tests ---

    @Test
    fun `onAddMediaItems with valid mediaId returns resolved MediaItem and fires playNowSuspend`() = runTest {
        val episode = PodcastEpisode(
            uuid = "ep-uuid-1",
            title = "My Episode",
            duration = 3600.0,
            publishedDate = Date(),
            podcastUuid = "pod-uuid-1",
            imageUrl = "https://example.com/episode.jpg",
        )
        val podcast = au.com.shiftyjelly.pocketcasts.models.entity.Podcast(
            uuid = "pod-uuid-1",
            title = "My Podcast",
        )
        whenever(episodeManager.findEpisodeByUuid("ep-uuid-1")).thenReturn(episode)
        whenever(podcastManager.findPodcastByUuid("pod-uuid-1")).thenReturn(podcast)

        val mediaItem = MediaItem.Builder()
            .setMediaId("pod-uuid-1#ep-uuid-1")
            .build()

        val future = callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem))
        testScope.advanceUntilIdle()

        val result = future.get()
        assertEquals(1, result.size)
        val resolved = result[0]
        assertEquals("pod-uuid-1#ep-uuid-1", resolved.mediaId)
        assertEquals("My Episode", resolved.mediaMetadata.title)
        assertEquals("My Podcast", resolved.mediaMetadata.artist)
        assertNotNull(resolved.mediaMetadata.artworkUri)
        assertTrue(resolved.mediaMetadata.isPlayable!!)

        verify(playbackManager).playNowSuspend(
            episode = any(),
            forceStream = any(),
            showedStreamWarning = any(),
            sourceView = any(),
        )
    }

    @Test
    fun `onAddMediaItems with unknown episodeId returns empty list`() = runTest {
        whenever(episodeManager.findEpisodeByUuid("unknown-uuid")).thenReturn(null)

        val mediaItem = MediaItem.Builder()
            .setMediaId("pod#unknown-uuid")
            .build()

        val future = callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem))
        testScope.advanceUntilIdle()

        val result = future.get()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `onAddMediaItems with UserEpisode returns item without podcast metadata`() = runTest {
        val episode = UserEpisode(
            uuid = "user-ep-1",
            title = "My Upload",
            publishedDate = Date(),
            artworkUrl = "https://example.com/user-art.jpg",
        )
        whenever(episodeManager.findEpisodeByUuid("user-ep-1")).thenReturn(episode)

        val mediaItem = MediaItem.Builder()
            .setMediaId("user#user-ep-1")
            .build()

        val future = callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem))
        testScope.advanceUntilIdle()

        val result = future.get()
        assertEquals(1, result.size)
        val resolved = result[0]
        assertEquals("My Upload", resolved.mediaMetadata.title)
        assertTrue(resolved.mediaMetadata.isPlayable!!)

        verify(playbackManager).playNowSuspend(
            episode = any(),
            forceStream = any(),
            showedStreamWarning = any(),
            sourceView = any(),
        )
    }

    // --- Helpers ---

    private fun sendMediaButtonEvent(keyCode: Int) {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        callback.onMediaButtonEvent(mockSession, mockController, intent)
    }

    private fun mockHeadphoneNextAction(action: HeadphoneAction) {
        val setting = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<HeadphoneAction>>()
        whenever(setting.value).thenReturn(action)
        whenever(settings.headphoneControlsNextAction).thenReturn(setting)
    }

    private fun mockHeadphonePreviousAction(action: HeadphoneAction) {
        val setting = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<HeadphoneAction>>()
        whenever(setting.value).thenReturn(action)
        whenever(settings.headphoneControlsPreviousAction).thenReturn(setting)
    }

    private fun mockSkipSettings() {
        val skipForward = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<Int>>()
        whenever(skipForward.value).thenReturn(30)
        whenever(settings.skipForwardInSecs).thenReturn(skipForward)

        val skipBack = mock<au.com.shiftyjelly.pocketcasts.preferences.UserSetting<Int>>()
        whenever(skipBack.value).thenReturn(10)
        whenever(settings.skipBackInSecs).thenReturn(skipBack)
    }
}
