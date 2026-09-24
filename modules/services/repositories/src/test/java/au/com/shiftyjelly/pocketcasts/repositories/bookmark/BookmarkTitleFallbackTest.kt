package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkTitleFallbackTest {

    @Test
    fun `takes the first six words of the passage`() {
        assertEquals(
            "that's the thing about selective admissions",
            BookmarkTitleFallback.fromPassage("that's the thing about selective admissions — the difference between"),
        )
    }

    @Test
    fun `keeps a passage shorter than six words`() {
        assertEquals("Not anymore.", BookmarkTitleFallback.fromPassage("Not anymore."))
    }

    @Test
    fun `collapses line breaks and repeated spaces between words`() {
        assertEquals("Hey! Hey! No, no, no! Up", BookmarkTitleFallback.fromPassage("  Hey!\nHey!   No, no,\tno! Up now! You!"))
    }

    @Test
    fun `strips trailing separators from the sixth word`() {
        assertEquals("one two three four five six", BookmarkTitleFallback.fromPassage("one two three four five six, seven"))
        assertEquals("one two three four five six", BookmarkTitleFallback.fromPassage("one two three four five six; seven"))
        assertEquals("one two three four five six", BookmarkTitleFallback.fromPassage("one two three four five six: seven"))
        assertEquals("one two three four five", BookmarkTitleFallback.fromPassage("one two three four five — six seven"))
    }

    @Test
    fun `keeps sentence ending punctuation`() {
        assertEquals("one two three four five six.", BookmarkTitleFallback.fromPassage("one two three four five six. seven"))
    }

    @Test
    fun `caps the title at 100 characters`() {
        val longWord = "a".repeat(150)

        assertEquals(100, BookmarkTitleFallback.fromPassage(longWord)?.length)
    }

    @Test
    fun `returns nothing for a missing or blank passage`() {
        assertNull(BookmarkTitleFallback.fromPassage(null))
        assertNull(BookmarkTitleFallback.fromPassage(" \n "))
        assertNull(BookmarkTitleFallback.fromPassage("—"))
    }
}
