package au.com.shiftyjelly.pocketcasts.settings.stats

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsHelperTest {

    @Test
    fun secondsToFriendlyString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("0 seconds", StatsHelper.secondsToFriendlyString(seconds = 0, resources = context.resources))
        assertEquals("34 seconds", StatsHelper.secondsToFriendlyString(seconds = 34, resources = context.resources))
        assertEquals("1 min 37 secs", StatsHelper.secondsToFriendlyString(seconds = 97, resources = context.resources))
        assertEquals("2 mins 4 secs", StatsHelper.secondsToFriendlyString(seconds = 124, resources = context.resources))
        assertEquals("10 hours", StatsHelper.secondsToFriendlyString(seconds = 36000, resources = context.resources))
        assertEquals("1 day 6 hours", StatsHelper.secondsToFriendlyString(seconds = 108061, resources = context.resources))
        assertEquals("10 hours 1 min", StatsHelper.secondsToFriendlyString(seconds = 36061, resources = context.resources))
        assertEquals("2 mins 0 secs", StatsHelper.secondsToFriendlyString(seconds = 120, resources = context.resources))
    }
}
