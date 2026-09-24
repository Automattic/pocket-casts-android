package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewDateFormatterTest {
    private val zone = ZoneId.of("Australia/Adelaide")

    private val formatter = WhatsNewDateFormatter(
        todayLabel = "Today",
        monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US),
        monthDayYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
        clock = Clock.fixed(Instant.parse("2026-09-23T02:00:00Z"), zone),
    )

    @Test
    fun `a message published today shows today`() {
        assertEquals("Today", formatter.format(Instant.parse("2026-09-22T15:00:00Z")))
    }

    @Test
    fun `today follows the local calendar day rather than UTC`() {
        assertEquals("Sep 22", formatter.format(Instant.parse("2026-09-22T14:00:00Z")))
    }

    @Test
    fun `a message published earlier this year omits the year`() {
        assertEquals("Jan 5", formatter.format(Instant.parse("2026-01-05T03:00:00Z")))
    }

    @Test
    fun `a message published in an earlier year shows the year`() {
        assertEquals("Dec 31, 2025", formatter.format(Instant.parse("2025-12-31T03:00:00Z")))
    }
}
