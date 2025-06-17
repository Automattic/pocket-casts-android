package au.com.shiftyjelly.pocketcasts.utils.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SearchMatchesTest {
    @Test
    fun `fail to create matches with invalid line coordinate`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SearchMatches(
                selectedCoordinate = SearchCoordinates(
                    line = 1,
                    match = 0,
                ),
                matchingCoordinates = mapOf(
                    0 to listOf(1),
                    2 to listOf(1),
                ),
            )
        }

        assertEquals("Match result is missing for line 1", exception.message)
    }

    @Test
    fun `fail to create matches with invalid match coordinate`() {
        val coordinates = SearchCoordinates(line = 1, match = 0)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            SearchMatches(
                selectedCoordinate = coordinates,
                matchingCoordinates = mapOf(
                    0 to listOf(1),
                    1 to listOf(1),
                ),
            )
        }

        assertEquals("Match result is missing for coordinates $coordinates", exception.message)
    }

    @Test
    fun `calculate correct selected index`() {
        var matches = SearchMatches(
            selectedCoordinate = SearchCoordinates(
                line = 1,
                match = 5,
            ),
            matchingCoordinates = mapOf(
                1 to listOf(5, 10, 25),
                3 to listOf(3, 6),
                8 to listOf(7),
            ),
        )

        val result = List(6) {
            val currentIndex = matches.selectedMatchIndex
            matches = matches.next()
            currentIndex
        }

        assertEquals(listOf(0, 1, 2, 3, 4, 5), result)
    }

    @Test
    fun `calculate match count`() {
        val matches = SearchMatches(
            selectedCoordinate = SearchCoordinates(
                line = 1,
                match = 5,
            ),
            matchingCoordinates = mapOf(
                1 to listOf(5, 10, 25),
                3 to listOf(3, 6),
                8 to listOf(7),
            ),
        )

        assertEquals(6, matches.count)
    }

    @Test
    fun `cycle next search`() {
        var matches = SearchMatches(
            selectedCoordinate = SearchCoordinates(
                line = 1,
                match = 5,
            ),
            matchingCoordinates = mapOf(
                1 to listOf(5, 10, 25),
                3 to listOf(3, 6),
                8 to listOf(7),
            ),
        )

        val result = List(6) {
            matches = matches.next()
            matches.selectedCoordinate
        }

        assertEquals(
            listOf(
                SearchCoordinates(1, 10),
                SearchCoordinates(1, 25),
                SearchCoordinates(3, 3),
                SearchCoordinates(3, 6),
                SearchCoordinates(8, 7),
                SearchCoordinates(1, 5),
            ),
            result,
        )
    }

    @Test
    fun `cycle previous search`() {
        var matches = SearchMatches(
            selectedCoordinate = SearchCoordinates(
                line = 15,
                match = 3,
            ),
            matchingCoordinates = mapOf(
                1 to listOf(5, 11),
                6 to listOf(8, 20, 55),
                15 to listOf(1, 3),
            ),
        )

        val result = List(7) {
            matches = matches.previous()
            matches.selectedCoordinate
        }

        assertEquals(
            listOf(
                SearchCoordinates(15, 1),
                SearchCoordinates(6, 55),
                SearchCoordinates(6, 20),
                SearchCoordinates(6, 8),
                SearchCoordinates(1, 11),
                SearchCoordinates(1, 5),
                SearchCoordinates(15, 3),
            ),
            result,
        )
    }
}
