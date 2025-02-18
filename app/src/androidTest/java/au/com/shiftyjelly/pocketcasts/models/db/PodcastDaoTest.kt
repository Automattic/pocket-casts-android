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
import com.squareup.moshi.Moshi
import java.util.UUID
import kotlinx.coroutines.runBlocking
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
    fun testFindSubscribedNotInFolder() = runBlocking {
        val subscribed1 = Podcast(uuid = "podcast1", isSubscribed = true, rawFolderUuid = UUID.randomUUID().toString())
        val subscribed2 = Podcast(uuid = "podcast2", isSubscribed = true, rawFolderUuid = null)

        podcastDao.insertBlocking(subscribed1)
        podcastDao.insertBlocking(subscribed2)

        val uuids = podcastDao.findFollowedPodcastsNotInFolderUuid()
        assertEquals(1, uuids.size)
        assertEquals("podcast2", uuids[0])
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
}
