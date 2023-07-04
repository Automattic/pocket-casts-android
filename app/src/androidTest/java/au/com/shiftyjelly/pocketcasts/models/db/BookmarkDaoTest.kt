package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.BookmarkDao
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.After
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
            bookmarkDao.insert(
                Bookmark(
                    uuid = uuid,
                    episodeUuid = UUID.randomUUID().toString(),
                    podcastUuid = UUID.randomUUID().toString(),
                    timeSecs = 61,
                    createdAt = Date(),
                    deleted = false,
                    syncStatus = Bookmark.SYNC_STATUS_NOT_SYNCED,
                    title = ""
                )
            )
            assertNotNull("Inserted bookmark should be able to be found", bookmarkDao.findByUuid(uuid))
        }
    }

    @Test
    fun testUpdateBookmark() {
        runTest {
            val uuid = UUID.randomUUID().toString()
            val bookmark = Bookmark(
                uuid = uuid,
                episodeUuid = UUID.randomUUID().toString(),
                podcastUuid = UUID.randomUUID().toString(),
                timeSecs = 61,
                createdAt = Date(),
                deleted = false,
                syncStatus = Bookmark.SYNC_STATUS_NOT_SYNCED,
                title = ""
            )
            bookmarkDao.insert(bookmark)

            val createdBookmark = bookmarkDao.findByUuid(uuid)
            assert(createdBookmark?.deleted == false)
            assert(createdBookmark?.syncStatus == Bookmark.SYNC_STATUS_NOT_SYNCED)

            bookmark.deleted = true
            bookmark.syncStatus = Bookmark.SYNC_STATUS_SYNCED

            bookmarkDao.update(bookmark)

            val updatedBookmark = bookmarkDao.findByUuid(uuid)
            assert(updatedBookmark?.deleted == true)
            assert(updatedBookmark?.syncStatus == Bookmark.SYNC_STATUS_SYNCED)
        }
    }

    @Test
    fun testDeleteBookmark() {
        runTest {
            val uuid = UUID.randomUUID().toString()
            val bookmark = Bookmark(
                uuid = uuid,
                episodeUuid = UUID.randomUUID().toString(),
                podcastUuid = UUID.randomUUID().toString(),
                timeSecs = 61,
                createdAt = Date(),
                deleted = false,
                syncStatus = Bookmark.SYNC_STATUS_NOT_SYNCED,
                title = ""
            )
            bookmarkDao.insert(bookmark)
            assertNotNull(bookmarkDao.findByUuid(uuid))
            bookmarkDao.delete(bookmark)
            assertNull(bookmarkDao.findByUuid(uuid))
        }
    }
}
