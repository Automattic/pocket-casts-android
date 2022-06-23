package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@LargeTest
class PodcastDaoTest {
    lateinit var podcastDao: PodcastDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        podcastDao = testDb.podcastDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun testInsertPodcast() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insert(Podcast(uuid = uuid, isSubscribed = true))
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuid(uuid))
    }

    @Test
    fun testInsertPodcastRx() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insertRx(Podcast(uuid = uuid)).blockingGet()
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuid(uuid))
    }

    @Test
    fun testInsertMultiple() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        val podcast2 = Podcast(uuid = uuid, isSubscribed = true)
        podcastDao.insert(podcast2)

        assertEquals("Insert should replace, count should be 1", 1, podcastDao.countByUuid(uuid))
        assertEquals("Podcast should be replaced, should be subscribed", true, podcastDao.findByUuid(uuid)?.isSubscribed)
    }

    @Test
    fun testUpdatePodcast() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        assert(podcastDao.findByUuid(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.update(podcast)
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuid(uuid)?.isSubscribed == true)
    }

    @Test
    fun testUpdatePodcastRx() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        assert(podcastDao.findByUuid(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.updateRx(podcast).blockingAwait()
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuid(uuid)?.isSubscribed == true)
    }

    @Test
    fun testFindSubscribed() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insert(subscribed)
        podcastDao.insert(unsubscribed)

        val subscribedList = podcastDao.findSubscribed()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }

    @Test
    fun testFindSubscribedRx() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insert(subscribed)
        podcastDao.insert(unsubscribed)

        val subscribedList = podcastDao.findSubscribedRx().blockingGet()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }
}
