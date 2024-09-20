package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoArchiveLimitTypeConverterTest {
    private val converter = AutoArchiveLimitTypeConverter()

    @Test
    fun `podcast limits are encoded correctly`() {
        val expected = mapOf(
            AutoArchiveLimit.None to 0,
            AutoArchiveLimit.One to 1,
            AutoArchiveLimit.Two to 2,
            AutoArchiveLimit.Five to 5,
            AutoArchiveLimit.Ten to 10,
        )

        val databaseValues = AutoArchiveLimit.entries.associateWith { limit ->
            converter.toInt(limit)
        }

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `podcast groupings are decoded correctly`() {
        val expected = mapOf(
            0 to AutoArchiveLimit.None,
            1 to AutoArchiveLimit.One,
            2 to AutoArchiveLimit.Two,
            5 to AutoArchiveLimit.Five,
            10 to AutoArchiveLimit.Ten,
        )

        val databaseValues = AutoArchiveLimit.entries.associate { it.serverId to converter.fromInt(it.serverId) }

        assertEquals(expected, databaseValues)
    }

    @Test
    fun `decode unknown value`() {
        val limit = converter.fromInt(Int.MIN_VALUE)

        assertEquals(AutoArchiveLimit.None, limit)
    }
}
