package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class UpNextSortTypeTest {
    @Test
    fun `sort newest to oldest`() {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "0",
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "1",
                publishedDate = Date(1),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "2",
                publishedDate = Date(2),
                addedDate = Date(100),
            ),
            PodcastEpisode(
                uuid = "3",
                publishedDate = Date(2),
                addedDate = Date(200),
            ),
        )

        assertEquals(
            listOf(
                PodcastEpisode(
                    uuid = "3",
                    publishedDate = Date(2),
                    addedDate = Date(200),
                ),
                PodcastEpisode(
                    uuid = "2",
                    publishedDate = Date(2),
                    addedDate = Date(100),
                ),
                PodcastEpisode(
                    uuid = "1",
                    publishedDate = Date(1),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "0",
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
            ),
            episodes.sortedWith(UpNextSortType.NewestToOldest),
        )
    }

    @Test
    fun `sort oldest to newest`() {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "3",
                publishedDate = Date(2),
                addedDate = Date(200),
            ),
            PodcastEpisode(
                uuid = "2",
                publishedDate = Date(2),
                addedDate = Date(100),
            ),
            PodcastEpisode(
                uuid = "1",
                publishedDate = Date(1),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "0",
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
        )

        assertEquals(
            listOf(
                PodcastEpisode(
                    uuid = "0",
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "1",
                    publishedDate = Date(1),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "2",
                    publishedDate = Date(2),
                    addedDate = Date(100),
                ),
                PodcastEpisode(
                    uuid = "3",
                    publishedDate = Date(2),
                    addedDate = Date(200),
                ),
            ),
            episodes.sortedWith(UpNextSortType.OldestToNewest),
        )
    }

    @Test
    fun `sort shortest to longest by time remaining`() {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "2",
                duration = 200.0,
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "0",
                duration = 0.0,
                publishedDate = Date(0),
                addedDate = Date(2),
            ),
            PodcastEpisode(
                uuid = "1",
                duration = 0.0,
                publishedDate = Date(0),
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "3",
                duration = 100.0,
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
        )

        assertEquals(
            listOf(
                PodcastEpisode(
                    uuid = "3",
                    duration = 100.0,
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "2",
                    duration = 200.0,
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
                // Episodes without a known duration sort to the bottom.
                PodcastEpisode(
                    uuid = "1",
                    duration = 0.0,
                    publishedDate = Date(0),
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "0",
                    duration = 0.0,
                    publishedDate = Date(0),
                    addedDate = Date(2),
                ),
            ),
            episodes.sortedWith(UpNextSortType.ShortestToLongest),
        )
    }

    @Test
    fun `sort longest to shortest by time remaining`() {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "2",
                duration = 100.0,
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "0",
                duration = 0.0,
                publishedDate = Date(0),
                addedDate = Date(2),
            ),
            PodcastEpisode(
                uuid = "1",
                duration = 0.0,
                publishedDate = Date(0),
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "3",
                duration = 200.0,
                publishedDate = Date(0),
                addedDate = Date(0),
            ),
        )

        assertEquals(
            listOf(
                PodcastEpisode(
                    uuid = "3",
                    duration = 200.0,
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "2",
                    duration = 100.0,
                    publishedDate = Date(0),
                    addedDate = Date(0),
                ),
                // Episodes without a known duration sort to the bottom.
                PodcastEpisode(
                    uuid = "1",
                    duration = 0.0,
                    publishedDate = Date(0),
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "0",
                    duration = 0.0,
                    publishedDate = Date(0),
                    addedDate = Date(2),
                ),
            ),
            episodes.sortedWith(UpNextSortType.LongestToShortest),
        )
    }

    @Test
    fun `sort by time remaining accounts for already played time`() {
        // A long episode that's almost finished has less time remaining than a
        // short, unplayed one, so it sorts first when shortest to longest.
        val longAlmostDone = PodcastEpisode(
            uuid = "longAlmostDone",
            duration = 3000.0,
            playedUpTo = 2900.0, // 100 remaining
            publishedDate = Date(0),
            addedDate = Date(0),
        )
        val shortUnplayed = PodcastEpisode(
            uuid = "shortUnplayed",
            duration = 600.0,
            playedUpTo = 0.0, // 600 remaining
            publishedDate = Date(0),
            addedDate = Date(0),
        )
        val midHalfPlayed = PodcastEpisode(
            uuid = "midHalfPlayed",
            duration = 1800.0,
            playedUpTo = 900.0, // 900 remaining
            publishedDate = Date(0),
            addedDate = Date(0),
        )
        val episodes = listOf(longAlmostDone, shortUnplayed, midHalfPlayed)

        assertEquals(
            listOf(longAlmostDone, shortUnplayed, midHalfPlayed),
            episodes.sortedWith(UpNextSortType.ShortestToLongest),
        )
        assertEquals(
            listOf(midHalfPlayed, shortUnplayed, longAlmostDone),
            episodes.sortedWith(UpNextSortType.LongestToShortest),
        )
    }

    @Test
    fun `sort by time remaining breaks ties by added date`() {
        // Both have 1000 remaining, so the earlier added episode sorts first.
        val addedEarlier = PodcastEpisode(
            uuid = "addedEarlier",
            duration = 1000.0,
            playedUpTo = 0.0, // 1000 remaining
            publishedDate = Date(0),
            addedDate = Date(1),
        )
        val addedLater = PodcastEpisode(
            uuid = "addedLater",
            duration = 2000.0,
            playedUpTo = 1000.0, // 1000 remaining
            publishedDate = Date(0),
            addedDate = Date(2),
        )
        val episodes = listOf(addedLater, addedEarlier)

        assertEquals(
            listOf(addedEarlier, addedLater),
            episodes.sortedWith(UpNextSortType.ShortestToLongest),
        )
        assertEquals(
            listOf(addedEarlier, addedLater),
            episodes.sortedWith(UpNextSortType.LongestToShortest),
        )
    }
}
