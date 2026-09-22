package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.squareup.moshi.Moshi
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WhatsNewManagerImplTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val moshi: Moshi = NetworkModule().provideMoshi()
    private lateinit var context: Context
    private lateinit var catalogStore: WhatsNewCatalogStore
    private lateinit var readStateStore: WhatsNewReadStateStore
    private lateinit var serviceManager: FakeServiceManager
    private lateinit var settings: Settings

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        File(context.cacheDir, "whats-new").deleteRecursively()

        val preferences = context.getSharedPreferences("whats-new-manager-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        catalogStore = WhatsNewCatalogStore(context, moshi)
        readStateStore = WhatsNewReadStateStore(preferences)
        serviceManager = FakeServiceManager(catalogJson("Browse by network"))
        val subscription = mock<ReadSetting<Subscription?>>()
        whenever(subscription.flow) doReturn MutableStateFlow(null)
        settings = mock()
        whenever(settings.cachedSubscription) doReturn subscription
        whenever(settings.getVersion()) doReturn "8.22"

        FeatureFlag.setEnabled(Feature.WHATS_NEW_FEED, true)
    }

    private fun manager() = WhatsNewManagerImpl(serviceManager, catalogStore, readStateStore, settings, UnconfinedTestDispatcher())

    @Test
    fun `fetches the catalog when there is nothing on disk`() = runTest {
        val manager = manager()

        manager.refreshIfNeeded()

        assertEquals(1, serviceManager.requestCount)
        assertEquals(listOf("Browse by network"), manager.catalog.value?.messages?.map { it.title })
    }

    @Test
    fun `publishes the catalog on disk without going to the network`() = runTest {
        manager().refreshIfNeeded()
        serviceManager.requestCount = 0

        val manager = manager()
        manager.refreshIfNeeded()

        assertEquals(0, serviceManager.requestCount)
        assertEquals(listOf("Browse by network"), manager.catalog.value?.messages?.map { it.title })
    }

    @Test
    fun `fetches again once the copy on disk has aged out`() = runTest {
        manager().refreshIfNeeded()
        ageOutTheCachedCatalog()
        serviceManager.requestCount = 0

        manager().refreshIfNeeded()

        assertEquals(1, serviceManager.requestCount)
    }

    @Test
    fun `refreshing fetches while the copy on disk is current`() = runTest {
        val manager = manager()
        manager.refreshIfNeeded()
        serviceManager.requestCount = 0

        manager.refresh()

        assertEquals(1, serviceManager.requestCount)
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
        ageOutTheCachedCatalog()
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

        assertEquals(0, serviceManager.requestCount)
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

    private fun ageOutTheCachedCatalog() {
        val file = File(File(context.cacheDir, "whats-new"), "catalog-en.json")
        file.setLastModified(System.currentTimeMillis() - WhatsNewManagerImpl.REFRESH_INTERVAL.toMillis() - 1000)
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

    private class FakeServiceManager(
        var catalog: String,
    ) : WhatsNewServiceManager {
        var requestCount = 0
        var error: Exception? = null

        override fun catalogLocale() = "en"

        override suspend fun getCatalog(): String {
            requestCount++
            error?.let { throw it }
            return catalog
        }
    }
}
