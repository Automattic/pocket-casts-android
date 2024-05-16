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
        assertEquals(Pair(10.seconds, 0.seconds), parser.parseTimestamp("10"))
    }

    @Test
    fun testParseTimestampHoursMinutesSeconds() {
        assertEquals(Pair(453590.seconds, 0.seconds), parser.parseTimestamp("125h59m50s"))
        assertEquals(Pair(3600.seconds, 0.seconds), parser.parseTimestamp("1h"))
        assertEquals(Pair(60.seconds, 0.seconds), parser.parseTimestamp("1m"))
        assertEquals(Pair(1.seconds, 0.seconds), parser.parseTimestamp("1s"))
        assertEquals(Pair(3661.seconds, 0.seconds), parser.parseTimestamp("1h1m1s"))
        assertEquals(Pair(5400.seconds, 0.seconds), parser.parseTimestamp("1h30m"))
        assertEquals(Pair(3605.seconds, 0.seconds), parser.parseTimestamp("1h5s"))
        assertEquals(Pair(1805.seconds, 0.seconds), parser.parseTimestamp("30m5s"))
    }

    @Test
    fun testParseTimestampHoursMinutesSecondsFraction() {
        assertEquals(Pair(3723.seconds, 0.seconds), parser.parseTimestamp("1:02:03"))
        assertEquals(Pair(120.seconds, 0.seconds), parser.parseTimestamp("0:02:00"))
        assertEquals(Pair(1500.milliseconds, 0.seconds), parser.parseTimestamp("00:00:01.500000000"))
        assertEquals(Pair(1.hours + 2.minutes + 3.seconds + 500.milliseconds, 0.seconds), parser.parseTimestamp("1:02:03.5000"))
    }

    @Test
    fun testParseTimestampMixedPatterns() {
        assertEquals(Pair(120.seconds, 2.minutes + 1.seconds + 5.milliseconds), parser.parseTimestamp("120,0:02:01.5"))
    }

    @Test
    fun testParseTimestampMinutesAndSecondsShouldNotBeGreaterThan60() {
        assertEquals(Pair(0.seconds, 0.seconds), parser.parseTimestamp("0:60:00"))
        assertEquals(Pair(0.seconds, 0.seconds), parser.parseTimestamp("0:00:60"))
        assertEquals(Pair(0.seconds, 0.seconds), parser.parseTimestamp("00h60m00s"))
        assertEquals(Pair(0.seconds, 0.seconds), parser.parseTimestamp("00h00m60s"))
    }
}
