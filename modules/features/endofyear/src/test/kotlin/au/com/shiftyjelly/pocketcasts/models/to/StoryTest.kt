package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.to.Story.YearVsYear
import junit.framework.TestCase.assertEquals
import kotlin.time.Duration.Companion.hours
import org.junit.Test

class StoryTest {

    @Test
    fun `YearVsYear percentageChange when both years are zero`() {
        val story = YearVsYear(
            lastYearDuration = 0.hours,
            thisYearDuration = 0.hours,
            subscriptionTier = null,
        )
        assertEquals(0, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when last year is zero and this year is 100`() {
        val story = YearVsYear(
            lastYearDuration = 0.hours,
            thisYearDuration = 100.hours,
            subscriptionTier = null,
        )
        assertEquals(Int.MAX_VALUE, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when this year is zero and last year is 100`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 0.hours,
            subscriptionTier = null,
        )
        assertEquals(-100, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when no change - same duration`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 100.hours,
            subscriptionTier = null,
        )
        assertEquals(0, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening doubled - 100 to 200 hours`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 200.hours,
            subscriptionTier = null,
        )
        assertEquals(100, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening tripled - 100 to 300 hours`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 300.hours,
            subscriptionTier = null,
        )
        assertEquals(200, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening increased 50 percent - 100 to 150 hours`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 150.hours,
            subscriptionTier = null,
        )
        assertEquals(50, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening halved - 100 to 50 hours`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 50.hours,
            subscriptionTier = null,
        )
        assertEquals(-50, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening decreased 25 percent - 100 to 75 hours`() {
        val story = YearVsYear(
            lastYearDuration = 100.hours,
            thisYearDuration = 75.hours,
            subscriptionTier = null,
        )
        assertEquals(-25, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange when listening decreased to one third - 300 to 100 hours`() {
        val story = YearVsYear(
            lastYearDuration = 300.hours,
            thisYearDuration = 100.hours,
            subscriptionTier = null,
        )
        assertEquals(-67, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange with small increase - 10 to 11 hours`() {
        val story = YearVsYear(
            lastYearDuration = 10.hours,
            thisYearDuration = 11.hours,
            subscriptionTier = null,
        )
        assertEquals(10, story.percentageChange)
    }

    @Test
    fun `YearVsYear percentageChange with small decrease - 10 to 9 hours`() {
        val story = YearVsYear(
            lastYearDuration = 10.hours,
            thisYearDuration = 9.hours,
            subscriptionTier = null,
        )
        assertEquals(-10, story.percentageChange)
    }
}
