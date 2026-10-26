package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerBroadcastReceiver.Companion.INTENT_ACTION_SKIP_FORWARD
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerBroadcastReceiver.Companion.INTENT_EXTRA_SECONDS
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class PlayerBroadcastReceiverTest {

    @Test
    fun `returns null when the extra is missing`() {
        assertNull(skipIntent().skipAmountSecondsOrNull())
    }

    @Test
    fun `reads an int extra`() {
        assertEquals(45, skipIntent().putExtra(INTENT_EXTRA_SECONDS, 45).skipAmountSecondsOrNull())
    }

    @Test
    fun `reads a string extra`() {
        assertEquals(45, skipIntent().putExtra(INTENT_EXTRA_SECONDS, "45").skipAmountSecondsOrNull())
    }

    @Test
    fun `returns null for a non numeric string extra`() {
        assertNull(skipIntent().putExtra(INTENT_EXTRA_SECONDS, "abc").skipAmountSecondsOrNull())
    }

    @Test
    fun `returns null for zero`() {
        assertNull(skipIntent().putExtra(INTENT_EXTRA_SECONDS, 0).skipAmountSecondsOrNull())
    }

    @Test
    fun `returns null for a negative amount`() {
        assertNull(skipIntent().putExtra(INTENT_EXTRA_SECONDS, -30).skipAmountSecondsOrNull())
    }

    @Test
    fun `caps an amount above an hour`() {
        assertEquals(3_600, skipIntent().putExtra(INTENT_EXTRA_SECONDS, Int.MAX_VALUE).skipAmountSecondsOrNull())
    }

    @Test
    fun `keeps an amount at the cap`() {
        assertEquals(3_600, skipIntent().putExtra(INTENT_EXTRA_SECONDS, 3_600).skipAmountSecondsOrNull())
    }

    @Test
    fun `notification dismissal while the service is in the foreground does not pause`() {
        val playbackManager = playbackManager(isServiceForeground = true)

        playbackManager.onPlaybackNotificationDismissed(SourceView.PLAYER_BROADCAST_ACTION)

        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `notification dismissal while the service is not in the foreground pauses`() {
        val playbackManager = playbackManager(isServiceForeground = false)

        playbackManager.onPlaybackNotificationDismissed(SourceView.PLAYER_BROADCAST_ACTION)

        verify(playbackManager).pause(sourceView = SourceView.PLAYER_BROADCAST_ACTION)
    }

    private fun playbackManager(isServiceForeground: Boolean): PlaybackManager {
        val mediaSessionManager = mock<MediaSessionManager> {
            on { this.isServiceForeground } doReturn MutableStateFlow(isServiceForeground)
        }
        return mock {
            on { this.mediaSessionManager } doReturn mediaSessionManager
        }
    }

    private fun skipIntent() = Intent(INTENT_ACTION_SKIP_FORWARD)
}
