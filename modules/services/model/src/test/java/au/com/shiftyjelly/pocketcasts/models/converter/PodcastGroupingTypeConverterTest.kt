package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastGroupingTypeConverterTest {
    private val converter = PodcastGroupingTypeConverter()

    @Test
    fun `podcast groupings are encoded correctly`() {
        val expected = mapOf(
            PodcastGrouping.None to 0,
            PodcastGrouping.Downloaded to 1,
            PodcastGrouping.Unplayed to 2,
            PodcastGrouping.Season to 3,
            PodcastGrouping.Starred to 4,
        )

        val databaseValues = PodcastGrouping.All.associateWith { grouping ->
            converter.toInt(grouping)
        }

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `podcast groupings are decoded correctly`() {
        val expected = mapOf(
            0 to PodcastGrouping.None,
            1 to PodcastGrouping.Downloaded,
            2 to PodcastGrouping.Unplayed,
            3 to PodcastGrouping.Season,
            4 to PodcastGrouping.Starred,
        )

        val databaseValues = List(PodcastGrouping.All.size) { it to converter.fromInt(it) }.toMap()

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `encode null value`() {
        val databaseValue = converter.toInt(null)

        assertEquals(0, databaseValue)
    }

    @Test
    fun `decode null value`() {
        val grouping = converter.fromInt(null)

        assertEquals(PodcastGrouping.None, grouping)
    }

    @Test
    fun `decode unknown value`() {
        val grouping = converter.fromInt(Int.MIN_VALUE)

        assertEquals(PodcastGrouping.None, grouping)
    }
}
