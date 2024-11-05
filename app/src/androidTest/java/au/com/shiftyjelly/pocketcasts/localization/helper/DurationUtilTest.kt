package au.com.shiftyjelly.pocketcasts.localization.helper

import androidx.test.platform.app.InstrumentationRegistry
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

        val text = duration.toFriendlyStirng(resources)

        assertEquals("1 day", text)
    }

    @Test
    fun daysPlural() {
        val duration = 2.days

        val text = duration.toFriendlyStirng(resources)

        assertEquals("2 days", text)
    }

    @Test
    fun hoursSingular() {
        val duration = 1.hours

        val text = duration.toFriendlyStirng(resources)

        assertEquals("1 hour", text)
    }

    @Test
    fun hoursPlural() {
        val duration = 2.hours

        val text = duration.toFriendlyStirng(resources)

        assertEquals("2 hours", text)
    }

    @Test
    fun minutesSingular() {
        val duration = 1.minutes

        val text = duration.toFriendlyStirng(resources)

        assertEquals("1 minute", text)
    }

    @Test
    fun minutesPlural() {
        val duration = 2.minutes

        val text = duration.toFriendlyStirng(resources)

        assertEquals("2 minutes", text)
    }

    @Test
    fun secondsSingular() {
        val duration = 1.seconds

        val text = duration.toFriendlyStirng(resources)

        assertEquals("1 second", text)
    }

    @Test
    fun secondsPlural() {
        val duration = 2.seconds

        val text = duration.toFriendlyStirng(resources)

        assertEquals("2 seconds", text)
    }

    @Test
    fun subSecondDurationIsZeroSeconds() {
        val duration = 1.seconds - 1.nanoseconds

        val text = duration.toFriendlyStirng(resources)

        assertEquals("0 seconds", text)
    }

    @Test
    fun maxLimitDurationToDays() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, maxUnit = FriendlyDurationUnit.Day)

        assertEquals("1 day 2 hours", text)
    }

    @Test
    fun maxLimitDurationToHours() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, maxUnit = FriendlyDurationUnit.Hours)

        assertEquals("26 hours 3 minutes", text)
    }

    @Test
    fun maxLimitDurationToMinutes() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, maxUnit = FriendlyDurationUnit.Minute)

        assertEquals("1563 minutes 4 seconds", text)
    }

    @Test
    fun maxLimitDurationToSeconds() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, maxUnit = FriendlyDurationUnit.Second)

        assertEquals("93784 seconds", text)
    }

    @Test
    fun minLimitDurationToDays() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Day, maxPartCount = 4)

        assertEquals("1 day", text)
    }

    @Test
    fun minLimitDurationToHours() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Hours, maxPartCount = 4)

        assertEquals("1 day 2 hours", text)
    }

    @Test
    fun minLimitDurationToMinutes() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Minute, maxPartCount = 4)

        assertEquals("1 day 2 hours 3 minutes", text)
    }

    @Test
    fun minLimitDurationToSeconds() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        val text = duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Second, maxPartCount = 4)

        assertEquals("1 day 2 hours 3 minutes 4 seconds", text)
    }

    @Test
    fun limitPartsToMaxPartCount() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        assertEquals("1 day 2 hours 3 minutes 4 seconds", duration.toFriendlyStirng(resources, maxPartCount = 4))
        assertEquals("1 day 2 hours 3 minutes", duration.toFriendlyStirng(resources, maxPartCount = 3))
        assertEquals("1 day 2 hours", duration.toFriendlyStirng(resources, maxPartCount = 2))
        assertEquals("1 day", duration.toFriendlyStirng(resources, maxPartCount = 1))
    }

    @Test
    fun useMinUnitIfMaxPartCountIsTooLow() {
        val duration = 1.days + 2.hours + 3.minutes + 4.seconds

        assertEquals("1 day", duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Day, maxPartCount = 0))
        assertEquals("26 hours", duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Hours, maxPartCount = 0))
        assertEquals("1563 minutes", duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Minute, maxPartCount = 0))
        assertEquals("93784 seconds", duration.toFriendlyStirng(resources, minUnit = FriendlyDurationUnit.Second, maxPartCount = 0))
    }

    @Test
    fun skipZeroParts() {
        val duration = 1.days + 3.minutes // no hours

        val text = duration.toFriendlyStirng(resources)

        assertEquals("1 day 3 minutes", text)
    }
}
