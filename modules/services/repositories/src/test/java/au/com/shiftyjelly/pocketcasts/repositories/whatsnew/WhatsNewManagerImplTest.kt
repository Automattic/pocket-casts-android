package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalogResponse
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.jakewharton.rxrelay2.BehaviorRelay
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.CacheControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WhatsNewManagerImplTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private lateinit var readStateStore: WhatsNewReadStateStore
    private lateinit var serviceManager: FakeServiceManager
    private lateinit var settings: Settings
    private val isLoggedIn = BehaviorRelay.createDefault(false)
    private val isDotEnabled = MutableStateFlow(true)
    private val showWhatsNewDot = mock<UserSetting<Boolean>> {
        on { flow } doReturn isDotEnabled
        on { value } doAnswer { isDotEnabled.value }
    }
    private val remote = FakeRemoteReadState()
    private val syncManager = mock<SyncManager> {
        on { isLoggedInObservable } doReturn isLoggedIn
        on { isLoggedIn() } doAnswer { isLoggedIn.value == true }
        on { getWhatsNewReadMessageIds(any()) } doSuspendableAnswer { invocation ->
            remote.readAmong(invocation.getArgument(0))
        }
        on { markWhatsNewAsRead(any()) } doSuspendableAnswer { invocation ->
            remote.markAsRead(invocation.getArgument(0))
        }
        on { markWhatsNewAsUnread(any()) } doSuspendableAnswer { invocation ->
            remote.markAsUnread(invocation.getArgument(0))
        }
    }

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("whats-new-manager-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        readStateStore = WhatsNewReadStateStore(preferences)
        serviceManager = FakeServiceManager(catalogJson("Browse by network"))
        val subscription = mock<ReadSetting<Subscription?>>()
        whenever(subscription.flow) doReturn MutableStateFlow(null)
        settings = mock()
        whenever(settings.cachedSubscription) doReturn subscription
        whenever(settings.getVersion()) doReturn "8.22"
        whenever(settings.showWhatsNewDot) doReturn showWhatsNewDot

        FeatureFlag.setEnabled(Feature.WHATS_NEW_FEED, true)
    }

    private fun TestScope.manager() = WhatsNewManagerImpl(
        serviceManager = serviceManager,
        readStateStore = readStateStore,
        settings = settings,
        syncManager = syncManager,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        applicationScope = backgroundScope,
    )

    @Test
    fun `fetches the catalog when nothing is cached`() = runTest {
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(1, serviceManager.networkRequestCount)
        assertEquals(listOf("Browse by network"), manager.catalog.value?.messages?.map { it.title })
    }

    @Test
    fun `publishes the cached catalog before going to the network`() = runTest {
        manager().refreshIfNeeded()
        serviceManager.error = IOException("no network")

        val manager = manager()
        manager.refreshIfNeeded()

        assertEquals(listOf("Browse by network"), manager.catalog.value?.messages?.map { it.title })
    }

    @Test
    fun `lets the http cache decide whether a refresh needs the network`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.refreshIfNeeded()

        assertEquals(listOf(null, null), serviceManager.networkCacheControls)
    }

    @Test
    fun `refreshing revalidates the catalog with the server`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.refresh()

        assertEquals(CacheControl.FORCE_NETWORK, serviceManager.networkCacheControls.last())
    }

    @Test
    fun `a message that expires leaves the feed and its dots on the next refresh`() = runTest {
        serviceManager.catalog = catalogJson("Downloads stalling", expiresAt = Instant.now().plusSeconds(1).toString())
        val manager = manager()
        manager.refreshIfNeeded()

        manager.feedMessages.test {
            assertEquals(listOf("Downloads stalling"), awaitItem().map { it.title })
        }

        Thread.sleep(1100)
        manager.refresh()

        manager.feedMessages.test {
            assertTrue(awaitItem().isEmpty())
        }
        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `a message published in the future stays out of the feed until it is due`() = runTest {
        serviceManager.catalog = catalogJson("Coming soon", publishedAt = Instant.now().plusSeconds(3600).toString())
        val manager = manager()

        manager.refreshIfNeeded()

        manager.feedMessages.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `keeps the catalog it has when the refresh fails`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        serviceManager.error = IOException("no network")

        manager.refresh()

        assertEquals(listOf("Browse by network"), manager.catalog.value?.messages?.map { it.title })
    }

    @Test
    fun `a failure with nothing cached leaves the feed without a catalog`() = runTest {
        serviceManager.error = IOException("no network")
        val manager = manager()

        manager.refreshIfNeeded()

        assertNull(manager.catalog.value)
    }

    @Test
    fun `nothing is fetched while the feature is off`() = runTest {
        FeatureFlag.setEnabled(Feature.WHATS_NEW_FEED, false)

        manager().refresh()

        assertEquals(0, serviceManager.networkRequestCount)
    }

    @Test
    fun `the feed lists what the catalog publishes for this user`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.feedMessages.test {
            assertEquals(listOf("Browse by network"), awaitItem().map { it.title })
        }
    }

    @Test
    fun `a message the feed has never listed puts a dot on the what's new row`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.hasUnlistedMessages.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `listing the feed takes the dot off both the row and the tab`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsListed(listOf("m1"))

        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `the profile tab pointing at a message leaves the row's own dot on`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsSeen(listOf("m1"))

        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnlistedMessages.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `showing profile marks the feed seen, taking the dot off the tab but not the row`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.markFeedAsSeen()

        assertEquals(setOf("m1"), manager.readState.value.seenMessageIds)
        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnlistedMessages.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `showing profile before anything has loaded marks nothing seen`() = runTest {
        val manager = manager()

        manager.markFeedAsSeen()

        assertTrue(manager.readState.value.seenMessageIds.isEmpty())
    }

    @Test
    fun `a fresh install starts with the messages published before it read and without dots`() = runTest {
        val manager = manager()
        manager.startFeed()
        manager.refreshIfNeeded()

        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `a message published after a fresh install started is unread`() = runTest {
        readStateStore.startFeed(Instant.parse("2026-09-01T00:00:00Z"))
        val manager = manager()
        manager.refreshIfNeeded()

        manager.hasUnlistedMessages.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `a read message does not light the row's dot even though the feed never listed it`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsRead(listOf("m1"))

        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `forgetting read messages brings them back unread without lighting the dots again`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsListed(listOf("m1"))
        manager.markAsRead(listOf("m1"))

        manager.forgetReadMessages()

        assertTrue(manager.readState.value.readMessageIds.isEmpty())
        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `a message this user is not targeted by never reaches the feed or its dots`() = runTest {
        serviceManager.catalog = catalogJson("For patrons only", audiences = """["patron"]""")
        val manager = manager()

        manager.refreshIfNeeded()

        manager.feedMessages.test {
            assertTrue(awaitItem().isEmpty())
        }
        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `refreshing while signed in takes on the messages the account has read`() = runTest {
        isLoggedIn.accept(true)
        remote.read += "m1"
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(setOf("m1"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `refreshing while signed in tells the account what this device has read`() = runTest {
        isLoggedIn.accept(true)
        readStateStore.markAsRead(listOf("m1"))
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(listOf(setOf("m1")), remote.markedRead)
    }

    @Test
    fun `reading a message while signed in tells the account`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()

        manager.markAsRead(listOf("m1"))

        assertEquals(setOf("m1"), remote.read)
    }

    @Test
    fun `only the messages in the catalog are reconciled`() = runTest {
        isLoggedIn.accept(true)
        readStateStore.markAsRead(listOf("m1", "retired"))
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(listOf(setOf("m1")), remote.markedRead)
    }

    @Test
    fun `nothing is synced while signed out`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        manager.markAsRead(listOf("m1"))

        assertTrue(remote.markedRead.isEmpty())
    }

    @Test
    fun `signing in takes on the messages the account has read`() = runTest {
        remote.read += "m1"
        val manager = manager()
        manager.refreshIfNeeded()
        runCurrent()

        isLoggedIn.accept(true)
        runCurrent()

        assertEquals(setOf("m1"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `a failed sync keeps what this device has read`() = runTest {
        isLoggedIn.accept(true)
        remote.error = IOException("no network")
        readStateStore.markAsRead(listOf("m1"))
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(setOf("m1"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `reads fetched for an account that signs out mid-sync are not taken on`() = runTest {
        isLoggedIn.accept(true)
        remote.read += "m1"
        val manager = manager()
        remote.onList = { manager.forgetReadMessages() }

        manager.refreshIfNeeded()

        assertTrue(manager.readState.value.readMessageIds.isEmpty())
    }

    @Test
    fun `nothing is synced while the feature is off`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()
        FeatureFlag.setEnabled(Feature.WHATS_NEW_FEED, false)

        manager.markAsRead(listOf("m1"))

        assertTrue(remote.markedRead.isEmpty())
    }

    @Test
    fun `reading a message that was already read does not reach the account again`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsRead(listOf("m1"))

        manager.markAsRead(listOf("m1"))

        assertEquals(listOf(setOf("m1")), remote.markedRead)
    }

    @Test
    fun `marking a message unread takes it back here and for the account`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsRead(listOf("m1"))

        manager.markAsUnread(listOf("m1"))

        assertTrue(manager.readState.value.readMessageIds.isEmpty())
        assertEquals(listOf(setOf("m1")), remote.markedUnread)
    }

    @Test
    fun `a message marked unread elsewhere is taken back here and not read again for the account`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markAsRead(listOf("m1"))

        remote.read -= "m1"
        manager.refresh()

        assertTrue(manager.readState.value.readMessageIds.isEmpty())
        assertEquals(listOf(setOf("m1")), remote.markedRead)
    }

    @Test
    fun `a read that failed to reach the account is sent on the next sync`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()
        manager.refreshIfNeeded()
        remote.error = IOException("no network")
        manager.markAsRead(listOf("m1"))

        remote.error = null
        manager.refresh()

        assertEquals(setOf("m1"), remote.read)
        assertEquals(setOf("m1"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `marking a message unread that was not read does not reach the account`() = runTest {
        isLoggedIn.accept(true)
        val manager = manager()

        manager.markAsUnread(listOf("m1"))

        assertTrue(remote.markedUnread.isEmpty())
    }

    @Test
    fun `turning the dot off takes it off the row and the tab`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()

        isDotEnabled.value = false

        manager.hasUnlistedMessages.test {
            assertFalse(awaitItem())
        }
        manager.hasUnseenMessages.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `turning the dot back on shows it again for messages still unseen`() = runTest {
        isDotEnabled.value = false
        val manager = manager()
        manager.refreshIfNeeded()
        manager.markFeedAsSeen()

        isDotEnabled.value = true

        manager.hasUnseenMessages.test {
            assertTrue(awaitItem())
        }
    }

    private fun catalogJson(
        title: String,
        audiences: String = """["free"]""",
        publishedAt: String = "2026-09-18T05:11:26Z",
        expiresAt: String? = null,
    ) = """
            {
              "schemaVersion": 1,
              "messages": [
                {
                  "id": "m1",
                  "type": "new_feature",
                  "publishedAt": "$publishedAt",
                  "expiresAt": ${expiresAt?.let { "\"$it\"" }},
                  "targeting": { "audiences": $audiences },
                  "title": "$title",
                  "pages": [{ "heading": "h", "description": "d" }]
                }
              ]
            }
    """.trimIndent()

    private class FakeRemoteReadState {
        val read = mutableSetOf<String>()
        val markedRead = mutableListOf<Set<String>>()
        val markedUnread = mutableListOf<Set<String>>()
        var error: Exception? = null
        var onList: () -> Unit = {}

        fun readAmong(ids: Collection<String>): Set<String> {
            error?.let { throw it }
            onList()
            return read intersect ids.toSet()
        }

        fun markAsRead(ids: Collection<String>) {
            error?.let { throw it }
            markedRead += ids.toSet()
            read += ids
        }

        fun markAsUnread(ids: Collection<String>) {
            markedUnread += ids.toSet()
            read -= ids.toSet()
        }
    }

    private class FakeServiceManager(
        var catalog: String,
    ) : WhatsNewServiceManager {
        private val adapter = NetworkModule().provideMoshi().adapter(WhatsNewCatalogResponse::class.java)
        private var cached: String? = null
        val networkCacheControls = mutableListOf<CacheControl?>()
        val networkRequestCount get() = networkCacheControls.size
        var error: Exception? = null

        override suspend fun getCatalog(cacheControl: CacheControl?): WhatsNewCatalog {
            val body = if (cacheControl?.onlyIfCached == true) {
                cached ?: throw IOException("not cached")
            } else {
                networkCacheControls += cacheControl
                error?.let { throw it }
                catalog.also { cached = it }
            }
            return requireNotNull(adapter.fromJson(body)).toCatalog()
        }
    }
}
