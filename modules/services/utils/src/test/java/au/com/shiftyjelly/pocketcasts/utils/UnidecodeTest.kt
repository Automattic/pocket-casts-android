package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.utils.extensions.unidecode
import org.junit.Assert.assertEquals
import org.junit.Test

class UnidecodeTest {
    @Test
    fun `decode unicode characters with accents`() {
        val accentedLetters = mapOf(
            listOf("á", "â", "ä", "Á", "Â", "Ä") to "a",
            listOf("ḃ", "ɓ", "ḇ", "Ḃ", "Ɓ", "Ḇ") to "b",
            listOf("ç", "ć", "ĉ", "Ç", "Ć", "Ĉ") to "c",
            listOf("ď", "đ", "ḓ", "Ď", "Đ", "Ḓ") to "d",
            listOf("é", "è", "ë", "É", "È", "Ë") to "e",
            listOf("ḟ", "ƒ", "ḟ", "Ḟ", "Ƒ", "Ḟ") to "f",
            listOf("ğ", "ģ", "ĝ", "Ğ", "Ģ", "Ĝ") to "g",
            listOf("ĥ", "ḧ", "ḩ", "Ĥ", "Ḧ", "Ḩ") to "h",
            listOf("í", "ì", "î", "Í", "Ì", "Î") to "i",
            listOf("ĵ", "ǰ", "ȷ", "Ĵ", "Ɉ", "ȷ") to "j",
            listOf("ķ", "ḱ", "ǩ", "Ķ", "Ḱ", "Ǩ") to "k",
            listOf("ĺ", "ļ", "ł", "Ĺ", "Ļ", "Ł") to "l",
            listOf("ḿ", "ṁ", "ṃ", "Ḿ", "Ṁ", "Ṃ") to "m",
            listOf("ń", "ñ", "ň", "Ń", "Ñ", "Ň") to "n",
            listOf("ó", "ò", "ö", "Ó", "Ò", "Ö") to "o",
            listOf("ṕ", "ṗ", "ƥ", "Ṕ", "Ṗ", "Ƥ") to "p",
            listOf("ɋ", "ʠ", "ʠ", "Ɋ", "ʠ", "ℚ") to "q",
            listOf("ŕ", "ř", "ṙ", "Ŕ", "Ř", "Ṙ") to "r",
            listOf("ś", "š", "ş", "Ś", "Š", "Ş") to "s",
            listOf("ţ", "ť", "ṫ", "Ţ", "Ť", "Ṫ") to "t",
            listOf("ú", "ù", "ü", "Ú", "Ù", "Ü") to "u",
            listOf("ṽ", "ṿ", "ʋ", "Ṽ", "Ṿ", "Ʋ") to "v",
            listOf("ẃ", "ŵ", "ẅ", "Ẃ", "Ŵ", "Ẅ") to "w",
            listOf("ẋ", "ẍ", "Ẋ", "Ẍ") to "x",
            listOf("ý", "ŷ", "ÿ", "Ý", "Ŷ", "Ÿ") to "y",
            listOf("ź", "ž", "ż", "Ź", "Ž", "Ż") to "z",
        )

        assertDecoding(accentedLetters)
    }

    @Test
    fun `decode Cyrillic alphabet`() {
        val cyrillicLetters = mapOf(
            listOf("А", "а") to "a",
            listOf("Б", "б") to "b",
            listOf("В", "в") to "v",
            listOf("Г", "г") to "g",
            listOf("Д", "д") to "d",
            listOf("Е", "е") to "e",
            listOf("Ë", "ë") to "e",
            listOf("Ж", "ж") to "zh",
            listOf("З", "з") to "z",
            listOf("И", "и") to "i",
            listOf("Й", "й") to "i",
            listOf("К", "к") to "k",
            listOf("Л", "л") to "l",
            listOf("М", "м") to "m",
            listOf("Н", "н") to "n",
            listOf("О", "о") to "o",
            listOf("П", "п") to "p",
            listOf("Р", "р") to "r",
            listOf("С", "с") to "s",
            listOf("Т", "т") to "t",
            listOf("У", "у") to "u",
            listOf("Ф", "ф") to "f",
            listOf("Х", "х") to "kh",
            listOf("Ц", "ц") to "ts",
            listOf("Ч", "ч") to "ch",
            listOf("Ш", "ш") to "sh",
            listOf("Щ", "щ") to "shch",
            listOf("Ы", "ы") to "y",
            listOf("Э", "э") to "e",
            listOf("Ю", "ю") to "iu",
            listOf("Я", "я") to "ia",
        )

        assertDecoding(cyrillicLetters)
    }

    @Test
    fun `accept only letters or digits`() {
        val input = "!(, word …-+=? and numbers 11 231 |\\/*&^"

        assertEquals("word and numbers 11 231", input.unidecode())
    }

    @Test
    fun `collapse whitespace`() {
        val input = "  \t\nthis   is \n\t an example \n\t      "

        assertEquals("this is an example", input.unidecode())
    }

    @Test
    fun `collapse accents`() {
        val input = "It's you're l`accent"

        assertEquals("its youre laccent", input.unidecode())
    }

    private fun assertDecoding(input: Map<List<String>, String>) {
        for ((characters, expected) in input) {
            for (character in characters) {
                val result = character.unidecode()
                assertEquals("'$character' should be decoded as '$expected' but was '$result'", expected, result)
            }
        }
    }
}
