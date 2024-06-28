package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
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
import org.mockito.kotlin.doReturnConsecutively
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
    lateinit var episode: PodcastEpisode

    private lateinit var episodeManagerImpl: EpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.episodeDao()).thenReturn(episodeDao)
        episodeManagerImpl = EpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            fileStorage = mock(),
            downloadManager = mock(),
            context = mock(),
            podcastCacheServerManager = mock(),
            userEpisodeManager = mock(),
            ioDispatcher = mock(),
            episodeAnalytics = mock(),
        )
    }

    @Test
    fun `get all podcasts episodes`() = runTest {
        val episodes = List(26) { PodcastEpisode(uuid = "$it", publishedDate = Date()) }
        episodeDao.stub {
            onBlocking { getAllPodcastEpisodes(any(), any()) } doReturnConsecutively (episodes.chunked(10) + listOf(emptyList()))
        }

        episodeManagerImpl.getAllPodcastEpisodes(10).test {
            episodes.forEachIndexed { index, episode ->
                assertEquals(episode to index, awaitItem())
            }

            expectNoEvents()
        }

        verify(episodeDao).getAllPodcastEpisodes(10, 0)
        verify(episodeDao).getAllPodcastEpisodes(10, 10)
        verify(episodeDao).getAllPodcastEpisodes(10, 20)
    }
}
