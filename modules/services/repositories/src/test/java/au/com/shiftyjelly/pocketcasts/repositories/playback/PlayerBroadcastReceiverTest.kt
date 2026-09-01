package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerBroadcastReceiver.Companion.INTENT_ACTION_SKIP_FORWARD
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerBroadcastReceiver.Companion.INTENT_EXTRA_SECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
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
    fun `caps an amount that would overflow the millisecond conversion`() {
        assertEquals(86_400, skipIntent().putExtra(INTENT_EXTRA_SECONDS, Int.MAX_VALUE).skipAmountSecondsOrNull())
    }

    private fun skipIntent() = Intent(INTENT_ACTION_SKIP_FORWARD)
}
