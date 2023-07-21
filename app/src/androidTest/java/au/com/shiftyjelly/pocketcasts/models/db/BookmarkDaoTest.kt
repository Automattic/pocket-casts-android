package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.BookmarkDao
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@LargeTest
class BookmarkDaoTest {

    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var testDatabase: AppDatabase

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookmarkDao = testDatabase.bookmarkDao()
    }

    @After
    fun closeDatabase() {
        testDatabase.close()
    }

    @Test
    fun testInsertBookmark() {
        runTest {
            val uuid = UUID.randomUUID().toString()
            bookmarkDao.insert(FakeBookmarksGenerator.create(uuid))
            assertNotNull(
                "Inserted bookmark should be able to be found",
                bookmarkDao.findByUuid(uuid)
            )
        }
    }

    @Test
    fun testUpdateBookmark() {
        runTest {
            val uuid = UUID.randomUUID().toString()
            val bookmark = FakeBookmarksGenerator.create(uuid)
            bookmarkDao.insert(bookmark)

            val createdBookmark = bookmarkDao.findByUuid(uuid)
            assert(createdBookmark?.deleted == false)
            assert(createdBookmark?.syncStatus == SyncStatus.NOT_SYNCED)

            bookmark.deleted = true
            bookmark.syncStatus = SyncStatus.SYNCED

            bookmarkDao.update(bookmark)

            val updatedBookmark = bookmarkDao.findByUuid(uuid)
            assert(updatedBookmark?.deleted == true)
            assert(updatedBookmark?.syncStatus == SyncStatus.SYNCED)
        }
    }

    @Test
    fun testDeleteBookmark() {
        runTest {
            val uuid = UUID.randomUUID().toString()
            val bookmark = FakeBookmarksGenerator.create(uuid)
            bookmarkDao.insert(bookmark)
            assertNotNull(bookmarkDao.findByUuid(uuid))
            bookmarkDao.delete(bookmark)
            assertNull(bookmarkDao.findByUuid(uuid))
        }
    }

    @Test
    fun testFindByEpisodeSortCreatedAtAsc() =
        runTest {
            val bookmark1 = FakeBookmarksGenerator.create(createdAt = Date(2000))
            val bookmark2 = FakeBookmarksGenerator.create(createdAt = Date(3000))
            val bookmark3 = FakeBookmarksGenerator.create(createdAt = Date(1000))
            bookmarkDao.insert(bookmark1)
            bookmarkDao.insert(bookmark2)
            bookmarkDao.insert(bookmark3)

            val result = bookmarkDao.findByEpisodeOrderCreatedAtFlow(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                deleted = false,
                isAsc = true
            ).first()

            with(result) {
                assert(get(0).createdAt == bookmark3.createdAt)
                assert(get(1).createdAt == bookmark1.createdAt)
                assert(get(2).createdAt == bookmark2.createdAt)
            }
        }

    @Test
    fun testFindByEpisodeSortCreatedAtDesc() =
        runTest {
            val bookmark1 = FakeBookmarksGenerator.create(createdAt = Date(2000))
            val bookmark2 = FakeBookmarksGenerator.create(createdAt = Date(3000))
            val bookmark3 = FakeBookmarksGenerator.create(createdAt = Date(1000))
            bookmarkDao.insert(bookmark1)
            bookmarkDao.insert(bookmark2)
            bookmarkDao.insert(bookmark3)

            val result = bookmarkDao.findByEpisodeOrderCreatedAtFlow(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                deleted = false,
                isAsc = false
            ).first()

            with(result) {
                assert(get(0).createdAt == bookmark2.createdAt)
                assert(get(1).createdAt == bookmark1.createdAt)
                assert(get(2).createdAt == bookmark3.createdAt)
            }
        }

    @Test
    fun testFindByEpisodeSortTimeAsc() =
        runTest {
            val bookmark1 = FakeBookmarksGenerator.create(time = 100)
            val bookmark2 = FakeBookmarksGenerator.create(time = 300)
            val bookmark3 = FakeBookmarksGenerator.create(time = 200)
            bookmarkDao.insert(bookmark1)
            bookmarkDao.insert(bookmark2)
            bookmarkDao.insert(bookmark3)

            val result = bookmarkDao.findByEpisodeOrderTimeFlow(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                deleted = false,
            ).first()

            with(result) {
                assert(get(0).timeSecs == bookmark1.timeSecs)
                assert(get(1).timeSecs == bookmark3.timeSecs)
                assert(get(2).timeSecs == bookmark2.timeSecs)
            }
        }

    companion object {
        private val episodeUuid = UUID.randomUUID().toString()
        private val podcastUuid = UUID.randomUUID().toString()

        object FakeBookmarksGenerator {
            fun create(
                uuid: String? = null,
                time: Int = 10,
                createdAt: Date = Date(),
            ) = Bookmark(
                uuid = uuid ?: UUID.randomUUID().toString(),
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                timeSecs = time,
                createdAt = createdAt,
                deleted = false,
                syncStatus = SyncStatus.NOT_SYNCED,
                title = ""
            )
        }
    }
}
