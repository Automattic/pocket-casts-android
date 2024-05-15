package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class SharingUrlTimestampParserTest {

    private val parser = SharingUrlTimestampParser()

    @Test
    fun parseTimestampInterval() {
        assertEquals(Pair(28.seconds, 60.seconds), parser.parseTimestamp("28,60"))
        assertEquals(Pair(0.seconds, 20.seconds), parser.parseTimestamp(",20"))
        assertEquals(Pair(10.seconds, 0.seconds), parser.parseTimestamp("10"))
    }

    @Test
    fun testParseTimestampHoursMinutesSeconds() {
        assertEquals(Pair(453590.seconds, null), parser.parseTimestamp("125h59m50s"))
        assertEquals(Pair(3600.seconds, null), parser.parseTimestamp("1h"))
        assertEquals(Pair(60.seconds, null), parser.parseTimestamp("1m"))
        assertEquals(Pair(1.seconds, null), parser.parseTimestamp("1s"))
        assertEquals(Pair(3661.seconds, null), parser.parseTimestamp("1h1m1s"))
        assertEquals(Pair(5400.seconds, null), parser.parseTimestamp("1h30m"))
        assertEquals(Pair(3605.seconds, null), parser.parseTimestamp("1h5s"))
        assertEquals(Pair(1805.seconds, null), parser.parseTimestamp("30m5s"))
    }

    @Test
    fun testParseTimestampHoursMinutesSecondsFraction() {
        assertEquals(Pair(3723.seconds, null), parser.parseTimestamp("1:02:03"))
        assertEquals(Pair(3728.seconds, null), parser.parseTimestamp("1:02:03.5000"))
        assertEquals(Pair(120.seconds, null), parser.parseTimestamp("0:02:00"))
    }
}
