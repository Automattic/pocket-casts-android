package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BumpStatsDaoTest {
    private lateinit var bumpStatsDao: BumpStatsDao
    private lateinit var testDatabase: AppDatabase

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        bumpStatsDao = testDatabase.bumpStatsDao()
    }

    @After
    fun closeDatabase() {
        testDatabase.clearAllTables()
        testDatabase.close()
    }

    @Test
    fun getOldestReturnsTheOldestBumpStatsUpToTheLimit() = runTest {
        insertBumpStats(eventTimes = listOf(300, 100, 200))

        val oldest = bumpStatsDao.getOldest(limit = 2)

        assertEquals(listOf(100L, 200L), oldest.map { it.eventTime })
    }

    @Test
    fun deleteAllRemovesBumpStatsWithNumericProps() = runTest {
        bumpStatsDao.insert(bumpStat(eventTime = 100, customEventProps = mapOf("category" to 5L)))

        val stored = bumpStatsDao.getOldest(limit = 10)
        bumpStatsDao.deleteAll(stored)

        assertEquals(emptyList<Any>(), bumpStatsDao.getOldest(limit = 10))
    }

    @Test
    fun deleteOlderThanRemovesOnlyExpiredBumpStats() = runTest {
        insertBumpStats(eventTimes = listOf(100, 200, 300))

        val deletedCount = bumpStatsDao.deleteOlderThan(timestamp = 200)

        assertEquals(1, deletedCount)
        assertEquals(listOf(200L, 300L), bumpStatsDao.getOldest(limit = 10).map { it.eventTime })
    }

    @Test
    fun deleteAllExceptNewestKeepsTheNewestBumpStats() = runTest {
        insertBumpStats(eventTimes = listOf(100, 400, 200, 300))

        val deletedCount = bumpStatsDao.deleteAllExceptNewest(count = 2)

        assertEquals(2, deletedCount)
        assertEquals(listOf(300L, 400L), bumpStatsDao.getOldest(limit = 10).map { it.eventTime })
    }

    private suspend fun insertBumpStats(eventTimes: List<Long>) {
        eventTimes.forEach { bumpStatsDao.insert(bumpStat(eventTime = it)) }
    }

    private fun bumpStat(eventTime: Long, customEventProps: Map<String, Any> = emptyMap()) = AnonymousBumpStat(
        name = "pcandroid_discover_list_impression_bump",
        eventTime = eventTime,
        customEventProps = customEventProps,
    )
}
