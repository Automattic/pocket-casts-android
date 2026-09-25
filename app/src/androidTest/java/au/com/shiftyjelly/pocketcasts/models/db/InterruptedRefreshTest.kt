package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PendingEpisodeTask
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.AutoAddUpNext
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.AutoDownloadEpisodeProvider
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.PendingEpisodeTaskProcessor
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import com.automattic.eventhorizon.EventHorizon
import com.squareup.moshi.Moshi
import java.time.Clock
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Reproduces PCDROID-379: a refresh that is killed after its episodes are committed but before the
 * auto download step runs used to lose those episodes for good, because a later refresh no longer
 * reports them as new.
 */
@RunWith(AndroidJUnit4::class)
class InterruptedRefreshTest {
    private lateinit var testDb: AppDatabase
    private lateinit var episodeManager: EpisodeManager

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val podcast = Podcast(uuid = "podcast-1", autoAddToUpNext = AutoAddUpNext.PLAY_LAST)
    private val podcastManager = mock<PodcastManager> {
        on { findPodcastByUuid("podcast-1") } doReturn podcast
    }
    private val playbackManager = mock<PlaybackManager>()
    private val downloadQueue = mock<DownloadQueue> {
        on { enqueueAll(any(), any(), any()) } doReturn CompletableDeferred(Unit)
    }
    private val autoDownloadProvider = mock<AutoDownloadEpisodeProvider>()

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        episodeManager = EpisodeManagerImpl(
            settings = mock(),
            downloadQueue = mock(),
            context = context,
            appDatabase = testDb,
            podcastCacheServiceManager = mock<PodcastCacheServiceManager>(),
            userEpisodeManager = mock<UserEpisodeManager>(),
            ioDispatcher = testDispatcher,
            eventHorizon = EventHorizon(TestEventSink()),
            clock = Clock.systemUTC(),
        )
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun aLaterRefreshDownloadsTheEpisodesTheInterruptedRefreshMissed() = runTest {
        // The killed refresh: episodes are committed together with the work still owed for them.
        val newEpisodes = listOf(episode("episode-1"), episode("episode-2"))
        val added = episodeManager.add(
            episodes = newEpisodes,
            podcastUuid = "podcast-1",
            downloadMetaData = false,
            pendingTasks = listOf(PendingEpisodeTask.Type.AUTO_DOWNLOAD, PendingEpisodeTask.Type.UP_NEXT),
        )
        assertEquals("Both episodes should be committed", 2, added.size)

        // A later refresh that returns nothing new: the episodes are no longer reported as new.
        val laterRefreshAdded = episodeManager.add(
            episodes = newEpisodes.map { episode(it.uuid) },
            podcastUuid = "podcast-1",
            downloadMetaData = false,
            pendingTasks = listOf(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
        assertEquals("A later refresh sees no new episodes", emptyList<PodcastEpisode>(), laterRefreshAdded)

        autoDownloadProvider.stub {
            on { getAll(listOf("episode-1", "episode-2")) } doReturn setOf("episode-1", "episode-2")
        }

        processor().processAutoDownload(newEpisodeUuids = emptyList())

        // Without the fix nothing would be enqueued here, which is exactly how the episodes were lost.
        verify(downloadQueue).enqueueAll(
            setOf("episode-1", "episode-2"),
            DownloadType.Automatic(bypassAutoDownloadStatus = false),
            SourceView.AUTO_DOWNLOAD,
        )
        assertEquals(
            "The tasks should be cleared once they are enqueued",
            emptyList<String>(),
            testDb.pendingEpisodeTaskDao().findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
    }

    @Test
    fun aLaterRefreshAddsTheEpisodesTheInterruptedRefreshMissedToUpNext() = runTest {
        val added = episodeManager.add(
            episodes = listOf(episode("episode-1")),
            podcastUuid = "podcast-1",
            downloadMetaData = false,
            pendingTasks = listOf(PendingEpisodeTask.Type.UP_NEXT),
        )

        processor().processUpNext(newEpisodeUuids = emptyList())

        verify(playbackManager).addEpisodes(listOf(AutoAddUpNext.PLAY_LAST to added.single()))
        assertEquals(
            "The tasks should be cleared once they are handled",
            emptyList<String>(),
            testDb.pendingEpisodeTaskDao().findEpisodeUuids(PendingEpisodeTask.Type.UP_NEXT),
        )
    }

    @Test
    fun nothingIsOwedWhenTheRefreshAddsNoEpisodes() = runTest {
        episodeManager.add(
            episodes = emptyList(),
            podcastUuid = "podcast-1",
            downloadMetaData = false,
            pendingTasks = listOf(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )

        assertEquals(
            emptyList<String>(),
            testDb.pendingEpisodeTaskDao().findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD),
        )
    }

    private fun processor() = PendingEpisodeTaskProcessor(
        pendingEpisodeTaskDao = testDb.pendingEpisodeTaskDao(),
        episodeManager = episodeManager,
        podcastManager = podcastManager,
        playbackManager = playbackManager,
        autoDownloadProvider = autoDownloadProvider,
        downloadQueue = downloadQueue,
        clock = Clock.systemUTC(),
    )

    private fun episode(uuid: String) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = "podcast-1",
        publishedDate = Date(),
    )
}
