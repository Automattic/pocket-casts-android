package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

import org.junit.Assert.assertEquals
import org.junit.Test

class LfmTokenSpanTest {
    @Test
    fun lastMatchOfUserTokensIsPreferred() {
        val prompt = intArrayOf(1, 2, 10, 11, 3, 10, 11, 4)
        val user = intArrayOf(10, 11)
        assertEquals(5 to 6, LfmTokenSpan.lastUserTokenSpan(prompt, user))
    }

    @Test
    fun missingContiguousMatch_fallsBackToTrailingWindow() {
        val prompt = IntArray(80) { it }
        val user = intArrayOf(900, 901) // not present
        val (start, end) = LfmTokenSpan.lastUserTokenSpan(prompt, user)
        // window = max(2, 32) = 32
        assertEquals(48, start)
        assertEquals(79, end)
    }
}
