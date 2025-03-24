package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextHistoryDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.toUpNextEpisode
import com.squareup.moshi.Moshi
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpNextHistoryDaoTest {
    private lateinit var testDb: AppDatabase
    private lateinit var upNextHistoryDao: UpNextHistoryDao
    private lateinit var upNextDao: UpNextDao
    private val episodeUuid = "episode_uuid"

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        upNextHistoryDao = testDb.upNextHistoryDao()
        upNextDao = testDb.upNextDao()
        val episode = PodcastEpisode(uuid = episodeUuid, publishedDate = Date())
        upNextDao.insertBlocking(episode.toUpNextEpisode())
    }

    @After
    fun closeDb() {
        upNextDao.deleteAllBlocking()
        testDb.close()
    }

    @Test
    fun insertHistoryForDate() = runTest {
        val date = Date()

        upNextHistoryDao.insertHistoryForDate(date)

        val entries = upNextHistoryDao.findAllHistoryEntries()
        assertEquals(1, entries.size)
        assertEquals(date, entries[0].date)
    }

    @Test
    fun doNotInsertHistoryForExistingDate() = runTest {
        val date = Date()

        upNextHistoryDao.insertHistoryForDate(date)
        upNextHistoryDao.insertHistoryForDate(date)

        val entries = upNextHistoryDao.findAllHistoryEntries()
        assertEquals(1, entries.size)
        assertEquals(date, entries[0].date)
    }

    @Test
    fun findAllHistoryEntriesShouldReturnEntriesByDateDescending() = runTest {
        val date1 = Date()
        val date2 = Date(date1.time + 1000)

        upNextHistoryDao.insertHistoryForDate(date1)
        upNextHistoryDao.insertHistoryForDate(date2)

        val entries = upNextHistoryDao.findAllHistoryEntries()
        assertEquals(listOf<Date>(date2, date1), entries.map { it.date })
    }

    @Test
    fun findEpisodeUuidsForDateReturnCorrectUuids() = runTest {
        val date = Date()
        upNextHistoryDao.insertHistoryForDate(date)

        val uuids = upNextHistoryDao.findEpisodeUuidsForDate(date)

        assertEquals(1, uuids.size)
        assertEquals(listOf<String>(episodeUuid), uuids)
    }

    @Test
    fun deleteHistoryOnOrBeforeDateShouldDeleteEntriesCorrectly() = runTest {
        val date1 = Date()
        val date2 = Date(date1.time + 1000)
        val date3 = Date(date1.time + 2000)
        upNextHistoryDao.insertHistoryForDate(date1)
        upNextHistoryDao.insertHistoryForDate(date2)
        upNextHistoryDao.insertHistoryForDate(date3)

        upNextHistoryDao.deleteHistoryOnOrBeforeDate(date2)

        val entries = upNextHistoryDao.findAllHistoryEntries()
        assertEquals(1, entries.size)
        assertEquals(date3, entries[0].date)
    }
}
