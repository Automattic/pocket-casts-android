package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastSessionFailureTest {

    @Test
    fun `start failure is surfaced during local playback`() {
        val shouldSurface = shouldSurfaceCastSessionFailure(
            failureType = CastManager.SessionFailureType.START,
            isCastPlayerActive = false,
        )

        assertTrue(shouldSurface)
    }

    @Test
    fun `resume failure is ignored during local playback`() {
        val shouldSurface = shouldSurfaceCastSessionFailure(
            failureType = CastManager.SessionFailureType.RESUME,
            isCastPlayerActive = false,
        )

        assertFalse(shouldSurface)
    }

    @Test
    fun `resume failure is surfaced when Cast player is active`() {
        val shouldSurface = shouldSurfaceCastSessionFailure(
            failureType = CastManager.SessionFailureType.RESUME,
            isCastPlayerActive = true,
        )

        assertTrue(shouldSurface)
    }
}
