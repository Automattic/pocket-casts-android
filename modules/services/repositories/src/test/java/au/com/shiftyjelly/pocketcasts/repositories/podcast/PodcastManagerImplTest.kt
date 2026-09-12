package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import android.content.pm.PackageManager
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.jakewharton.rxrelay2.PublishRelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class PodcastManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var appDatabase: AppDatabase

    @Mock
    lateinit var podcastDao: PodcastDao

    @Mock
    lateinit var subscribeManager: SubscribeManager

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    private val subscriptionChangedRelay = PublishRelay.create<String>()

    private lateinit var podcastManager: PodcastManagerImpl

    @Before
    fun setUp() {
        whenever(appDatabase.podcastDao()).thenReturn(podcastDao)
        whenever(appDatabase.episodeDao()).thenReturn(mock())
        whenever(appDatabase.playlistDao()).thenReturn(mock())
        whenever(subscribeManager.subscriptionChangedRelay).thenReturn(subscriptionChangedRelay)
        podcastManager = PodcastManagerImpl(
            episodeManager = mock(),
            settings = mock(),
            context = context,
            subscribeManager = subscribeManager,
            refreshServiceManager = mock(),
            syncManager = mock(),
            podcastRefresher = mock(),
            downloadQueue = mock(),
            applicationScope = CoroutineScope(coroutineRule.testDispatcher),
            ioDispatcher = coroutineRule.testDispatcher,
            appDatabase = appDatabase,
        )
    }

    @Test
    fun `podcast subscriptions emit the current uuids once before any change`() = runTest {
        whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1", "uuid-2"))
        whenever(subscribeManager.getSubscribingPodcastUuids()).thenReturn(emptySet())

        podcastManager.podcastSubscriptionsFlow().test {
            assertEquals(listOf("uuid-1", "uuid-2"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `podcast subscriptions include podcasts that are still subscribing`() = runTest {
        whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1"))
        whenever(subscribeManager.getSubscribingPodcastUuids()).thenReturn(setOf("uuid-1", "uuid-2"))

        podcastManager.podcastSubscriptionsFlow().test {
            assertEquals(listOf("uuid-1", "uuid-2"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `podcast subscriptions reload when a subscription changes`() = runTest {
        whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1"))
        whenever(subscribeManager.getSubscribingPodcastUuids()).thenReturn(emptySet())

        podcastManager.podcastSubscriptionsFlow().test {
            assertEquals(listOf("uuid-1"), awaitItem())

            whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1", "uuid-2"))
            subscriptionChangedRelay.accept("uuid-2")

            assertEquals(listOf("uuid-1", "uuid-2"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `podcast subscriptions reload when a podcast is unsubscribed`() = runTest {
        whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1", "uuid-2"))
        whenever(subscribeManager.getSubscribingPodcastUuids()).thenReturn(emptySet())
        whenever(podcastDao.findPodcastByUuid("uuid-2")).thenReturn(Podcast(uuid = "uuid-2"))
        whenever(context.packageName).thenReturn("au.com.shiftyjelly.pocketcasts")
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.getApplicationInfo(any(), any<Int>())).thenReturn(mock())

        podcastManager.podcastSubscriptionsFlow().test {
            assertEquals(listOf("uuid-1", "uuid-2"), awaitItem())

            whenever(podcastDao.findSubscribedUuids()).thenReturn(listOf("uuid-1"))
            podcastManager.unsubscribe("uuid-2", SourceView.UNKNOWN)

            assertEquals(listOf("uuid-1"), awaitItem())
            expectNoEvents()
        }
    }
}
