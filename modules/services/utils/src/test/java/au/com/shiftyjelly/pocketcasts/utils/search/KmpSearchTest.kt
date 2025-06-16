package au.com.shiftyjelly.pocketcasts.utils.search

import org.junit.Assert.assertEquals
import org.junit.Test

class KmpSearchTest {
    @Test
    fun `search empty text`() {
        val result = "".kmpSearch("test")

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `search with empty pattern`() {
        val result = "test text".kmpSearch("")

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `find pattern start indicies in text`() {
        val result = "texas text is shorter than california text".kmpSearch("tex")

        assertEquals(listOf(0, 6, 38), result)
    }

    @Test
    fun `find nested results`() {
        val result = "a a a".kmpSearch("a a")

        assertEquals(listOf(0, 2), result)
    }

    @Test
    fun `search is case insensitve`() {
        val result = "abba ABBA ABba abbA".kmpSearch("abba ABBA")

        assertEquals(listOf(0, 5, 10), result)
    }

    @Test
    fun `search is diacretic insensitive`() {
        val text = "testing diacritics żółŁć zolLc.frère hôtel niño über façade māter mătură čaj ząb mąż kő ångest cơm fødsel ȑat"

        assertEquals(
            "Pattern without diacritics not found",
            listOf(19, 25),
            text.kmpSearch("zolLc"),
        )

        assertEquals(
            "Grave Accent (`) not found",
            listOf(31),
            text.kmpSearch("frère"),
        )

        assertEquals(
            "Circumflex (^) not found",
            listOf(37),
            text.kmpSearch("hôtel"),
        )

        assertEquals(
            "Umlaut or Diaeresis (¨) not found",
            listOf(48),
            text.kmpSearch("über"),
        )

        assertEquals(
            "Cedilla (¸) not found",
            listOf(53),
            text.kmpSearch("façade"),
        )

        assertEquals(
            "Macron (¯) not found",
            listOf(60),
            text.kmpSearch("MĀTER"),
        )

        assertEquals(
            "Breve (˘) not found",
            listOf(66),
            text.kmpSearch("mătură"),
        )

        assertEquals(
            "Caron or Háček (ˇ) not found",
            listOf(73),
            text.kmpSearch("čaj"),
        )

        assertEquals(
            "Ogonek (˛) not found",
            listOf(77),
            text.kmpSearch("ząb"),
        )

        assertEquals(
            "Dot Above (˙) not found",
            listOf(81),
            text.kmpSearch("mąż"),
        )

        assertEquals(
            "Double Acute (˝) not found",
            listOf(85),
            text.kmpSearch("kő"),
        )

        assertEquals(
            "Ring Above (°) not found",
            listOf(88),
            text.kmpSearch("ångest"),
        )

        assertEquals(
            "Horn (˛) not found",
            listOf(95),
            text.kmpSearch("cơm"),
        )

        assertEquals(
            "Stroke (Ø) not found",
            listOf(99),
            text.kmpSearch("fødsel"),
        )

        assertEquals(
            "Inverted Breve (̑) not found",
            listOf(106),
            text.kmpSearch("ȑat"),
        )
    }

    @Test
    fun `seaerch multiple lines`() {
        val texts = listOf(
            "text",
            "text with another text",
            "",
            "some tex",
            "t TEXT",
        )

        val result = texts.kmpSearch("text")

        assertEquals(
            mapOf(
                0 to listOf(0),
                1 to listOf(0, 18),
                4 to listOf(2),
            ),
            result,
        )
    }
}
