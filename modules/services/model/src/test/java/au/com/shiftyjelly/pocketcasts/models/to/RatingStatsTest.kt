package au.com.shiftyjelly.pocketcasts.models.to

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingStatsTest {
    @Test
    fun `rating count`() {
        val stats = RatingStats(
            ones = 0,
            twos = 76,
            threes = 25,
            fours = 11,
            fives = 100,
        )

        assertEquals(stats.count(Rating.One), 0)
        assertEquals(stats.count(Rating.Two), 76)
        assertEquals(stats.count(Rating.Three), 25)
        assertEquals(stats.count(Rating.Four), 11)
        assertEquals(stats.count(Rating.Five), 100)
    }

    @Test
    fun `all ratings`() {
        val stats = RatingStats(
            ones = 0,
            twos = 76,
            threes = 25,
            fours = 11,
            fives = 100,
        )

        assertEquals(stats.count(Rating.One), 0)
        assertEquals(stats.count(Rating.Two), 76)
        assertEquals(stats.count(Rating.Three), 25)
        assertEquals(stats.count(Rating.Four), 11)
        assertEquals(stats.count(Rating.Five), 100)
    }

    @Test
    fun `max rating`() {
        val stats = RatingStats(
            ones = 110,
            twos = 234,
            threes = 17,
            fours = 0,
            fives = 55,
        )

        assertEquals(stats.max(), Rating.Two to 234)
    }

    @Test
    fun `max rating prioritizes higher rating`() {
        val stats = RatingStats(
            ones = 110,
            twos = 234,
            threes = 17,
            fours = 0,
            fives = 234,
        )

        assertEquals(stats.max(), Rating.Five to 234)
    }

    @Test
    fun `relative for no ratings`() {
        val stats = RatingStats(
            ones = 0,
            twos = 0,
            threes = 0,
            fours = 0,
            fives = 0,
        )

        assertEquals(stats.relativeToMax(Rating.One), 0f)
        assertEquals(stats.relativeToMax(Rating.Two), 0f)
        assertEquals(stats.relativeToMax(Rating.Three), 0f)
        assertEquals(stats.relativeToMax(Rating.Four), 0f)
        assertEquals(stats.relativeToMax(Rating.Five), 0f)
    }

    @Test
    fun `relative for equal ratings`() {
        val stats = RatingStats(
            ones = 10,
            twos = 10,
            threes = 10,
            fours = 10,
            fives = 10,
        )

        assertEquals(stats.relativeToMax(Rating.One), 1f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Two), 1f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Three), 1f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Four), 1f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Five), 1f, 0.001f)
    }

    @Test
    fun `relative for ratings`() {
        val stats = RatingStats(
            ones = 0,
            twos = 76,
            threes = 25,
            fours = 11,
            fives = 100,
        )

        assertEquals(stats.relativeToMax(Rating.One), 0f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Two), 0.76f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Three), 0.25f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Four), 0.11f, 0.001f)
        assertEquals(stats.relativeToMax(Rating.Five), 1f, 0.001f)
    }
}
