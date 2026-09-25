package au.com.shiftyjelly.pocketcasts.repositories.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimestampTest {
    @Test
    fun `parse timestamp with hours minutes and seconds`() {
        assertEquals(3_723_000, parseTimestampMs("01:02:03"))
    }

    @Test
    fun `parse timestamp with comma milliseconds`() {
        assertEquals(3_723_456, parseTimestampMs("01:02:03,456"))
    }

    @Test
    fun `parse timestamp with period milliseconds`() {
        assertEquals(3_723_456, parseTimestampMs("01:02:03.456"))
    }

    @Test
    fun `parse timestamp pads partial milliseconds`() {
        assertEquals(3_723_400, parseTimestampMs("01:02:03.4"))
    }

    @Test
    fun `parse timestamp truncates long milliseconds`() {
        assertEquals(3_723_456, parseTimestampMs("01:02:03.4567"))
    }

    @Test
    fun `parse timestamp returns null for malformed values`() {
        listOf(
            "",
            "01:02",
            "01:02:03:04",
            "01:02:03.milliseconds",
            "01:02:seconds",
            "-01:02:03",
        ).forEach { value ->
            assertNull(value, parseTimestampMs(value))
        }
    }
}
