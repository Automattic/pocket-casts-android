package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserEpisodeDaoTest {
    lateinit var userEpisodeDao: UserEpisodeDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userEpisodeDao = testDb.userEpisodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun testInsertAndFind() {
        val uuid = UUID.randomUUID().toString()
        runBlocking {
            userEpisodeDao.insert(UserEpisode(uuid = uuid, publishedDate = Date()))
            assertNotNull("Should be able to find inserted episode", userEpisodeDao.findEpisodeByUuid(uuid))
        }
    }
}
