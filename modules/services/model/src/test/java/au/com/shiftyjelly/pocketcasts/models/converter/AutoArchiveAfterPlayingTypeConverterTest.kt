package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoArchiveAfterPlayingTypeConverterTest {
    private val converter = AutoArchiveAfterPlayingTypeConverter()

    @Test
    fun `podcast groupings are encoded correctly`() {
        val expected = mapOf(
            AutoArchiveAfterPlaying.Never to 0,
            AutoArchiveAfterPlaying.AfterPlaying to 1,
            AutoArchiveAfterPlaying.Hours24 to 2,
            AutoArchiveAfterPlaying.Days2 to 3,
            AutoArchiveAfterPlaying.Weeks1 to 4,
        )

        val databaseValues = AutoArchiveAfterPlaying.All.associateWith { grouping ->
            converter.toInt(grouping)
        }

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `podcast groupings are decoded correctly`() {
        val expected = mapOf(
            0 to AutoArchiveAfterPlaying.Never,
            1 to AutoArchiveAfterPlaying.AfterPlaying,
            2 to AutoArchiveAfterPlaying.Hours24,
            3 to AutoArchiveAfterPlaying.Days2,
            4 to AutoArchiveAfterPlaying.Weeks1,
        )

        val databaseValues = List(AutoArchiveAfterPlaying.All.size) { it to converter.fromInt(it) }.toMap()

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

        assertEquals(AutoArchiveAfterPlaying.Never, grouping)
    }

    @Test
    fun `decode unknown value`() {
        val grouping = converter.fromInt(Int.MIN_VALUE)

        assertEquals(AutoArchiveAfterPlaying.Never, grouping)
    }
}
