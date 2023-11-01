package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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

    private lateinit var endOfYearManagerImpl: EndOfYearManagerImpl

    @Before
    fun setup() = runTest {
        whenever(episodeManager.findListenedNumbers(anyLong(), anyLong())).thenReturn(ListenedNumbers())
        whenever(podcastManager.findTopPodcasts(anyLong(), anyLong(), anyInt())).thenReturn(emptyList())
        whenever(episodeManager.findListenedCategories(anyLong(), anyLong())).thenReturn(emptyList())
        whenever(episodeManager.yearOverYearListeningTime(anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(
            YearOverYearListeningTime(0, 0)
        )
        whenever(episodeManager.countEpisodesStartedAndCompleted(anyLong(), anyLong())).thenReturn(
            EpisodesStartedAndCompleted(0, 0)
        )
        endOfYearManagerImpl = EndOfYearManagerImpl(
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            historyManager = historyManager,
            syncManager = syncManager
        )
    }

    @Test
    fun testCompletionRateStory() = runTest {
        whenever(episodeManager.countEpisodesStartedAndCompleted(anyLong(), anyLong())).thenReturn(
            EpisodesStartedAndCompleted(100, 50)
        )
        val stories = endOfYearManagerImpl.loadStories()

        val storyCompletionRate = stories.firstOrNull { it is StoryCompletionRate } as? StoryCompletionRate
        assertTrue(storyCompletionRate?.episodesStartedAndCompleted?.percentage == 50.0)
    }
}
