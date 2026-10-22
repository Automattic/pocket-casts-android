package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CastSessionFailureTest {

    @Test
    fun `start failure only shows a toast during local playback`() {
        val action = castSessionFailureAction(
            failureType = CastManager.SessionFailureType.START,
            isCastPlayerActive = false,
        )

        assertEquals(CastSessionFailureAction.ShowToast, action)
    }

    @Test
    fun `start failure shows a toast and an error when Cast player is active`() {
        val action = castSessionFailureAction(
            failureType = CastManager.SessionFailureType.START,
            isCastPlayerActive = true,
        )

        assertEquals(CastSessionFailureAction.ShowToastAndError, action)
    }

    @Test
    fun `resume failure is ignored during local playback`() {
        val action = castSessionFailureAction(
            failureType = CastManager.SessionFailureType.RESUME,
            isCastPlayerActive = false,
        )

        assertEquals(CastSessionFailureAction.Ignore, action)
    }

    @Test
    fun `resume failure shows a toast and an error when Cast player is active`() {
        val action = castSessionFailureAction(
            failureType = CastManager.SessionFailureType.RESUME,
            isCastPlayerActive = true,
        )

        assertEquals(CastSessionFailureAction.ShowToastAndError, action)
    }
}
