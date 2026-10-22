package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.EaseOut
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChatWordRevealTest {
    private val text = AnnotatedString("One two  three\nfour five six seven eight nine ten")
    private val words = wordRanges(text.text)
    private val chunks = revealChunks(words.size)

    @Test
    fun `words are split on whitespace and zero width spaces`() {
        assertEquals(listOf(0..2, 4..6, 9..13, 15..18), words.take(4))
        assertEquals(listOf(12..13), wordRanges("​ ok", startOffset = 10))
    }

    @Test
    fun `words are grouped into chunks of varying size`() {
        assertEquals(listOf(0..2, 3..8, 9..9), chunks)
        assertEquals(emptyList<IntRange>(), revealChunks(0))
    }

    @Test
    fun `nothing is shown before the first chunk starts`() {
        assertEquals("", text.revealed(words, chunks, chunkProgress = 0f, defaultColor = Color.White).text)
    }

    @Test
    fun `a started chunk is shown without the whitespace after it`() {
        assertEquals("One two  three", text.revealed(words, chunks, chunkProgress = 0.5f, defaultColor = Color.White).text)
    }

    @Test
    fun `the last chunk reveals the whole text`() {
        assertEquals(text.text, text.revealed(words, chunks, chunkProgress = 2.2f, defaultColor = Color.White).text)
    }

    @Test
    fun `every word of a fading chunk shares its alpha`() {
        val revealed = text.revealed(words, chunks, chunkProgress = 1f, defaultColor = Color.White)

        val alpha = EaseOut.transform(1f / REVEAL_FADE_CHUNKS)
        assertEquals(
            listOf(
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = alpha)), 0, 3),
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = alpha)), 4, 7),
                AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = alpha)), 9, 14),
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
        val styledWords = wordRanges(styled.text)

        val revealed = styled.revealed(styledWords, revealChunks(styledWords.size), chunkProgress = 1f, defaultColor = Color.White)

        assertEquals(SpanStyle(color = Color.Red.copy(alpha = EaseOut.transform(1f / REVEAL_FADE_CHUNKS))), revealed.spanStyles.last().item)
    }

    @Test
    fun `the text is returned unchanged once every chunk has faded in`() {
        val target = revealedChunkTarget(chunks.size).toFloat()

        assertSame(text, text.revealed(words, chunks, chunkProgress = target, defaultColor = Color.White))
    }
}
