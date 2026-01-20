package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.io.File
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class EpisodeManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var appDatabase: AppDatabase

    @Mock
    lateinit var episodeDao: EpisodeDao

    @Mock
    lateinit var transcriptDao: TranscriptDao

    @Mock
    lateinit var downloadManager: DownloadManager

    @Mock
    lateinit var fileStorage: FileStorage

    @Mock
    lateinit var context: Context

    private lateinit var episodeManagerImpl: EpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.episodeDao()).thenReturn(episodeDao)
        whenever(appDatabase.transcriptDao()).thenReturn(transcriptDao)
        whenever(appDatabase.userEpisodeDao()).thenReturn(mock())
        whenever(fileStorage.getOrCreatePodcastEpisodeTempFile(any())).thenReturn(File("/tmp/test"))
        episodeManagerImpl = EpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            fileStorage = fileStorage,
            downloadManager = downloadManager,
            context = context,
            podcastCacheServiceManager = mock(),
            userEpisodeManager = mock(),
            ioDispatcher = coroutineRule.testDispatcher,
            episodeAnalytics = mock(),
        )
    }

    @Test
    fun `get all podcasts episodes`() = runTest {
        val episodes = List(26) { PodcastEpisode(uuid = "$it", publishedDate = Date()) }
        episodeDao.stub {
            on { getAllPodcastEpisodes(any(), any()) } doReturnConsecutively (episodes.chunked(10) + listOf(emptyList()))
        }

        episodeManagerImpl.getAllPodcastEpisodes(10).test {
            episodes.forEachIndexed { index, episode ->
                assertEquals(episode to index, awaitItem())
            }

            awaitComplete()
        }

        verify(episodeDao).getAllPodcastEpisodes(10, 0)
        verify(episodeDao).getAllPodcastEpisodes(10, 10)
        verify(episodeDao).getAllPodcastEpisodes(10, 20)
    }

    @Test
    fun `deleteEpisodesWithoutSync cleans up transcripts`() = runTest {
        val episode1 = PodcastEpisode(uuid = "episode-1", publishedDate = Date()).apply {
            downloadedFilePath = null
        }
        val episode2 = PodcastEpisode(uuid = "episode-2", publishedDate = Date()).apply {
            downloadedFilePath = null
        }
        val episodes = listOf(episode1, episode2)
        val playbackManager = mock<PlaybackManager>()

        episodeManagerImpl.deleteEpisodesWithoutSync(episodes, playbackManager)

        // Verify transcript deletion happens for each episode (via cleanUpDownloadFiles)
        verify(transcriptDao).deleteForEpisode("episode-1")
        verify(transcriptDao).deleteForEpisode("episode-2")
        verify(episodeDao).deleteAll(episodes)
    }

    @Test
    fun `deleteEpisodeWithoutSyncBlocking cleans up transcripts`() = runTest {
        val episode = PodcastEpisode(uuid = "episode-1", publishedDate = Date()).apply {
            downloadedFilePath = null
        }
        val playbackManager = mock<PlaybackManager>()

        episodeManagerImpl.deleteEpisodeWithoutSyncBlocking(episode, playbackManager)

        verify(transcriptDao).deleteForEpisode("episode-1")
        verify(episodeDao).deleteBlocking(episode)
    }
}
