package au.com.shiftyjelly.pocketcasts.models

import au.com.shiftyjelly.pocketcasts.models.converter.SafeDateTypeConverter
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Date

class SafeDateTypeConverterTest {

    @Test
    fun `creates date from non-null long`() {
        val l = 125542352L
        val expected = Date(l)
        val actual = SafeDateTypeConverter().toDate(l)
        assertEquals(expected, actual)
    }

    @Test
    fun `creates date from null long`() {
        val expected = Date(Instant.EPOCH.toEpochMilli())
        val actual = SafeDateTypeConverter().toDate(null)
        assertEquals(expected, actual)
    }

    @Test
    fun `creates long from non-null date`() {
        val expected = 125542352L
        val d = Date(expected)
        val actual = SafeDateTypeConverter().toLong(d)
        assertEquals(expected, actual)
    }

    @Test
    fun `creates long from null date`() {
        val actual = SafeDateTypeConverter().toLong(null)
        assertEquals(0L, actual)
    }
}
