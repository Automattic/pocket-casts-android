package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTextRevealTest {
    @Test
    fun `plain text reveals one character per tick`() {
        val timeline = TextRevealTimeline(AnnotatedString("Hello world"))

        assertEquals(11, timeline.ticks)
        assertEquals(0f, timeline.revealedLength(0f))
        assertEquals(4.5f, timeline.revealedLength(4.5f))
        assertEquals(11f, timeline.revealedLength(11f))
    }

    @Test
    fun `the revealed length never passes the text`() {
        val timeline = TextRevealTimeline(AnnotatedString("Hi"))

        assertEquals(2f, timeline.revealedLength(50f))
        assertEquals(0f, timeline.revealedLength(-3f))
    }

    @Test
    fun `the reveal pauses before each new paragraph`() {
        val text = "Intro\n•  Point".withHangingBullets(10.sp)
        val timeline = TextRevealTimeline(text)

        assertEquals(text.length + PARAGRAPH_PAUSE_TICKS, timeline.ticks)
        assertEquals(5f, timeline.revealedLength(5f))
        assertEquals(5f, timeline.revealedLength(5f + PARAGRAPH_PAUSE_TICKS))
        assertEquals(6f, timeline.revealedLength(6f + PARAGRAPH_PAUSE_TICKS))
    }

    @Test
    fun `blank paragraphs do not add a pause`() {
        val text = "Intro\n\nOutro".withHangingBullets(10.sp)
        val timeline = TextRevealTimeline(text)

        assertEquals(text.length + PARAGRAPH_PAUSE_TICKS, timeline.ticks)
    }
}
