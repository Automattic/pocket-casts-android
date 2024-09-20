package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PlaybackEffectsDataTest {

    @Test
    fun `data to effects and back to data is equal`() {
        val start = PlaybackEffectsData(
            playbackSpeed = 1.5,
            trimMode = TrimMode.MEDIUM,
            isVolumeBoosted = true,
        )
        val end = start.toEffects().toData()
        assertEquals(start, end)
    }
}
