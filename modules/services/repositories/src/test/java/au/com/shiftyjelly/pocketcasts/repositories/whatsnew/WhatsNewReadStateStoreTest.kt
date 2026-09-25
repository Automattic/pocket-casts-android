package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import app.cash.turbine.test
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

        assertTrue(store().state.value.isRead("m1"))
    }

    @Test
    fun `a message nobody opened is not read`() {
        store().markAsRead(listOf("m1"))

        assertFalse(store().state.value.isRead("m2"))
    }

    @Test
    fun `listing the feed marks its messages seen as well as listed`() {
        val store = store()

        store.markAsListed(listOf("m1"))

        assertTrue(store.state.value.isListed("m1"))
        assertFalse(store.state.value.isUnseen("m1"))
        assertFalse(store.state.value.isRead("m1"))
    }

    @Test
    fun `the profile tab pointing at a message leaves it off the feed's own list`() {
        val store = store()

        store.markAsSeen(listOf("m1"))

        assertFalse(store.state.value.isUnseen("m1"))
        assertFalse(store.state.value.isListed("m1"))
    }

    @Test
    fun `a message that was read counts as seen even if the tab never pointed at it`() {
        val store = store()

        store.markAsRead(listOf("m1"))

        assertFalse(store.state.value.isUnseen("m1"))
    }

    @Test
    fun `marking messages never forgets the ones marked before`() {
        val store = store()

        store.markAsRead(listOf("m1"))
        store.markAsRead(listOf("m2"))

        assertEquals(setOf("m1", "m2"), store.state.value.readMessageIds)
    }

    @Test
    fun `an answered poll stays answered in the next session`() {
        store().markAsResponded("p1")

        assertTrue(store().state.value.hasRespondedTo("p1"))
    }

    @Test
    fun `resetting forgets everything read, seen, listed or answered`() {
        val store = store()
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
}
