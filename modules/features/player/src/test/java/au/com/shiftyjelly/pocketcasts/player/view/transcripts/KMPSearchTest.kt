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
        val text = "testing diacritics żółć zolc. frère hôtel niño über façade māter mătură čaj ząb mąż kő ångest cơm fødsel ȑat"

        val pattern1 = "żółć"
        kmpSearch.setPattern(pattern1)
        var result = kmpSearch.search(text)
        assertEquals(listOf(19, 24), result)

        // Testing pattern without diacritics
        val pattern2 = "zolc"
        kmpSearch.setPattern(pattern2)
        result = kmpSearch.search(text)
        assertEquals(listOf(19, 24), result)

        // Testing Grave Accent (`)
        val pattern3 = "frère"
        kmpSearch.setPattern(pattern3)
        result = kmpSearch.search(text)
        assertEquals(listOf(30), result)

        // Testing Circumflex (^)
        val pattern4 = "hôtel"
        kmpSearch.setPattern(pattern4)
        result = kmpSearch.search(text)
        assertEquals(listOf(36), result)

        // Testing Umlaut or Diaeresis (¨)
        val pattern5 = "über"
        kmpSearch.setPattern(pattern5)
        result = kmpSearch.search(text)
        assertEquals(listOf(47), result)

        // Testing Cedilla (¸)
        val pattern6 = "façade"
        kmpSearch.setPattern(pattern6)
        result = kmpSearch.search(text)
        assertEquals(listOf(52), result)

        // Testing Macron (¯)
        val pattern8 = "MĀTER"
        kmpSearch.setPattern(pattern8)
        result = kmpSearch.search(text)
        assertEquals(listOf(59), result)

        // Testing Breve (˘)
        val pattern9 = "mătură"
        kmpSearch.setPattern(pattern9)
        result = kmpSearch.search(text)
        assertEquals(listOf(65), result)

        // Testing Caron or Háček (ˇ)
        val pattern10 = "čaj"
        kmpSearch.setPattern(pattern10)
        result = kmpSearch.search(text)
        assertEquals(listOf(72), result)

        // Testing Ogonek (˛)
        val pattern11 = "ząb"
        kmpSearch.setPattern(pattern11)
        result = kmpSearch.search(text)
        assertEquals(listOf(76), result)

        // Testing Dot Above (˙)
        val pattern12 = "mąż"
        kmpSearch.setPattern(pattern12)
        result = kmpSearch.search(text)
        assertEquals(listOf(80), result)

        // Testing Double Acute (˝)
        val pattern13 = "kő"
        kmpSearch.setPattern(pattern13)
        result = kmpSearch.search(text)
        assertEquals(listOf(84), result)

        // Testing Ring Above (°)
        val pattern14 = "ångest"
        kmpSearch.setPattern(pattern14)
        result = kmpSearch.search(text)
        assertEquals(listOf(87), result)

        // Testing Horn (˛)
        val pattern15 = "cơm"
        kmpSearch.setPattern(pattern15)
        result = kmpSearch.search(text)
        assertEquals(listOf(94), result)

        // Testing Stroke (Ø)
        val pattern16 = "fødsel"
        kmpSearch.setPattern(pattern16)
        result = kmpSearch.search(text)
        assertEquals(listOf(98), result)

        // Testing Inverted Breve (̑)
        val pattern17 = "ȑat"
        kmpSearch.setPattern(pattern17)
        result = kmpSearch.search(text)
        assertEquals(listOf(105), result)
    }
}
