package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class EpisodeManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var fileStorage: FileStorage

    @Mock
    lateinit var downloadManager: DownloadManager

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appDatabase: AppDatabase

    @Mock
    lateinit var podcastCacheServerManager: PodcastCacheServerManager

    @Mock
    lateinit var userEpisodeManager: UserEpisodeManager

    @Mock
    lateinit var ioDispatcher: CoroutineDispatcher

    @Mock
    lateinit var episodeAnalytics: EpisodeAnalytics

    @Mock
    lateinit var episodeDao: EpisodeDao

    private lateinit var episodeManagerImpl: EpisodeManagerImpl

    @Before
    fun setUp() {
        whenever(appDatabase.episodeDao()).thenReturn(episodeDao)
        episodeManagerImpl = EpisodeManagerImpl(
            settings = settings,
            fileStorage = fileStorage,
            downloadManager = downloadManager,
            context = context,
            appDatabase = appDatabase,
            podcastCacheServerManager = podcastCacheServerManager,
            userEpisodeManager = userEpisodeManager,
            ioDispatcher = ioDispatcher,
            episodeAnalytics = episodeAnalytics,
        )
    }

    @Test
    fun `select chapter removes element`() = runTest {
        whenever(episodeDao.findDeselectedChaptersByEpisodeId(anyString()))
            .thenReturn("1,2,3")

        episodeManagerImpl.selectChapterForEpisodeId(1, "")

        verify(episodeDao).updateDeselectedChaptersForEpisodeId("2,3", "")
    }

    @Test
    fun `deselect chapter adds element`() = runTest {
        whenever(episodeDao.findDeselectedChaptersByEpisodeId(anyString()))
            .thenReturn("1,2")

        episodeManagerImpl.deselectChapterForEpisodeId(3, "")

        verify(episodeDao).updateDeselectedChaptersForEpisodeId("1,2,3", "")
    }

    @Test
    fun `deselect chapter is not added twice`() = runTest {
        whenever(episodeDao.findDeselectedChaptersByEpisodeId(anyString()))
            .thenReturn("1,2,3")

        episodeManagerImpl.deselectChapterForEpisodeId(3, "")

        verify(episodeDao, never()).updateDeselectedChaptersForEpisodeId(any(), any())
    }
}
