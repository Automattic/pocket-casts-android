package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Looper
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SeedStatePlayerTest {

    private lateinit var player: SeedStatePlayer

    @Before
    fun setUp() {
        player = SeedStatePlayer(applicationLooper = Looper.getMainLooper())
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `initial state is idle before seed`() {
        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }

    @Test
    fun `seed transitions to ready with one item`() {
        player.seed(positionMs = 5000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(Player.STATE_READY, player.playbackState)
        assertEquals(1, player.mediaItemCount)
    }

    @Test
    fun `seed reports correct position`() {
        player.seed(positionMs = 42_000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(42_000L, player.currentPosition)
    }

    @Test
    fun `available commands after seed include play pause seek and stop`() {
        player.seed(positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        val commands = player.availableCommands
        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_STOP))
        assertTrue(commands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `playWhenReady is false after seed`() {
        player.seed(positionMs = 1000L)
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `play does not throw`() {
        player.seed(positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.play()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `pause does not throw`() {
        player.seed(positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.pause()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `seekTo does not throw`() {
        player.seed(positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.seekTo(10_000L)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `stop does not throw`() {
        player.seed(positionMs = 0L)
        shadowOf(Looper.getMainLooper()).idle()

        player.stop()
        shadowOf(Looper.getMainLooper()).idle()
    }
}
