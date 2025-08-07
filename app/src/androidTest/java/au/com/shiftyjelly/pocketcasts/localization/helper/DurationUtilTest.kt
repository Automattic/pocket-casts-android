package au.com.shiftyjelly.pocketcasts.localization.helper

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationUtilTest {
    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun daysSingular() {
        val duration = 1.days

        val text = duration.toFriendlyString(resources)

        assertEquals("1\u00a0day", text)
    }

    @Test
    fun daysPlural() {
        val duration = 2.days

        val text = duration.toFriendlyString(resources)

        assertEquals("2\u00a0days", text)
    }

    @Test
    fun hoursSingular() {
        val duration = 1.hours

        val text = duration.toFriendlyString(resources)

        assertEquals("1\u00a0hour", text)
    }

    @Test
    fun hoursPlural() {
        val duration = 2.hours

        val text = duration.toFriendlyString(resources)

        assertEquals("2\u00a0hours", text)
    }

    @Test
    fun minutesSingular() {
        val duration = 1.minutes

        val text = duration.toFriendlyString(resources)

        assertEquals("1\u00a0minute", text)
    }

    @Test
    fun minutesPlural() {
        val duration = 2.minutes

        val text = duration.toFriendlyString(resources)

        assertEquals("2\u00a0minutes", text)
    }

    @Test
    fun secondsSingular() {
        val duration = 1.seconds

        val text = duration.toFriendlyString(resources)

        assertEquals("1\u00a0second", text)
    }

    @Test
    fun secondsPlural() {
        val duration = 2.seconds

        val text = duration.toFriendlyString(resources)

        assertEquals("2\u00a0seconds", text)
    }

    @Test
    fun subSecondDurationIsZeroSeconds() {
        val duration = 1.seconds - 1.nanoseconds

        val text = duration.toFriendlyString(resources)

        assertEquals("0\u00a0seconds", text)
    }

    @Test
    fun maxLimitDurationToDays() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, maxUnit = FriendlyDurationUnit.Day)

        assertEquals("1\u00a0day 2\u00a0hours", text)
    }

    @Test
    fun maxLimitDurationToHours() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, maxUnit = FriendlyDurationUnit.Hour)

        assertEquals("26\u00a0hours 3\u00a0minutes", text)
    }

    @Test
    fun maxLimitDurationToMinutes() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, maxUnit = FriendlyDurationUnit.Minute)

        assertEquals("1563\u00a0minutes 4\u00a0seconds", text)
    }

    @Test
    fun maxLimitDurationToSeconds() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, maxUnit = FriendlyDurationUnit.Second)

        assertEquals("93784\u00a0seconds", text)
    }

    @Test
    fun minLimitDurationToDays() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Day, maxPartCount = 4)

        assertEquals("1\u00a0day", text)
    }

    @Test
    fun minLimitDurationToHours() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Hour, maxPartCount = 4)

        assertEquals("1\u00a0day 2\u00a0hours", text)
    }

    @Test
    fun minLimitDurationToMinutes() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Minute, maxPartCount = 4)

        assertEquals("1\u00a0day 2\u00a0hours 3\u00a0minutes", text)
    }

    @Test
    fun minLimitDurationToSeconds() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Second, maxPartCount = 4)

        assertEquals("1\u00a0day 2\u00a0hours 3\u00a0minutes 4\u00a0seconds", text)
    }

    @Test
    fun limitPartsToMaxPartCount() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        assertEquals("1\u00a0day 2\u00a0hours 3\u00a0minutes 4\u00a0seconds", duration.toFriendlyString(resources, maxPartCount = 4))
        assertEquals("1\u00a0day 2\u00a0hours 3\u00a0minutes", duration.toFriendlyString(resources, maxPartCount = 3))
        assertEquals("1\u00a0day 2\u00a0hours", duration.toFriendlyString(resources, maxPartCount = 2))
        assertEquals("1\u00a0day", duration.toFriendlyString(resources, maxPartCount = 1))
    }

    @Test
    fun useMinUnitIfMaxPartCountIsTooLow() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        assertEquals("1\u00a0day", duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Day, maxPartCount = 0))
        assertEquals("26\u00a0hours", duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Hour, maxPartCount = 0))
        assertEquals("1563\u00a0minutes", duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Minute, maxPartCount = 0))
        assertEquals("93784\u00a0seconds", duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Second, maxPartCount = 0))
    }

    @Test
    fun skipZeroParts() {
        val duration = 1.days + 3.minutes // no hours

        val text = duration.toFriendlyString(resources)

        assertEquals("1\u00a0day 3\u00a0minutes", text)
    }

    @Test
    fun negativeDuration() {
        val duration = (-1).days - 2.hours - 3.minutes - 5.seconds

        val text = duration.toFriendlyString(resources)

        assertEquals("0\u00a0seconds", text)
    }

    @Test
    fun zeroDuration() {
        val duration = Duration.ZERO

        val text = duration.toFriendlyString(resources)

        assertEquals("0\u00a0seconds", text)
    }

    @Test
    fun mismatchedMinAndMaxUnits() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources, minUnit = FriendlyDurationUnit.Hour, maxUnit = FriendlyDurationUnit.Minute)

        assertEquals("26\u00a0hours", text)
    }

    @Test
    fun largeDuration() {
        val duration = 100.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyString(resources)

        assertEquals("100\u00a0days 2\u00a0hours", text)
    }
}
