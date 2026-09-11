package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.BookmarkDao
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BookmarkSyncTest {

    private val bookmarkDao = mock<BookmarkDao>()
    private val appDatabase = mock<AppDatabase> { on { bookmarkDao() } doReturn bookmarkDao }
    private val bookmarkSync = BookmarkSync(mock<SyncManager>(), appDatabase)

    @Test
    fun `outbound sync emits the passage and reference-time groups`() = runTest {
        whenever(bookmarkDao.getAllUnsynced()).thenReturn(
            listOf(
                bookmark(
                    passage = "the captured passage",
                    passageLocation = 5,
                    passageModified = 4000,
                    referenceTime = 42,
                    referenceTimeModified = 5000,
                ),
            ),
        )

        val bookmark = bookmarkSync.incrementalData().single().bookmark

        assertTrue(bookmark.hasPassage())
        assertEquals("the captured passage", bookmark.passage.value)
        assertEquals(5, bookmark.passageLocation.value)
        assertEquals(4000, bookmark.passageModified.value)
        assertTrue(bookmark.hasReferenceTime())
        assertEquals(42, bookmark.referenceTime.value)
        assertEquals(5000, bookmark.referenceTimeModified.value)
    }

    @Test
    fun `outbound sync omits the groups when the bookmark was never enriched`() = runTest {
        whenever(bookmarkDao.getAllUnsynced()).thenReturn(
            listOf(bookmark(passageModified = null, referenceTimeModified = null)),
        )

        val bookmark = bookmarkSync.incrementalData().single().bookmark

        assertFalse(bookmark.hasPassage())
        assertFalse(bookmark.hasPassageModified())
        assertFalse(bookmark.hasReferenceTime())
        assertFalse(bookmark.hasReferenceTimeModified())
    }

    @Test
    fun `outbound sync uses placeholders for a null passage within an emitted group`() = runTest {
        whenever(bookmarkDao.getAllUnsynced()).thenReturn(
            listOf(bookmark(passage = null, passageLocation = null, passageModified = 4000)),
        )

        val bookmark = bookmarkSync.incrementalData().single().bookmark

        assertTrue(bookmark.hasPassage())
        assertEquals("", bookmark.passage.value)
        assertEquals(0, bookmark.passageLocation.value)
        assertEquals(4000, bookmark.passageModified.value)
    }

    private fun bookmark(
        passage: String? = "passage",
        passageLocation: Int? = 1,
        passageModified: Long? = 4000,
        referenceTime: Int? = 42,
        referenceTimeModified: Long? = 5000,
    ) = Bookmark(
        uuid = "uuid1",
        podcastUuid = "podcast1",
        episodeUuid = "episode1",
        timeSecs = 10,
        createdAt = Date(1000),
        title = "Title",
        titleModified = 2000,
        deleted = false,
        deletedModified = 3000,
        passage = passage,
        passageLocation = passageLocation,
        passageModified = passageModified,
        referenceTime = referenceTime,
        referenceTimeModified = referenceTimeModified,
        syncStatus = SyncStatus.NOT_SYNCED,
    )
}
