package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChatWordRevealTest {
    private val text = AnnotatedString("One two  three\nfour")
    private val words = wordRanges(text.text)

    @Test
    fun `words are split on whitespace and zero width spaces`() {
        assertEquals(
            listOf(0..2, 4..6, 9..13, 15..18),
            words,
        )
        assertEquals(listOf(12..13), wordRanges("​ ok", startOffset = 10))
    }

    @Test
    fun `nothing is shown before the first word starts`() {
        assertEquals("", text.revealed(words, wordProgress = 0f, defaultColor = Color.White).text)
    }

    @Test
    fun `a partly revealed word is shown without the whitespace after it`() {
        assertEquals("One two", text.revealed(words, wordProgress = 1.5f, defaultColor = Color.White).text)
    }

    @Test
    fun `the last word reveals the whole text`() {
        assertEquals(text.text, text.revealed(words, wordProgress = 3.2f, defaultColor = Color.White).text)
    }

    @Test
    fun `the newest words fade in`() {
        val revealed = text.revealed(words, wordProgress = 3.5f, defaultColor = Color.White)

        assertEquals(
            listOf(
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = 2.5f / 3)), 4, 7),
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = 1.5f / 3)), 9, 14),
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = 0.5f / 3)), 15, 19),
            ),
            revealed.spanStyles,
        )
    }

    @Test
    fun `a fading word keeps its own colour`() {
        val styled = buildAnnotatedString {
            append("Quote ")
            withStyle(SpanStyle(color = Color.Red)) { append("(1:00)") }
        }

        val revealed = styled.revealed(wordRanges(styled.text), wordProgress = 1.5f, defaultColor = Color.White)

        assertEquals(SpanStyle(color = Color.Red.copy(alpha = 0.5f / 3)), revealed.spanStyles.last().item)
    }

    @Test
    fun `the text is returned unchanged once every word has faded in`() {
        assertSame(text, text.revealed(words, wordProgress = revealedWordTarget(words.size).toFloat(), defaultColor = Color.White))
    }
}
