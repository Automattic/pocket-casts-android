package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.squareup.moshi.Moshi
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PodcastDaoTest {
    lateinit var podcastDao: PodcastDao
    lateinit var episodeDao: EpisodeDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        podcastDao = testDb.podcastDao()
        episodeDao = testDb.episodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun testInsertPodcast() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insertBlocking(Podcast(uuid = uuid, isSubscribed = true))
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuidBlocking(uuid))
    }

    @Test
    fun testInsertPodcastRx() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insertRxSingle(Podcast(uuid = uuid)).blockingGet()
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuidBlocking(uuid))
    }

    @Test
    fun testInsertMultiple() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insertBlocking(podcast)
        val podcast2 = Podcast(uuid = uuid, isSubscribed = true)
        podcastDao.insertBlocking(podcast2)

        assertEquals("Insert should replace, count should be 1", 1, podcastDao.countByUuidBlocking(uuid))
        assertEquals("Podcast should be replaced, should be subscribed", true, podcastDao.findByUuidBlocking(uuid)?.isSubscribed)
    }

    @Test
    fun testUpdatePodcast() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insertBlocking(podcast)
        assert(podcastDao.findByUuidBlocking(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.updateBlocking(podcast)
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuidBlocking(uuid)?.isSubscribed == true)
    }

    @Test
    fun testUpdatePodcastRx() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insertBlocking(podcast)
        assert(podcastDao.findByUuidBlocking(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.updateRxCompletable(podcast).blockingAwait()
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuidBlocking(uuid)?.isSubscribed == true)
    }

    @Test
    fun testFindSubscribed() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insertBlocking(subscribed)
        podcastDao.insertBlocking(unsubscribed)

        val subscribedList = podcastDao.findSubscribedBlocking()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }

    @Test
    fun testFindSubscribedRx() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insertBlocking(subscribed)
        podcastDao.insertBlocking(unsubscribed)

        val subscribedList = podcastDao.findSubscribedRxSingle().blockingGet()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }

    @Test
    fun testObservePodcastsOrderByRecentlyPlayedEpisode() = runTest {
        val (podcast1, podcast2, _) = insertTestPodcastsAndEpisodes()

        val subscribedList = podcastDao.observePodcastsOrderByRecentlyPlayedEpisode().first()
        assertEquals("Should only be 2 results", 2, subscribedList.size)
        assertEquals("First podcast should be most recently played", podcast2.uuid, subscribedList[0].uuid)
        assertEquals("Second podcast should be least recently played", podcast1.uuid, subscribedList[1].uuid)
    }

    @Test
    fun testObserveFolderPodcastsOrderByRecentlyPlayedEpisode() = runTest {
        val folderId = 1L.toString()
        val (podcast1, podcast2, _) = insertTestPodcastsAndEpisodes(folderId)

        val folderPodcasts = podcastDao.observePodcastsOrderByRecentlyPlayedEpisode(folderUuid = folderId).first()
        assertEquals("Should only be 2 results", 2, folderPodcasts.size)
        assertEquals("First podcast should be most recently played", podcast2.uuid, folderPodcasts[0].uuid)
        assertEquals("Second podcast should be least recently played", podcast1.uuid, folderPodcasts[1].uuid)
    }

    @Test
    fun testFindPodcastsOrderByRecentlyPlayedEpisodeEpisode() = runTest {
        val (podcast1, podcast2, _) = insertTestPodcastsAndEpisodes()

        val subscribedList = podcastDao.findPodcastsOrderByRecentlyPlayedEpisode()
        assertEquals("Should only be 2 results", 2, subscribedList.size)
        assertEquals("First podcast should be most recently played", podcast2.uuid, subscribedList[0].uuid)
        assertEquals("Second podcast should be least recently played", podcast1.uuid, subscribedList[1].uuid)
    }

    @Test
    fun testFindFolderPodcastsOrderByRecentlyPlayedEpisode() = runTest {
        val folderId = 1L.toString()
        val (podcast1, podcast2, _) = insertTestPodcastsAndEpisodes(folderId)

        val folderPodcasts = podcastDao.findPodcastsOrderByRecentlyPlayedEpisode(folderId)
        assertEquals("Should only be 2 results", 2, folderPodcasts.size)
        assertEquals("First podcast should be most recently played", podcast2.uuid, folderPodcasts[0].uuid)
        assertEquals("Second podcast should be least recently played", podcast1.uuid, folderPodcasts[1].uuid)
    }

    private suspend fun insertTestPodcastsAndEpisodes(folderId: String? = null): Triple<Podcast, Podcast, Podcast> {
        val podcast1 = Podcast(uuid = UUID.randomUUID().toString(), rawFolderUuid = folderId, isSubscribed = true)
        val podcast2 = Podcast(uuid = UUID.randomUUID().toString(), rawFolderUuid = folderId, isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), rawFolderUuid = folderId, isSubscribed = false)

        val episode1 = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            publishedDate = Date(),
            podcastUuid = podcast1.uuid,
            lastPlaybackInteraction = 1000L,
        )
        val episode2 = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            publishedDate = Date(),
            podcastUuid = podcast2.uuid,
            lastPlaybackInteraction = 2000L,
        )
        val episode3 = PodcastEpisode(
            uuid = UUID.randomUUID().toString(),
            publishedDate = Date(),
            podcastUuid = unsubscribed.uuid,
            lastPlaybackInteraction = 3000L,
        )

        podcastDao.insertSuspend(podcast1)
        podcastDao.insertSuspend(podcast2)
        podcastDao.insertSuspend(unsubscribed)

        episodeDao.insert(episode1)
        episodeDao.insert(episode2)
        episodeDao.insert(episode3)

        return Triple(podcast1, podcast2, unsubscribed)
    }
}
