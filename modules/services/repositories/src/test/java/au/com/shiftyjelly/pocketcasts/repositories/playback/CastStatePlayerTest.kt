package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Looper
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CastStatePlayerTest {

    private lateinit var onPlay: () -> Unit
    private lateinit var onPause: () -> Unit
    private lateinit var onSeekTo: (Long) -> Unit
    private lateinit var onStop: () -> Unit
    private lateinit var player: CastStatePlayer

    @Before
    fun setUp() {
        onPlay = mock()
        onPause = mock()
        onSeekTo = mock()
        onStop = mock()
        player = CastStatePlayer(
            applicationLooper = Looper.getMainLooper(),
            onPlay = onPlay,
            onPause = onPause,
            onSeekTo = onSeekTo,
            onStop = onStop,
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `initial state is idle before first update`() {
        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    @Test
    fun `updateCastState with playing true sets playWhenReady`() {
        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 5000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)
        assertEquals(5000L, player.currentPosition)
    }

    @Test
    fun `updateCastState with buffering sets state to buffering`() {
        player.updateCastState(isPlaying = false, isBuffering = true, positionMs = 1000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_BUFFERING, player.playbackState)
        assertEquals(1000L, player.currentPosition)
    }

    @Test
    fun `updateCastState with paused sets playWhenReady false`() {
        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 3000L)
        shadowOf(Looper.getMainLooper()).idle()

        player.updateCastState(isPlaying = false, isBuffering = false, positionMs = 3000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)
    }

    @Test
    fun `state transitions playing to buffering to paused`() {
        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 1000L)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)

        player.updateCastState(isPlaying = false, isBuffering = true, positionMs = 2000L)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_BUFFERING, player.playbackState)

        player.updateCastState(isPlaying = false, isBuffering = false, positionMs = 2000L)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)
    }

    @Test
    fun `play delegates to onPlay callback`() {
        player.updateCastState(isPlaying = false, isBuffering = false, positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.play()
        shadowOf(Looper.getMainLooper()).idle()

        verify(onPlay).invoke()
    }

    @Test
    fun `pause delegates to onPause callback`() {
        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.pause()
        shadowOf(Looper.getMainLooper()).idle()

        verify(onPause).invoke()
    }

    @Test
    fun `seekTo delegates to onSeekTo callback`() {
        player.updateCastState(isPlaying = false, isBuffering = false, positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.seekTo(42_000L)
        shadowOf(Looper.getMainLooper()).idle()

        verify(onSeekTo).invoke(42_000L)
    }

    @Test
    fun `stop delegates to onStop callback`() {
        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.stop()
        shadowOf(Looper.getMainLooper()).idle()

        verify(onStop).invoke()
    }

    @Test
    fun `available commands after update include play pause seek and stop`() {
        player.updateCastState(isPlaying = false, isBuffering = false, positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        val commands = player.availableCommands
        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_STOP))
        assertTrue(commands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `state changes are reflected after updateCastState`() {
        assertFalse(player.playWhenReady)

        player.updateCastState(isPlaying = true, isBuffering = false, positionMs = 10_000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)
        assertEquals(10_000L, player.currentPosition)
    }
}
