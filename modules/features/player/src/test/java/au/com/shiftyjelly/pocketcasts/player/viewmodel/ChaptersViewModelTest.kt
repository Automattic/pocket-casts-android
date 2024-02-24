package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.InMemoryFeatureProvider
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChaptersViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var theme: Theme

    @Mock
    private lateinit var upNextQueue: UpNextQueue

    private lateinit var chaptersViewModel: ChaptersViewModel

    @Before
    fun setUp() {
        FeatureFlag.initialize(
            listOf(object : InMemoryFeatureProvider() {}),
        )
    }

    @Test
    fun `given unselected chapter contains playback pos, then skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150)

        verify(playbackManager).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given selected chapter contains playback pos, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 50)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given user seeking playback pos, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150, lastChangeFrom = PlaybackManager.LastChangeFrom.OnUserSeeking.value)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given seek complete, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150, lastChangeFrom = PlaybackManager.LastChangeFrom.OnSeekComplete.value)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given feature flag off, then chapter is not skipped`() = runTest {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    private fun initChapters() =
        Chapters(
            listOf(
                Chapter("1", 0, 100, selected = true),
                Chapter("2", 101, 200, selected = false),
                Chapter("3", 201, 300, selected = true),
            ),
        )

    private fun initViewModel() {
        whenever(playbackManager.playbackStateRelay)
            .thenReturn(BehaviorRelay.create<PlaybackState>().toSerialized())
        whenever(upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager))
            .thenReturn(BehaviorRelay.create<UpNextQueue.State>().toSerialized())
        whenever(playbackManager.upNextQueue)
            .thenReturn(upNextQueue)

        chaptersViewModel = ChaptersViewModel(
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            playbackManager = playbackManager,
            theme = theme,
        )
    }
}
