package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAnswerStreamTest {
    @Test
    fun `an answer counts the same words its bubble reveals`() {
        val answer = ChatMessage.Assistant(text = "Intro line\r\n\n- first point\n* second  point\n\nOutro")

        val (_, bubbleWords) = answer.displayText.withHangingBullets(10.sp)

        assertEquals(bubbleWords.size, answer.revealWordCount())
        assertEquals(9, answer.revealWordCount())
    }

    @Test
    fun `a quote counts its timestamp words`() {
        val quote = ChatMessage.Quote(text = "\"Keep it simple\"", start = "12:04", end = "12:18")

        assertEquals(6, quote.revealWordCount())
    }

    @Test
    fun `a quote without a timestamp counts only its text`() {
        val quote = ChatMessage.Quote(text = "Keep it simple", start = "", end = "")

        assertEquals(3, quote.revealWordCount())
    }
}
