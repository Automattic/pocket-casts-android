package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class EndOfYearManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var episodeManager: EpisodeManager

    @Mock
    lateinit var podcastManager: PodcastManager

    @Mock
    lateinit var historyManager: HistoryManager

    @Mock
    lateinit var syncManager: SyncManager

    @Mock
    lateinit var settings: Settings

    private lateinit var endOfYearManagerImpl: EndOfYearManagerImpl

    @Test
    fun testCompletionRateStory() = runTest {
        initEndOfYearManager()
        whenever(episodeManager.countEpisodesStartedAndCompleted(anyLong(), anyLong())).thenReturn(
            EpisodesStartedAndCompleted(100, 50)
        )
        val stories = endOfYearManagerImpl.loadStories()

        val storyCompletionRate = stories.firstOrNull { it is StoryCompletionRate } as? StoryCompletionRate
        assertTrue(storyCompletionRate?.episodesStartedAndCompleted?.percentage == 50.0)
    }

    /* Sync */
    @Test
    fun testRequiresNoSyncWhenServerCountEqualsLocalCount() = runTest {
        initEndOfYearManager(
            userTier = listOf(UserTier.Free, UserTier.Plus).random(),
            listeningHistorySyncLocalCount = 10,
            listeningHistorySyncServerCount = 10,
        )

        endOfYearManagerImpl.downloadListeningHistory {}

        verify(historyManager, times(0))
            .processServerResponse(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun testRequiresSyncWhenServerCountGreaterThanLocalCount() = runTest {
        initEndOfYearManager(
            userTier = listOf(UserTier.Free, UserTier.Plus).random(),
            listeningHistorySyncLocalCount = 5,
            listeningHistorySyncServerCount = 10,
        )

        endOfYearManagerImpl.downloadListeningHistory {}

        verify(historyManager, atLeast(1))
            .processServerResponse(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun testSyncWhenFreeUser() = runTest {
        initEndOfYearManager(userTier = UserTier.Free)

        endOfYearManagerImpl.downloadListeningHistory {}

        with(syncManager) {
            verify(this).historyYear(2023, false) // synced for this year
            verify(this, times(0)).historyYear(2022, false) // not synced for last year
        }
    }

    @Test
    fun testSyncWhenPlusUser() = runTest {
        initEndOfYearManager(userTier = UserTier.Plus)

        endOfYearManagerImpl.downloadListeningHistory {}

        with(syncManager) {
            verify(this).historyYear(2023, false) // synced for this year
            verify(this).historyYear(2022, false) // synced for last year
        }
    }

    private fun initEndOfYearManager(
        userTier: UserTier = UserTier.Free,
        listeningHistorySyncLocalCount: Int = 5,
        listeningHistorySyncServerCount: Long = 10,
    ) = runTest {
        whenever(settings.userTier).thenReturn(userTier)
        whenever(episodeManager.findListenedNumbers(anyLong(), anyLong())).thenReturn(ListenedNumbers())
        whenever(podcastManager.findTopPodcasts(anyLong(), anyLong(), anyInt())).thenReturn(emptyList())
        whenever(episodeManager.findListenedCategories(anyLong(), anyLong())).thenReturn(emptyList())
        whenever(episodeManager.yearOverYearListeningTime(anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(
            YearOverYearListeningTime(0, 0)
        )
        whenever(episodeManager.countEpisodesStartedAndCompleted(anyLong(), anyLong())).thenReturn(
            EpisodesStartedAndCompleted(0, 0)
        )
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        val historyCountResponse = mock<HistoryYearResponse>()
        whenever(historyCountResponse.count).thenReturn(listeningHistorySyncServerCount) // set server count
        whenever(historyCountResponse.history).thenReturn(mock())
        whenever(syncManager.historyYear(anyInt(), anyBoolean())).thenReturn(historyCountResponse)
        whenever(episodeManager.countEpisodesInListeningHistory(anyLong(), anyLong())).thenReturn(listeningHistorySyncLocalCount) // set local count

        endOfYearManagerImpl = EndOfYearManagerImpl(
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            historyManager = historyManager,
            syncManager = syncManager,
            settings = settings,
        )
    }
}
