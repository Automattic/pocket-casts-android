package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

class EarconIdTest {
    @Test
    fun `all six earcon IDs are defined`() {
        assertEquals(6, EarconId.entries.size)
    }

    @Test
    fun `each earcon ID has a distinct ordinal`() {
        val ordinals = EarconId.entries.map { it.ordinal }.toSet()
        assertEquals(EarconId.entries.size, ordinals.size)
    }
}
