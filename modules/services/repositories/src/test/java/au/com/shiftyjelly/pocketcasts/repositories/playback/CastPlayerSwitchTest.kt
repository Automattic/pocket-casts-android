package au.com.shiftyjelly.pocketcasts.repositories.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastPlayerSwitchTest {

    @Test
    fun `switches a local player to Cast when connected`() {
        assertTrue(castPlayerSwitchRequired(isCastConnected = true, isCastPlayer = false, isBoundToCurrentSession = false))
    }

    @Test
    fun `keeps a Cast player bound to the current session`() {
        assertFalse(castPlayerSwitchRequired(isCastConnected = true, isCastPlayer = true, isBoundToCurrentSession = true))
    }

    @Test
    fun `replaces a Cast player from a previous session`() {
        assertTrue(castPlayerSwitchRequired(isCastConnected = true, isCastPlayer = true, isBoundToCurrentSession = false))
    }

    @Test
    fun `switches a Cast player to local when disconnected`() {
        assertTrue(castPlayerSwitchRequired(isCastConnected = false, isCastPlayer = true, isBoundToCurrentSession = false))
    }

    @Test
    fun `keeps a local player when disconnected`() {
        assertFalse(castPlayerSwitchRequired(isCastConnected = false, isCastPlayer = false, isBoundToCurrentSession = false))
    }
}
