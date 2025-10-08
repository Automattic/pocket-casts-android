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
}
