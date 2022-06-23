package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.PodcastFolderHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class PodcastFolderHelperTest {

    private var formatter = SimpleDateFormat("yyyy-MM-dd")

    private val folderNews = Folder(
        uuid = UUID.randomUUID().toString(),
        name = "News",
        color = 0,
        addedDate = Date(),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST,
        deleted = false,
        syncModified = 0
    )

    private val folderTech = Folder(
        uuid = UUID.randomUUID().toString(),
        name = "Tech",
        color = 0,
        addedDate = Date(),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST,
        deleted = false,
        syncModified = 0
    )

    private val list = listOf(
        PodcastFolder(
            podcast = Podcast(
                uuid = UUID.randomUUID().toString(),
                title = "The Daily",
                author = "The New York Times",
                addedDate = formatter.parse("2022-01-01"),
                latestEpisodeDate = formatter.parse("2022-04-01")
            ),
            folder = folderNews
        ),
        PodcastFolder(
            podcast = Podcast(
                uuid = UUID.randomUUID().toString(),
                title = "Stuff You Should Know",
                author = "iHeartPodcasts",
                addedDate = formatter.parse("2022-02-02"),
                latestEpisodeDate = formatter.parse("2022-03-01")
            )
        ),
        PodcastFolder(
            podcast = Podcast(
                uuid = UUID.randomUUID().toString(),
                title = "Global News Podcast",
                author = "BBC World Service",
                addedDate = formatter.parse("2022-01-02"),
                latestEpisodeDate = formatter.parse("2022-02-01")
            ),
            folder = folderTech
        ),
        PodcastFolder(
            podcast = Podcast(
                uuid = UUID.randomUUID().toString(),
                title = "The World Today",
                author = "ABC Radio",
                addedDate = formatter.parse("2022-02-01"),
                latestEpisodeDate = formatter.parse("2022-01-01")
            )
        )
    )

    @Test
    fun filter() {
        PodcastFolderHelper.filter(searchText = "Daily", list = list).let {
            assertEquals(1, it.size)
            assertEquals("The Daily", it.first().podcast.title)
        }

        PodcastFolderHelper.filter(searchText = "abc", list = list).let {
            assertEquals(1, it.size)
            assertEquals("The World Today", it.first().podcast.title)
        }

        PodcastFolderHelper.filter(searchText = "", list = list).let {
            assertEquals(4, it.size)
        }

        PodcastFolderHelper.filter(searchText = "Notfound", list = list).let {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun sort() {
        PodcastFolderHelper.sortForSelectingPodcasts(
            sortType = PodcastsSortType.NAME_A_TO_Z,
            podcastsSortedByReleaseDate = list,
            currentFolderUuid = null
        ).let {
            assertEquals("Stuff You Should Know", it[0].podcast.title)
            assertEquals("The World Today", it[1].podcast.title)
            // podcasts with existing folders should be at the end of the list
            assertEquals("The Daily", it[2].podcast.title) // prefix of 'The ' is ignored
            assertEquals("Global News Podcast", it[3].podcast.title)
        }

        PodcastFolderHelper.sortForSelectingPodcasts(
            sortType = PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST,
            podcastsSortedByReleaseDate = list,
            currentFolderUuid = null
        ).let {
            assertEquals("The World Today", it[0].podcast.title)
            assertEquals("Stuff You Should Know", it[1].podcast.title)
            // podcasts with existing folders should be at the end of the list
            assertEquals("The Daily", it[2].podcast.title)
            assertEquals("Global News Podcast", it[3].podcast.title)
        }

        PodcastFolderHelper.sortForSelectingPodcasts(
            sortType = PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST,
            podcastsSortedByReleaseDate = list,
            currentFolderUuid = null
        ).let {
            assertEquals("Stuff You Should Know", it[0].podcast.title)
            assertEquals("The World Today", it[1].podcast.title)
            // podcasts with existing folders should be at the end of the list
            assertEquals("The Daily", it[2].podcast.title)
            assertEquals("Global News Podcast", it[3].podcast.title)
        }

        PodcastFolderHelper.sortForSelectingPodcasts(
            sortType = PodcastsSortType.NAME_A_TO_Z,
            podcastsSortedByReleaseDate = list,
            currentFolderUuid = folderNews.uuid
        ).let {
            assertEquals("The Daily", it[0].podcast.title)
            assertEquals("Stuff You Should Know", it[1].podcast.title)
            assertEquals("The World Today", it[2].podcast.title)
            // podcasts with existing folders should be at the end of the list, unless it is the current folder
            assertEquals("Global News Podcast", it[3].podcast.title)
        }
    }
}
