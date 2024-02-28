package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoArchiveInactiveTypeConverterTest {
    private val converter = AutoArchiveInactiveTypeConverter()

    @Test
    fun `podcast groupings are encoded correctly`() {
        val expected = mapOf(
            AutoArchiveInactive.Never to 0,
            AutoArchiveInactive.Hours24 to 1,
            AutoArchiveInactive.Days2 to 2,
            AutoArchiveInactive.Weeks1 to 3,
            AutoArchiveInactive.Weeks2 to 4,
            AutoArchiveInactive.Days30 to 5,
            AutoArchiveInactive.Days90 to 6,
        )

        val databaseValues = AutoArchiveInactive.All.associateWith { grouping ->
            converter.toInt(grouping)
        }

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `podcast groupings are decoded correctly`() {
        val expected = mapOf(
            0 to AutoArchiveInactive.Never,
            1 to AutoArchiveInactive.Hours24,
            2 to AutoArchiveInactive.Days2,
            3 to AutoArchiveInactive.Weeks1,
            4 to AutoArchiveInactive.Weeks2,
            5 to AutoArchiveInactive.Days30,
            6 to AutoArchiveInactive.Days90,
        )

        val databaseValues = List(AutoArchiveInactive.All.size) { it to converter.fromInt(it) }.toMap()

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

        assertEquals(AutoArchiveInactive.Never, grouping)
    }

    @Test
    fun `decode unknown value`() {
        val grouping = converter.fromInt(Int.MIN_VALUE)

        assertEquals(AutoArchiveInactive.Never, grouping)
    }
}
