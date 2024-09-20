package au.com.shiftyjelly.pocketcasts.models.converter

import java.time.Instant
import junit.framework.TestCase.assertEquals
import org.junit.Test

class InstantConverterTest {
    private val converter = InstantConverter()

    @Test
    fun `convert from db value`() {
        val dbValue = 125542352L

        val instant = converter.fromDbValue(dbValue)

        val expected = Instant.ofEpochMilli(dbValue)
        assertEquals(expected, instant)
    }

    @Test
    fun `convert to db value`() {
        val millis = 984572L
        val instant = Instant.ofEpochMilli(millis)

        val dbValue = converter.toDbValue(instant)

        assertEquals(millis, dbValue)
    }
}
