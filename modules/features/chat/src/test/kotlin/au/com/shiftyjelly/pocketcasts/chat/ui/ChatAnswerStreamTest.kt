package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAnswerStreamTest {
    @Test
    fun `an answer counts the same ticks its bubble reveals`() {
        val answer = ChatMessage.Assistant(text = "Intro line\r\n\n- first point\n* second  point\n\nOutro")

        val bubbleText = answer.displayText.withHangingBullets(10.sp)

        assertEquals(TextRevealTimeline(bubbleText).ticks, answer.revealTickCount())
    }

    @Test
    fun `a quote counts its timestamp`() {
        val quote = ChatMessage.Quote(text = "\"Keep it simple\"", start = "12:04", end = "12:18")

        assertEquals(quoteText(quote.displayText, quote.timestampLabel).length, quote.revealTickCount())
        assertEquals(quote.displayText.length + 1 + quoteTimestampText(quote.timestampLabel).length, quote.revealTickCount())
    }

    @Test
    fun `a quote without a timestamp counts only its text`() {
        val quote = ChatMessage.Quote(text = "Keep it simple", start = "", end = "")

        assertEquals(quote.displayText.length, quote.revealTickCount())
    }

    @Test
    fun `each message of an answer starts after the previous one and a pause`() {
        val offsets = answerTickOffsets(listOf(10, 20, 5))

        assertEquals(listOf(0, 10 + MESSAGE_PAUSE_TICKS, 30 + 2 * MESSAGE_PAUSE_TICKS), offsets.take(3))
        assertEquals(35 + 2 * MESSAGE_PAUSE_TICKS, answerTotalTicks(listOf(10, 20, 5)))
    }

    @Test
    fun `an empty answer has no ticks`() {
        assertEquals(0, answerTotalTicks(emptyList()))
    }
}
