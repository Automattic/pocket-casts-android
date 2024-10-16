package au.com.shiftyjelly.pocketcasts.models.to

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingStatsTest {
    @Test
    fun `relateive sats for no ratings`() {
        val stats = RatingStats(
            ones = 0,
            twos = 0,
            threes = 0,
            fours = 0,
            fives = 0,
        )

        assertEquals(stats.onesRelative, 0f)
        assertEquals(stats.twosRelative, 0f)
        assertEquals(stats.threesRelative, 0f)
        assertEquals(stats.foursRelative, 0f)
        assertEquals(stats.fivesRelative, 0f)
    }

    @Test
    fun `relative stats for equal ratings`() {
        val stats = RatingStats(
            ones = 10,
            twos = 10,
            threes = 10,
            fours = 10,
            fives = 10,
        )

        assertEquals(stats.onesRelative, 1f, 0.001f)
        assertEquals(stats.twosRelative, 1f, 0.001f)
        assertEquals(stats.threesRelative, 1f, 0.001f)
        assertEquals(stats.foursRelative, 1f, 0.001f)
        assertEquals(stats.fivesRelative, 1f, 0.001f)
    }

    @Test
    fun `relative stats for ratings`() {
        val stats = RatingStats(
            ones = 0,
            twos = 76,
            threes = 25,
            fours = 11,
            fives = 100,
        )

        assertEquals(stats.onesRelative, 0f, 0.001f)
        assertEquals(stats.twosRelative, 0.76f, 0.001f)
        assertEquals(stats.threesRelative, 0.25f, 0.001f)
        assertEquals(stats.foursRelative, 0.11f, 0.001f)
        assertEquals(stats.fivesRelative, 1f, 0.001f)
    }
}
