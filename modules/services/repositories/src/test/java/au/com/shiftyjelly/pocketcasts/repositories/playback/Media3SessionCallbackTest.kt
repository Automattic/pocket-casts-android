package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Bundle
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Media3SessionCallbackTest {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var actions: MediaSessionActions
    private lateinit var callback: Media3SessionCallback
    private lateinit var mockSession: MediaSession
    private lateinit var mockController: MediaSession.ControllerInfo
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        playbackManager = mock()
        actions = mock()
        mockSession = mock()
        mockController = mock()
        testScope = TestScope(UnconfinedTestDispatcher())

        callback = Media3SessionCallback(
            playbackManager = playbackManager,
            episodeManager = mock(),
            settings = mock(),
            actions = actions,
            bookmarkHelper = mock(),
            scope = testScope,
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
    fun `onCustomCommand routes mark as played`() {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY),
            Bundle.EMPTY,
        )

        verify(actions).markAsPlayed()
    }

    @Test
    fun `onCustomCommand routes star`() {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_STAR, Bundle.EMPTY),
            Bundle.EMPTY,
        )

        verify(actions).starEpisode()
    }

    @Test
    fun `onCustomCommand routes unstar`() {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY),
            Bundle.EMPTY,
        )

        verify(actions).unstarEpisode()
    }

    @Test
    fun `onCustomCommand routes change speed`() {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY),
            Bundle.EMPTY,
        )

        verify(actions).changePlaybackSpeed()
    }

    @Test
    fun `onCustomCommand routes archive`() {
        callback.onCustomCommand(
            mockSession,
            mockController,
            SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY),
            Bundle.EMPTY,
        )

        verify(actions).archive()
    }

    @Test
    fun `onSetRating stars episode when heart is true`() {
        callback.onSetRating(
            mockSession,
            mockController,
            HeartRating(true),
        )

        verify(actions).starEpisode()
    }

    @Test
    fun `onSetRating unstars episode when heart is false`() {
        callback.onSetRating(
            mockSession,
            mockController,
            HeartRating(false),
        )

        verify(actions).unstarEpisode()
    }

    @Test
    fun `onAddMediaItems with search query calls performPlayFromSearch`() {
        val mediaItem = MediaItem.Builder()
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setSearchQuery("test podcast")
                    .build(),
            )
            .build()

        callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem))

        verify(actions).performPlayFromSearch("test podcast")
    }
}
