package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SubscribeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.FakeCrashLogging
import au.com.shiftyjelly.pocketcasts.utils.Optional
import com.squareup.moshi.Moshi
import io.reactivex.Single
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4ClassRunner::class)
class PodcastManagerTest {
    lateinit var podcastManagerSignedOut: PodcastManager
    lateinit var podcastManagerSignedIn: PodcastManager
    lateinit var appDatabase: AppDatabase
    lateinit var podcastDao: PodcastDao

    val uuid = UUID.randomUUID().toString()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val episodeManager = mock<EpisodeManager>()
        val playlistManager = mock<PlaylistManager>()

        val settings = mock<Settings> {
            on { podcastGroupingDefault } doReturn UserSetting.Mock(PodcastGrouping.None, mock())
            on { showArchivedDefault } doReturn UserSetting.Mock(false, mock())
        }

        val syncManagerSignedOut = mock<SyncManager> {
            on { isLoggedIn() } doReturn false
        }
        val syncManagerSignedIn = mock<SyncManager> {
            on { isLoggedIn() } doReturn true
        }

        val application = context
        val podcastCacheService = mock<PodcastCacheServiceManager> {
            on { getPodcast(uuid) } doReturn Single.just(Podcast(uuid))
        }
        val staticServiceManager = mock<StaticServiceManager> {
            on { getColorsSingle(uuid) } doReturn Single.just(Optional.empty())
        }

        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()

        val refreshServiceManager = mock<RefreshServiceManager> {}
        val subscribeManager = SubscribeManager(appDatabase, podcastCacheService, staticServiceManager, syncManagerSignedOut, application, settings)
        podcastDao = appDatabase.podcastDao()
        podcastManagerSignedOut = PodcastManagerImpl(
            episodeManager = episodeManager,
            playlistManager = playlistManager,
            settings = settings,
            context = application,
            subscribeManager = subscribeManager,
            cacheServiceManager = podcastCacheService,
            refreshServiceManager = refreshServiceManager,
            syncManager = syncManagerSignedOut,
            appDatabase = appDatabase,
            applicationScope = CoroutineScope(Dispatchers.Default),
            crashLogging = FakeCrashLogging(),
        )
        podcastManagerSignedIn = PodcastManagerImpl(
            episodeManager = episodeManager,
            playlistManager = playlistManager,
            settings = settings,
            context = application,
            subscribeManager = subscribeManager,
            cacheServiceManager = podcastCacheService,
            refreshServiceManager = refreshServiceManager,
            syncManager = syncManagerSignedIn,
            applicationScope = CoroutineScope(Dispatchers.Default),
            appDatabase = appDatabase,
            crashLogging = FakeCrashLogging(),
        )
    }

    @After
    fun close() {
        appDatabase.close()
    }

    @Test
    fun testSubscribeToExistingPodcast() {
        podcastDao.insert(Podcast(uuid, isSubscribed = true))
        podcastManagerSignedOut.subscribeToPodcastRx(uuid, sync = false).blockingGet()
        val subscribedList = podcastManagerSignedOut.getSubscribedPodcastUuids().blockingGet()
        assertTrue("Podcast uuid should be subscribed", subscribedList.contains(uuid))
    }

    @Test
    fun testSubscribeToServerPodcast() {
        podcastManagerSignedOut.subscribeToPodcastRx(uuid, sync = false).blockingGet()
        val subscribedList = podcastManagerSignedOut.getSubscribedPodcastUuids().blockingGet()
        assertTrue("Podcast uuid should be subscribed", subscribedList.contains(uuid))
    }

    @Test
    fun testUnsubscribeSignedOut() {
        val playbackManager = mock<PlaybackManager> {}
        podcastDao.insert(Podcast(uuid, isSubscribed = true))
        podcastManagerSignedOut.unsubscribe(uuid, playbackManager)
        val daoPodcast = podcastDao.findByUuid(uuid)
        assertTrue("Podcast should be null", daoPodcast == null)
    }

    @Test
    fun testUnsubscribeSignedIn() {
        val playbackManager = mock<PlaybackManager> {}
        podcastDao.insert(Podcast(uuid, isSubscribed = true))
        podcastManagerSignedIn.unsubscribe(uuid, playbackManager)
        val daoPodcast = podcastDao.findByUuid(uuid)
        assertTrue("Podcast should be unsubscribed", daoPodcast?.isSubscribed == false)
    }
}
