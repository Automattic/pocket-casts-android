package au.com.shiftyjelly.pocketcasts.views.multiselect

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import com.automattic.eventhorizon.BookmarkDeleteFormSubmittedEvent
import com.automattic.eventhorizon.BookmarkDeletedEvent
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions

class BookmarkDeleterTest {
    private val bookmarkManager = mock<BookmarkManager>()
    private val eventSink = TestEventSink()
    private val deleter = BookmarkDeleter(bookmarkManager, EventHorizon(eventSink), TestScope())

    @Test
    fun `deletes every bookmark and tracks the submitted and deleted events`() = runTest {
        deleter.deleteConfirmed(listOf(bookmark("uuid-1")), SourceView.PLAYER)

        assertTrue(eventSink.pollEvent() is BookmarkDeleteFormSubmittedEvent)
        verifyBlocking(bookmarkManager) { deleteToSync("uuid-1") }
        assertTrue(eventSink.pollEvent() is BookmarkDeletedEvent)
    }

    @Test
    fun `tracks one deleted event per bookmark`() = runTest {
        deleter.deleteConfirmed(listOf(bookmark("uuid-1"), bookmark("uuid-2")), SourceView.PROFILE)

        assertTrue(eventSink.pollEvent() is BookmarkDeleteFormSubmittedEvent)
        assertTrue(eventSink.pollEvent() is BookmarkDeletedEvent)
        assertTrue(eventSink.pollEvent() is BookmarkDeletedEvent)
        verifyBlocking(bookmarkManager) { deleteToSync("uuid-1") }
        verifyBlocking(bookmarkManager) { deleteToSync("uuid-2") }
    }

    @Test
    fun `tracks nothing when the bookmark list is empty`() = runTest {
        deleter.deleteConfirmed(emptyList(), SourceView.PODCAST_SCREEN)

        assertTrue(eventSink.isEmpty())
        verifyNoInteractions(bookmarkManager)
    }

    private fun bookmark(uuid: String) = Bookmark(
        uuid = uuid,
        podcastUuid = "podcast-uuid",
        episodeUuid = "episode-uuid",
        timeSecs = 10,
        title = "Bookmark",
        createdAt = Date(),
    )
}
