package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherQueueEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpNextDaoTest {
    lateinit var podcastEpisodeDao: EpisodeDao
    lateinit var userEpisodeDao: UserEpisodeDao
    lateinit var upNextDao: UpNextDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        podcastEpisodeDao = testDb.episodeDao()
        userEpisodeDao = testDb.userEpisodeDao()
        upNextDao = testDb.upNextDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun getNovaLauncherQueuePodcastEpisodes() = runTest {
        val podcastEpisode = PodcastEpisode(
            uuid = "id-1",
            podcastUuid = "p-id-1",
            title = "title-1",
            duration = 1000.0,
            playedUpTo = 500.0,
            season = 10,
            number = 4,
            publishedDate = Date(800),
            lastPlaybackInteraction = 400,
        )
        podcastEpisodeDao.insert(podcastEpisode)
        upNextDao.insert(UpNextEpisode(episodeUuid = "id-1"))

        val episodes = upNextDao.getNovaLauncherQueueEpisodes(limit = 100).first()

        val expected = listOf(
            NovaLauncherQueueEpisode(
                isPodcastEpisode = true,
                id = "id-1",
                title = "title-1",
                duration = 1000,
                currentPosition = 500,
                releaseTimestamp = 800,
                podcastId = "p-id-1",
                seasonNumber = 10,
                episodeNumber = 4,
                lastUsedTimestamp = 400,
                artworkUrl = null,
                tintColorIndex = null,
            ),
        )
        assertEquals(expected, episodes)
    }

    @Test
    fun getNovaLauncherQueueUserEpisodes() = runTest {
        val userEpisode = UserEpisode(
            uuid = "id-1",
            title = "title-1",
            duration = 555.0,
            playedUpTo = 200.0,
            publishedDate = Date(100),
            artworkUrl = "artwork-url-1",
            tintColorIndex = 20,
        )
        userEpisodeDao.insert(userEpisode)
        upNextDao.insert(UpNextEpisode(episodeUuid = "id-1"))

        val episodes = upNextDao.getNovaLauncherQueueEpisodes(limit = 100).first()

        val expected = listOf(
            NovaLauncherQueueEpisode(
                isPodcastEpisode = false,
                id = "id-1",
                title = "title-1",
                duration = 555,
                currentPosition = 200,
                releaseTimestamp = 100,
                artworkUrl = "artwork-url-1",
                tintColorIndex = 20,
                podcastId = null,
                seasonNumber = null,
                episodeNumber = null,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, episodes)
    }

    @Test
    fun getNovaLauncherQueueEpisodesInCorrectOrder() = runTest {
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-4", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-5", publishedDate = Date()))
        upNextDao.insertAll(
            listOf(
                UpNextEpisode(episodeUuid = "id-1", position = 0),
                UpNextEpisode(episodeUuid = "id-2", position = 4),
                UpNextEpisode(episodeUuid = "id-3", position = 1),
                UpNextEpisode(episodeUuid = "id-4", position = 3),
                UpNextEpisode(episodeUuid = "id-5", position = 2),
            ),
        )

        val episodIds = upNextDao.getNovaLauncherQueueEpisodes(limit = 100).first().map(NovaLauncherQueueEpisode::id)

        assertEquals(listOf("id-1", "id-3", "id-5", "id-4", "id-2"), episodIds)
    }

    @Test
    fun ignoreUnknnownNovaLauncherQueueEpisodes() = runTest {
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-5", publishedDate = Date()))
        upNextDao.insertAll(
            listOf(
                UpNextEpisode(episodeUuid = "id-1", position = 0),
                UpNextEpisode(episodeUuid = "id-2", position = 1),
                UpNextEpisode(episodeUuid = "id-3", position = 2),
                UpNextEpisode(episodeUuid = "id-4", position = 3),
                UpNextEpisode(episodeUuid = "id-5", position = 5),
            ),
        )

        val episodIds = upNextDao.getNovaLauncherQueueEpisodes(limit = 100).first().map(NovaLauncherQueueEpisode::id)

        assertEquals(listOf("id-1", "id-3", "id-5"), episodIds)
    }

    @Test
    fun limitNovaLauncherQueueEpisodes() = runTest {
        val podcastEpisodes = List(30) { PodcastEpisode(uuid = "id-$it", publishedDate = Date()) }
        podcastEpisodeDao.insertAll(podcastEpisodes)
        upNextDao.insertAll(podcastEpisodes.mapIndexed { index, episode -> UpNextEpisode(episodeUuid = episode.uuid, position = index) })

        val episodes = upNextDao.getNovaLauncherQueueEpisodes(limit = 8).first()

        assertEquals(8, episodes.size)
    }
}
