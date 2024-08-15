package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KMPSearchTest {
    private lateinit var kmpSearch: KMPSearch

    @Before
    fun setUp() {
        kmpSearch = KMPSearch()
    }

    @Test
    fun `search returns correct indices when pattern is found`() {
        val text = "this is a test. This is only a Test."
        val pattern = "test"
        kmpSearch.setPattern(pattern)

        val result = kmpSearch.search(text)

        assertEquals(listOf(10, 31), result)
    }

    @Test
    fun `search returns empty list when pattern is not found`() {
        val text = "this is a trial. This is only a trial."
        val pattern = "test"
        kmpSearch.setPattern(pattern)

        val result = kmpSearch.search(text)

        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `search returns empty list when pattern is empty`() {
        val text = "this is a test. This is only a test."
        val pattern = ""
        kmpSearch.setPattern(pattern)

        val result = kmpSearch.search(text)

        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `search handles diacritics`() {
        val text = "testing diacritics żółć zolc."

        val pattern1 = "żółć"
        kmpSearch.setPattern(pattern1)
        var result = kmpSearch.search(text)
        assertEquals(listOf(19, 24), result)

        val pattern2 = "zolc"
        kmpSearch.setPattern(pattern2)
        result = kmpSearch.search(text)
        assertEquals(listOf(19, 24), result)
    }
}
