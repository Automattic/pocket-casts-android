package au.com.shiftyjelly.pocketcasts.localization.helper

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimeHelperTests {

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testMaxTimeDurationShortString() {
        val maxString = TimeHelper.getTimeDurationShortString(timeMs = Long.MAX_VALUE, context = context)
        assertEquals("2562047788015h 12m", maxString)
    }

    @Test
    fun testLongTime1hr() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 60 * 60 + 10, inProgress = true, context = context)
        assertTrue("Time left should be plurised for 1h left but was $timeLeft", timeLeft.text == "1h left")
        assertTrue("Time left description should be plurised for 1 hour left but was $timeLeft", timeLeft.description == "1 hour left")
    }

    @Test
    fun testLongTime2hr() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 60 * 60 * 2 + 10, inProgress = true, context = context)
        assertTrue("Time left should be for 2h left but was $timeLeft", timeLeft.text == "2h left")
        assertTrue("Time left description should be for 2h left but was $timeLeft", timeLeft.description == "2 hours left")
    }

    @Test
    fun testLongTime1hr10min() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 60 * 60 + 1000 * 60 * 10 + 10, inProgress = true, context = context)
        assertTrue("Time left should be 1h 10m left but was $timeLeft", timeLeft.text == "1h 10m left")
        assertTrue("Time left description should be 1h 10m left but was $timeLeft", timeLeft.description == "1 hour 10 minutes left")
    }

    @Test
    fun testLongTime10min() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 60 * 10 + 10, inProgress = true, context = context)
        assertTrue("Time left should be 10m left but was $timeLeft", timeLeft.text == "10m left")
        assertTrue("Time left description should be 10m left but was $timeLeft", timeLeft.description == "10 minutes left")
    }

    @Test
    fun testLongTime10min10sec() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 60 * 10 + 1000 * 10 + 10, inProgress = true, context = context)
        assertTrue("Time left should be 10m left but was $timeLeft", timeLeft.text == "10m left") // We don't show seconds on purpose
        assertTrue("Time left description should be 10m left but was $timeLeft", timeLeft.description == "10 minutes left")
    }

    @Test
    fun testLongTime10sec() {
        val timeLeft = TimeHelper.getTimeLeft(currentTimeMs = 10, durationMs = 1000 * 10 + 10, inProgress = true, context = context)
        assertTrue("Time left should be 10s left but was $timeLeft", timeLeft.text == "10s left")
        assertTrue("Time left description should be 10s left but was $timeLeft", timeLeft.description == "10 seconds left")
    }
}
