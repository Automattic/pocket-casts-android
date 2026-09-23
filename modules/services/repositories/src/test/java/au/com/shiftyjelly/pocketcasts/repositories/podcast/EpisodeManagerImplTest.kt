package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

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
    lateinit var context: Context

    @Mock
    lateinit var podcastCacheServiceManager: PodcastCacheServiceManager

    @Mock
    lateinit var userEpisodeManager: UserEpisodeManager

    private lateinit var episodeManagerImpl: EpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.episodeDao()).thenReturn(episodeDao)
        whenever(appDatabase.userEpisodeDao()).thenReturn(mock())
        episodeManagerImpl = EpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            downloadQueue = mock(),
            context = context,
            podcastCacheServiceManager = podcastCacheServiceManager,
            userEpisodeManager = userEpisodeManager,
            ioDispatcher = coroutineRule.testDispatcher,
            eventHorizon = EventHorizon(TestEventSink()),
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
    fun `download missing episode returns the local episode without calling the server`() = runTest {
        val episode = createEpisode()
        whenever(episodeDao.exists("episode1")).thenReturn(true)
        whenever(episodeDao.findByUuid("episode1")).thenReturn(episode)

        val result = downloadMissingEpisode(podcastUuid = "podcast1")

        assertEquals(episode, result)
        verify(podcastCacheServiceManager, never()).getPodcastAndEpisode(any(), any())
    }

    @Test
    fun `download missing episode inserts the server episode`() = runTest {
        val serverEpisode = createEpisode()
        val podcast = Podcast(uuid = "podcast1").apply { episodes.add(serverEpisode) }
        whenever(episodeDao.exists("episode1")).thenReturn(false)
        whenever(podcastCacheServiceManager.getPodcastAndEpisode("podcast1", "episode1")).thenReturn(podcast)
        episodeDao.stub {
            on { findByUuid("episode1") } doReturnConsecutively listOf(null, serverEpisode)
        }

        val result = downloadMissingEpisode(podcastUuid = "podcast1")

        assertEquals(serverEpisode, result)
        verify(episodeDao).insertAllOrIgnore(listOf(serverEpisode))
    }

    @Test
    fun `download missing episode inserts the skeleton when the server does not know the episode`() = runTest {
        val skeletonEpisode = PodcastEpisode(uuid = "episode1", publishedDate = Date())
        whenever(episodeDao.exists("episode1")).thenReturn(false)
        whenever(podcastCacheServiceManager.getPodcastAndEpisode("podcast1", "episode1")).thenReturn(Podcast(uuid = "podcast1"))
        episodeDao.stub {
            on { findByUuid("episode1") } doReturnConsecutively listOf(null, skeletonEpisode)
        }

        val result = downloadMissingEpisode(podcastUuid = "podcast1", skeletonEpisode = skeletonEpisode)

        assertEquals(skeletonEpisode, result)
        assertEquals("podcast1", skeletonEpisode.podcastUuid)
        verify(episodeDao).insertAllOrIgnore(listOf(skeletonEpisode))
    }

    @Test
    fun `download missing episode propagates server errors`() = runTest {
        whenever(episodeDao.exists("episode1")).thenReturn(false)
        whenever(podcastCacheServiceManager.getPodcastAndEpisode("podcast1", "episode1"))
            .thenThrow(HttpException(Response.error<Any>(404, "".toResponseBody())))

        val result = runCatching { downloadMissingEpisode(podcastUuid = "podcast1") }

        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `download missing episode returns null when a user episode cannot be found`() = runTest {
        whenever(episodeDao.exists("episode1")).thenReturn(false)
        whenever(episodeDao.findByUuid("episode1")).thenReturn(null)
        whenever(userEpisodeManager.findEpisodeByUuid("episode1")).thenReturn(null)

        val result = downloadMissingEpisode(podcastUuid = Podcast.userPodcast.uuid)

        assertNull(result)
        verify(podcastCacheServiceManager, never()).getPodcastAndEpisode(any(), any())
    }

    @Test
    fun `download missing episode finishes the insert when the caller is cancelled`() = runTest {
        val serverEpisode = createEpisode()
        val podcast = Podcast(uuid = "podcast1").apply { episodes.add(serverEpisode) }
        whenever(episodeDao.exists("episode1")).thenReturn(false)
        whenever(podcastCacheServiceManager.getPodcastAndEpisode("podcast1", "episode1")).thenReturn(podcast)
        whenever(episodeDao.insertAllOrIgnore(any())).doSuspendableAnswer { delay(1_000) }
        episodeDao.stub {
            on { findByUuid("episode1") } doReturnConsecutively listOf(null, serverEpisode)
        }

        var result: BaseEpisode? = null
        val job = launch { result = downloadMissingEpisode(podcastUuid = "podcast1") }
        advanceTimeBy(500)
        job.cancel()
        advanceUntilIdle()

        verify(episodeDao, times(2)).findByUuid("episode1")
        assertTrue(job.isCancelled)
        assertNull(result)
    }

    private fun createEpisode() = PodcastEpisode(uuid = "episode1", podcastUuid = "podcast1", publishedDate = Date())

    private suspend fun downloadMissingEpisode(
        podcastUuid: String,
        skeletonEpisode: PodcastEpisode = createEpisode(),
    ) = episodeManagerImpl.downloadMissingEpisode(
        episodeUuid = "episode1",
        podcastUuid = podcastUuid,
        skeletonEpisode = skeletonEpisode,
        downloadMetaData = false,
    )
}
