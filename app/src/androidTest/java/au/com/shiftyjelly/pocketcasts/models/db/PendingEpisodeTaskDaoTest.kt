package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PendingEpisodeTaskDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PendingEpisodeTask
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.squareup.moshi.Moshi
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingEpisodeTaskDaoTest {
    private lateinit var testDb: AppDatabase
    private lateinit var pendingEpisodeTaskDao: PendingEpisodeTaskDao
    private lateinit var episodeDao: EpisodeDao

    private val createdAt = Instant.parse("2026-09-22T10:00:00Z")

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        pendingEpisodeTaskDao = testDb.pendingEpisodeTaskDao()
        episodeDao = testDb.episodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun storesATaskPerEpisodeAndType() = runTest {
        insertEpisodes("episode-1", "episode-2")

        pendingEpisodeTaskDao.insertAll(
            listOf(
                task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
                task("episode-1", PendingEpisodeTask.Type.UP_NEXT),
                task("episode-2", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
            ),
        )

        assertEquals(
            listOf("episode-1", "episode-2"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD).sorted(),
        )
        assertEquals(
            listOf("episode-1"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.UP_NEXT),
        )
    }

    @Test
    fun ignoresATaskThatIsAlreadyRecorded() = runTest {
        insertEpisodes("episode-1")

        pendingEpisodeTaskDao.insertAll(listOf(task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD)))
        pendingEpisodeTaskDao.insertAll(listOf(task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD)))

        assertEquals(
            listOf("episode-1"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
    }

    @Test
    fun deletesOnlyTheGivenTypeForTheGivenEpisodes() = runTest {
        insertEpisodes("episode-1", "episode-2")
        pendingEpisodeTaskDao.insertAll(
            listOf(
                task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
                task("episode-1", PendingEpisodeTask.Type.UP_NEXT),
                task("episode-2", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
            ),
        )

        pendingEpisodeTaskDao.delete(PendingEpisodeTask.Type.AUTO_DOWNLOAD, listOf("episode-1"))

        assertEquals(
            listOf("episode-2"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
        assertEquals(
            listOf("episode-1"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.UP_NEXT),
        )
    }

    @Test
    fun deletesMoreEpisodesThanSqliteCanBindAtOnce() = runTest {
        val episodeUuids = List(AppDatabase.SQLITE_BIND_ARG_LIMIT * 2) { index -> "episode-$index" }
        insertEpisodes(*episodeUuids.toTypedArray())
        pendingEpisodeTaskDao.insertAll(episodeUuids.map { task(it, PendingEpisodeTask.Type.AUTO_DOWNLOAD) })

        pendingEpisodeTaskDao.delete(PendingEpisodeTask.Type.AUTO_DOWNLOAD, episodeUuids)

        assertEquals(emptyList<String>(), pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD))
    }

    @Test
    fun deletesTasksCreatedBeforeAGivenTime() = runTest {
        insertEpisodes("episode-1", "episode-2")
        pendingEpisodeTaskDao.insertAll(
            listOf(
                task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD, createdAt),
                task("episode-2", PendingEpisodeTask.Type.AUTO_DOWNLOAD, createdAt.plusSeconds(60)),
            ),
        )

        pendingEpisodeTaskDao.deleteCreatedBefore(createdAt.plusSeconds(30))

        assertEquals(
            listOf("episode-2"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
    }

    @Test
    fun deletesTasksWhenTheirEpisodeIsDeleted() = runTest {
        insertEpisodes("episode-1", "episode-2")
        pendingEpisodeTaskDao.insertAll(
            listOf(
                task("episode-1", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
                task("episode-2", PendingEpisodeTask.Type.AUTO_DOWNLOAD),
            ),
        )

        episodeDao.deleteAll(listOf(PodcastEpisode(uuid = "episode-1", publishedDate = Date())))

        assertEquals(
            listOf("episode-2"),
            pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
    }

    private suspend fun insertEpisodes(vararg uuids: String) {
        episodeDao.insertAllOrIgnore(uuids.map { PodcastEpisode(uuid = it, publishedDate = Date()) })
    }

    private fun task(
        episodeUuid: String,
        type: PendingEpisodeTask.Type,
        createdAt: Instant = this.createdAt,
    ) = PendingEpisodeTask(episodeUuid = episodeUuid, task = type, createdAt = createdAt)
}
