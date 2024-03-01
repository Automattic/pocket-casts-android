package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    fun `select chapter removes element`() = runTest {
        whenever(episode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2, 3)))

        episodeManagerImpl.selectChapterIndexForEpisode(1, episode)

        verify(episode).deselectedChapters = ChapterIndices(listOf(2, 3))
    }

    @Test
    fun `deselect chapter adds element`() = runTest {
        whenever(episode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2)))

        episodeManagerImpl.deselectChapterIndexForEpisode(3, episode)

        verify(episode).deselectedChapters = ChapterIndices(listOf(1, 2, 3))
    }

    @Test
    fun `deselect chapter is not added twice`() = runTest {
        whenever(episode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2, 3)))

        episodeManagerImpl.deselectChapterIndexForEpisode(3, episode)

        verify(episodeDao, never()).update(any())
    }
}
