package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastsSortTypeTest {
    @Test
    fun `name A to Z sorts accented characters correctly`() {
        val podcasts = listOf(
            Podcast(uuid = "1", title = "Álbum"),
            Podcast(uuid = "2", title = "Beta"),
            Podcast(uuid = "3", title = "Alpha"),
        )

        val sorted = podcasts.sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)

        assertEquals(
            listOf(
                Podcast(uuid = "1", title = "Álbum"),
                Podcast(uuid = "3", title = "Alpha"),
                Podcast(uuid = "2", title = "Beta"),
            ),
            sorted,
        )
    }

    @Test
    fun `name A to Z removes 'the' prefix`() {
        val podcasts = listOf(
            Podcast(uuid = "1", title = "The Zebra"),
            Podcast(uuid = "2", title = "Alpha"),
        )

        val sorted = podcasts.sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)

        assertEquals(
            listOf(
                Podcast(uuid = "2", title = "Alpha"),
                Podcast(uuid = "1", title = "The Zebra"),
            ),
            sorted,
        )
    }

    @Test
    fun `name A to Z is case insensitive`() {
        val podcasts = listOf(
            Podcast(uuid = "1", title = "BETA"),
            Podcast(uuid = "2", title = "alpha"),
            Podcast(uuid = "3", title = "Charlie"),
        )

        val sorted = podcasts.sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)

        assertEquals(
            listOf(
                Podcast(uuid = "2", title = "alpha"),
                Podcast(uuid = "1", title = "BETA"),
                Podcast(uuid = "3", title = "Charlie"),
            ),
            sorted,
        )
    }
}
