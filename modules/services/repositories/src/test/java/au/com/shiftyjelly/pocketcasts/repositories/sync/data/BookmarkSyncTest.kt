package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.BookmarkDao
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.bookmarkResponse
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

    @Test
    fun `full sync restores the passage and reference time onto a passageless bookmark`() {
        val local = bookmark(
            passage = null,
            passageLocation = null,
            passageModified = null,
            referenceTime = null,
            referenceTimeModified = null,
        )

        val restored = local.applyServerBookmark(
            serverBookmarkResponse(
                passage = "the captured passage",
                passageLocation = 5,
                passageModified = 4000,
                referenceTime = 42,
                referenceTimeModified = 5000,
            ),
        )

        assertEquals("the captured passage", restored.passage)
        assertEquals(5, restored.passageLocation)
        assertEquals(4000L, restored.passageModified)
        assertEquals(42, restored.referenceTime)
        assertEquals(5000L, restored.referenceTimeModified)
    }

    @Test
    fun `full sync keeps a locally newer passage`() {
        val local = bookmark(passage = "local passage", passageLocation = 1, passageModified = 9000)

        val merged = local.applyServerBookmark(
            serverBookmarkResponse(passage = "server passage", passageLocation = 5, passageModified = 4000),
        )

        assertEquals("local passage", merged.passage)
        assertEquals(1, merged.passageLocation)
        assertEquals(9000L, merged.passageModified)
    }

    @Test
    fun `full sync takes the server passage when the modified dates match`() {
        val local = bookmark(passage = "local passage", passageLocation = 1, passageModified = 4000)

        val merged = local.applyServerBookmark(
            serverBookmarkResponse(passage = "server passage", passageLocation = 5, passageModified = 4000),
        )

        assertEquals("server passage", merged.passage)
        assertEquals(5, merged.passageLocation)
        assertEquals(4000L, merged.passageModified)
    }

    @Test
    fun `full sync leaves local values untouched when the response omits the groups`() {
        val local = bookmark(
            passage = "local passage",
            passageLocation = 1,
            passageModified = 4000,
            referenceTime = 7,
            referenceTimeModified = 5000,
        )

        val merged = local.applyServerBookmark(
            serverBookmarkResponse(passageModified = null, referenceTimeModified = null),
        )

        assertEquals("local passage", merged.passage)
        assertEquals(1, merged.passageLocation)
        assertEquals(4000L, merged.passageModified)
        assertEquals(7, merged.referenceTime)
        assertEquals(5000L, merged.referenceTimeModified)
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

    private fun serverBookmarkResponse(
        passage: String? = "passage",
        passageLocation: Int? = 1,
        passageModified: Long? = 4000,
        referenceTime: Int? = 42,
        referenceTimeModified: Long? = 5000,
    ) = bookmarkResponse {
        bookmarkUuid = "uuid1"
        podcastUuid = "podcast1"
        episodeUuid = "episode1"
        time = 10
        title = "Title"
        passageModified?.let { modifiedAt ->
            this.passage = stringValue { value = passage.orEmpty() }
            this.passageLocation = int32Value { value = passageLocation ?: 0 }
            this.passageModified = int64Value { value = modifiedAt }
        }
        referenceTimeModified?.let { modifiedAt ->
            this.referenceTime = int32Value { value = referenceTime ?: 0 }
            this.referenceTimeModified = int64Value { value = modifiedAt }
        }
    }
}
