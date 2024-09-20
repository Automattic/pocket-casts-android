package au.com.shiftyjelly.pocketcasts.models.type

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EpisodePlayingStatusTest {

    @Test
    fun `to-from int round trip`() {
        EpisodePlayingStatus.values().forEach {
            assertEquals(it, EpisodePlayingStatus.fromInt(it.toInt()))
        }
    }
}
