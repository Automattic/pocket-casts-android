package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WhatsNewReadStateStoreTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences("whats-new-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    private fun store() = WhatsNewReadStateStore(preferences)

    @Test
    fun `a message the user opened stays read in the next session`() {
        store().markAsRead(listOf("m1"))

        assertTrue(store().state.value.isRead(message("m1")))
    }

    @Test
    fun `a message nobody opened is not read`() {
        store().markAsRead(listOf("m1"))

        assertFalse(store().state.value.isRead(message("m2")))
    }

    @Test
    fun `listing the feed marks its messages seen as well as listed`() {
        val store = store()

        store.markAsListed(listOf("m1"))

        assertFalse(store.state.value.isUnlisted(message("m1")))
        assertFalse(store.state.value.isUnseen(message("m1")))
        assertFalse(store.state.value.isRead(message("m1")))
    }

    @Test
    fun `the profile tab pointing at a message leaves it off the feed's own list`() {
        val store = store()

        store.markAsSeen(listOf("m1"))

        assertFalse(store.state.value.isUnseen(message("m1")))
        assertTrue(store.state.value.isUnlisted(message("m1")))
    }

    @Test
    fun `a message that was read counts as seen even if the tab never pointed at it`() {
        val store = store()

        store.markAsRead(listOf("m1"))

        assertFalse(store.state.value.isUnseen(message("m1")))
    }

    @Test
    fun `marking messages never forgets the ones marked before`() {
        val store = store()

        store.markAsRead(listOf("m1"))
        store.markAsRead(listOf("m2"))

        assertEquals(setOf("m1", "m2"), store.state.value.readMessageIds)
    }

    @Test
    fun `a read waits for the account until it is uploaded`() {
        store().markAsRead(listOf("m1"))
        assertEquals(setOf("m1"), store().state.value.pendingReadMessageIds)

        store().markAsUploaded(readMessageIds = setOf("m1"))

        assertTrue(store().state.value.pendingReadMessageIds.isEmpty())
    }

    @Test
    fun `the account's read state replaces reads that were already uploaded`() {
        val store = store()
        store.markAsRead(listOf("m1", "m2"))
        store.markAsUploaded(readMessageIds = setOf("m1"))

        store.applyAccountReadState(messageIds = setOf("m1", "m2", "m3"), accountReadMessageIds = setOf("m3"))

        assertEquals(setOf("m2", "m3"), store.state.value.readMessageIds)
    }

    @Test
    fun `a pending unread is not read again by the account`() {
        val store = store()
        store.markAsRead(listOf("m1"))
        store.markAsUploaded(readMessageIds = setOf("m1"))
        store.markAsUnread(listOf("m1"))

        store.applyAccountReadState(messageIds = setOf("m1"), accountReadMessageIds = setOf("m1"))

        assertFalse(store.state.value.isRead(message("m1")))
        assertEquals(setOf("m1"), store.state.value.pendingUnreadMessageIds)
    }

    @Test
    fun `an answered poll stays answered in the next session`() {
        store().markAsResponded("p1")

        assertTrue(store().state.value.hasRespondedTo("p1"))
    }

    @Test
    fun `a message published before the feed started counts as read`() {
        store().startFeed(Instant.parse("2026-09-20T00:00:00Z"))

        val state = store().state.value
        assertTrue(state.isRead(message("old", publishedAt = "2026-09-19T00:00:00Z")))
        assertFalse(state.isRead(message("new", publishedAt = "2026-09-21T00:00:00Z")))
        assertFalse(state.isUnseen(message("old", publishedAt = "2026-09-19T00:00:00Z")))
        assertFalse(state.isUnlisted(message("old", publishedAt = "2026-09-19T00:00:00Z")))
    }

    @Test
    fun `starting the feed again keeps the date it first started`() {
        val store = store()
        store.startFeed(Instant.parse("2026-09-20T00:00:00Z"))

        store.startFeed(Instant.parse("2026-09-25T00:00:00Z"))

        assertEquals(Instant.parse("2026-09-20T00:00:00Z"), store().state.value.feedStartDate)
    }

    @Test
    fun `without a feed start every message the user has not opened is unread`() {
        assertFalse(store().state.value.isRead(message("m1", publishedAt = "2020-01-01T00:00:00Z")))
    }

    @Test
    fun `a read message is not unlisted even if the feed never listed it`() {
        store().markAsRead(listOf("m1"))

        assertFalse(store().state.value.isUnlisted(message("m1")))
    }

    @Test
    fun `forgetting read messages keeps what the dots pointed at, answered polls and the feed start`() {
        val store = store()
        store.startFeed(Instant.parse("2026-09-20T00:00:00Z"))
        store.markAsRead(listOf("m1"))
        store.markAsListed(listOf("m2"))
        store.markAsSeen(listOf("m3"))
        store.markAsResponded("p1")

        store.forgetReadMessages()

        val expected = WhatsNewReadState(
            seenMessageIds = setOf("m2", "m3"),
            listedMessageIds = setOf("m2"),
            respondedPollIds = setOf("p1"),
            feedStartDate = Instant.parse("2026-09-20T00:00:00Z"),
        )
        assertEquals(expected, store.state.value)
        assertEquals(expected, store().state.value)
    }

    @Test
    fun `resetting forgets everything read, seen, listed or answered`() {
        val store = store()
        store.startFeed(Instant.parse("2026-09-20T00:00:00Z"))
        store.markAsRead(listOf("m1"))
        store.markAsListed(listOf("m2"))
        store.markAsResponded("p1")

        store.reset()

        assertEquals(WhatsNewReadState(), store.state.value)
        assertEquals(WhatsNewReadState(), store().state.value)
    }

    @Test
    fun `the state is published to anything watching it`() = runTest {
        val store = store()

        store.state.test {
            assertEquals(emptySet<String>(), awaitItem().readMessageIds)

            store.markAsRead(listOf("m1"))

            assertEquals(setOf("m1"), awaitItem().readMessageIds)
        }
    }

    @Test
    fun `read state wiped on sign out does not come back`() {
        val store = store()
        store.markAsRead(listOf("m1"))

        preferences.edit(commit = true) {
            preferences.all.keys.forEach(::remove)
        }

        assertEquals(WhatsNewReadState(), store.state.value)

        store.markAsSeen(listOf("m2"))
        assertEquals(emptySet<String>(), store.state.value.readMessageIds)
    }

    private fun message(id: String, publishedAt: String = "2026-09-22T00:00:00Z") = WhatsNewMessage(
        id = id,
        type = WhatsNewMessageType.Tip,
        publishedAt = Instant.parse(publishedAt),
        expiresAt = null,
        targeting = WhatsNewTargeting(audiences = emptyList(), minimumAppVersion = null),
        title = "t",
        content = WhatsNewContent.Pages(listOf(WhatsNewPage(image = null, heading = "h", description = "d", action = null))),
    )
}
