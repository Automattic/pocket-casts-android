package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState.Loaded.RatingText
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.Star
import au.com.shiftyjelly.pocketcasts.utils.extensions.abbreviated
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PodcastRatingsViewModelRatingsStateTest {

    /*
     * Unexpected/Invalid states
     */

    @Test
    fun `Shows nothing with non-null average and null total`() {
        val state = loadedStateWith(average = 0.0, total = null)
        assertTrue(state.ratingText is RatingText.ShowNothing)
    }

    @Test
    fun `Shows nothing with non-zero average and 0 total`() {
        val state = loadedStateWith(average = 1.1, total = 0)
        assertTrue(state.ratingText is RatingText.ShowNothing)
    }

    /*
     * Not Enough to Rate
     */

    @Test
    fun `Shows not enough to rate with null average and null total`() {
        val state = loadedStateWith(average = null, total = null)
        assertTrue(state.ratingText is RatingText.NotEnoughToRate)
    }
    @Test
    fun `Shows not enough to rate with null average and 0 total`() {
        val state = loadedStateWith(average = null, total = 0)
        assertTrue(state.ratingText is RatingText.NotEnoughToRate)
    }

    @Test
    fun `Shows not enough to rate with 0 average and 0 total`() {
        val state = loadedStateWith(average = 0.0, total = 0)
        assertTrue(state.ratingText is RatingText.NotEnoughToRate)
    }

    /*
     * Shows Total
     */

    @Test
    fun `Shows total with non-null average and non-zero total`() {
        val total = 1
        val state = loadedStateWith(average = 0.0, total = total)

        // Compare the ShowTotal::text field because equality is not working on ShowTotal objects
        val expected = RatingText.ShowTotal(total.abbreviated).text
        val actual = (state.ratingText as? RatingText.ShowTotal)?.text
        assertEquals(expected, actual)
    }

    /*
     * Stars list
     */

    @Test
    fun `Star list generation`() {
        fun starValue(n: Double) = when (n) {
            0.0 -> listOf(Star.BorderedStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar)
            0.5 -> listOf(Star.HalfStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar)
            1.0 -> listOf(Star.FilledStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar)
            1.5 -> listOf(Star.FilledStar, Star.HalfStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar)
            2.0 -> listOf(Star.FilledStar, Star.FilledStar, Star.BorderedStar, Star.BorderedStar, Star.BorderedStar)
            2.5 -> listOf(Star.FilledStar, Star.FilledStar, Star.HalfStar, Star.BorderedStar, Star.BorderedStar)
            3.0 -> listOf(Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.BorderedStar, Star.BorderedStar)
            3.5 -> listOf(Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.HalfStar, Star.BorderedStar)
            4.0 -> listOf(Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.BorderedStar)
            4.5 -> listOf(Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.HalfStar)
            5.0 -> listOf(Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.FilledStar, Star.FilledStar)
            else -> throw IllegalArgumentException("Invalid star value: $n")
        }

        listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0).forEach {
            assertEquals("Failed getting stars for $it average.", starValue(it), loadedStateWith(average = it).stars)
        }
    }

    /*
     * Helpers
     */

    private fun loadedStateWith(
        podcastUuid: String = "",
        average: Double? = null,
        total: Int? = null
    ) = RatingState.Loaded(
        PodcastRatings(
            podcastUuid = podcastUuid,
            average = average,
            total = total
        )
    )
}
