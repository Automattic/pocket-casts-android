package au.com.shiftyjelly.pocketcasts.models.db

import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SubscribeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import com.squareup.moshi.Moshi
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.UUID

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

        val settings = SettingsImpl(
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
            privatePreferences = PreferenceManager.getDefaultSharedPreferences(context),
            context = context,
            firebaseRemoteConfig = mock(),
            moshi = Moshi.Builder().build(),
        )

        val syncManagerSignedOut = mock<SyncManager> {
            on { isLoggedIn() } doReturn false
        }
        val syncManagerSignedIn = mock<SyncManager> {
            on { isLoggedIn() } doReturn true
        }

        val application = context
        val podcastCacheServer = mock<PodcastCacheServerManager> {
            on { getPodcast(uuid) } doReturn Single.just(Podcast(uuid))
        }
        val staticServerManager = mock<StaticServerManager> {
            on { getColorsSingle(uuid) } doReturn Single.just(Optional.empty())
        }

        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        val refreshServerManager = mock<RefreshServerManager> {}
        val subscribeManager = SubscribeManager(appDatabase, podcastCacheServer, staticServerManager, syncManagerSignedOut, application, settings)
        podcastDao = appDatabase.podcastDao()
        podcastManagerSignedOut = PodcastManagerImpl(episodeManager, playlistManager, settings, application, subscribeManager, podcastCacheServer, refreshServerManager, syncManagerSignedOut, appDatabase)
        podcastManagerSignedIn = PodcastManagerImpl(episodeManager, playlistManager, settings, application, subscribeManager, podcastCacheServer, refreshServerManager, syncManagerSignedIn, appDatabase)
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
