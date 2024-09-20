package au.com.shiftyjelly.pocketcasts.models.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterIndicesTest {
    @Test
    fun testEqualitySameOrder() {
        val chapterIndices1 = ChapterIndices(listOf(1, 2))
        val chapterIndices2 = ChapterIndices(listOf(1, 2))
        assertTrue(chapterIndices1 == chapterIndices2)
    }

    @Test
    fun testEqualitySameOrderWithList() {
        val chapterIndices = ChapterIndices(listOf(1, 2))
        val listOfIndices = listOf(1, 2)
        assertFalse(chapterIndices == listOfIndices)
    }

    @Test
    fun testEqualityDiffOrder() {
        val chapterIndices1 = ChapterIndices(listOf(1, 2))
        val chapterIndices2 = ChapterIndices(listOf(2, 1))
        assertFalse(chapterIndices1 == chapterIndices2)
    }

    @Test
    fun testEqualityDiffOrderSorted() {
        val chapterIndices1 = ChapterIndices(listOf(1, 2))
        val chapterIndices2 = ChapterIndices(listOf(2, 1))
        assertTrue(chapterIndices1.sorted() == chapterIndices2.sorted())
    }

    @Test
    fun testEqualityDiffOrderWithListSorted() {
        val chapterIndices = ChapterIndices(listOf(1, 2))
        val listOfIndices = listOf(2, 1)
        assertTrue(chapterIndices.sorted() == listOfIndices.sorted())
    }
}
