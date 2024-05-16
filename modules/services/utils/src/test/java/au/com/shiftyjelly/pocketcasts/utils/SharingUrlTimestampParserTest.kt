package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class SharingUrlTimestampParserTest {

    private val parser = SharingUrlTimestampParser()

    @Test
    fun parseTimestampInterval() {
        assertEquals(Pair(28.seconds, 60.seconds), parser.parseTimestamp("28,60"))
        assertEquals(Pair(0.seconds, 20.seconds), parser.parseTimestamp(",20"))
        assertEquals(Pair(10.seconds, null), parser.parseTimestamp("10"))
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
        assertEquals(Pair(120.seconds, null), parser.parseTimestamp("0:02:00"))
        assertEquals(Pair(1500.milliseconds, null), parser.parseTimestamp("00:00:01.500000000"))
        assertEquals(Pair(1.hours + 2.minutes + 3.seconds + 500.milliseconds, null), parser.parseTimestamp("1:02:03.5000"))
    }

    @Test
    fun testParseTimestampMixedPatterns() {
        assertEquals(Pair(120.seconds, 2.minutes + 1.seconds + 5.milliseconds), parser.parseTimestamp("120,0:02:01.5"))
        assertEquals(Pair(2.minutes + 1.seconds + 5.milliseconds, 120.seconds), parser.parseTimestamp("0:02:01.5,120"))
        assertEquals(Pair(1805.seconds, 120.seconds), parser.parseTimestamp("30m5s,120"))
        assertEquals(Pair(120.seconds, 1805.seconds), parser.parseTimestamp("120,30m5s"))
        assertEquals(Pair(1805.seconds, 2.minutes + 1.seconds + 5.milliseconds), parser.parseTimestamp("30m5s,0:02:01.5"))
        assertEquals(Pair(2.minutes + 1.seconds + 5.milliseconds, 1805.seconds), parser.parseTimestamp("0:02:01.5,30m5s"))
    }

    @Test
    fun testParseTimestampMinutesAndSecondsShouldNotBeGreaterThan60() {
        assertEquals(Pair(null, null), parser.parseTimestamp("0:60:00"))
        assertEquals(Pair(null, null), parser.parseTimestamp("0:00:60"))
        assertEquals(Pair(null, null), parser.parseTimestamp("00h60m00s"))
        assertEquals(Pair(null, null), parser.parseTimestamp("00h00m60s"))
    }
}
