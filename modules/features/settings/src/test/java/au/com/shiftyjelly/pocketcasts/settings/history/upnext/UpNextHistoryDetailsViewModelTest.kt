package au.com.shiftyjelly.pocketcasts.settings.history.upnext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryDetailsViewModel.UiState
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class UpNextHistoryDetailsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var upNextHistoryManager: UpNextHistoryManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: UpNextHistoryDetailsViewModel
    private val episodes: List<BaseEpisode> = listOf(PodcastEpisode("uuid1", publishedDate = Date()), PodcastEpisode("uuid2", publishedDate = Date()))

    @Before
    fun setUp() {
        val userSettingsArtworkConfigurationMock = mock<UserSetting<ArtworkConfiguration>>()
        whenever(userSettingsArtworkConfigurationMock.value).thenReturn(ArtworkConfiguration(true, emptySet()))
        whenever(settings.artworkConfiguration).thenReturn(userSettingsArtworkConfigurationMock)
        whenever(savedStateHandle.get<Long>(anyString())).thenReturn(123456789L)
        val upNextQueue = mock<UpNextQueue>()
        whenever(upNextQueue.queueEpisodes).thenReturn(emptyList<BaseEpisode>())
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
    }

    @Test
    fun `given init vm, when load episodes is successful, then updates state to loaded`() = runTest {
        val episodeUuids = episodes.map { it.uuid }
        whenever(episodeManager.findEpisodesByUuids(episodeUuids)).thenReturn(episodes)
        whenever(upNextHistoryManager.findEpisodeUuidsForDate(anyOrNull())).thenReturn(episodeUuids)

        initViewModel()

        viewModel.state.test {
            val state = awaitItem() as UiState.Loaded
            assertEquals(episodes, state.episodes)
        }
    }

    @Test
    fun `given init vm, when load episodes fails, then updates state to error`() = runTest {
        whenever(upNextHistoryManager.findEpisodeUuidsForDate(anyOrNull())).thenThrow(RuntimeException::class.java)

        initViewModel()

        viewModel.state.test {
            assertTrue(awaitItem() is UiState.Error)
        }
    }

    @Test
    fun `given vm init vm, when restore up next is successful, then episodes added at the end of playback queue`() = runTest {
        val episodeUuids = episodes.map { it.uuid }
        whenever(episodeManager.findEpisodesByUuids(episodeUuids)).thenReturn(episodes)
        whenever(upNextHistoryManager.findEpisodeUuidsForDate(anyOrNull())).thenReturn(episodeUuids)
        initViewModel()

        viewModel.state.test {
            viewModel.restoreUpNext()
            awaitItem()
            verify(playbackManager).playEpisodesLast(episodes, SourceView.UP_NEXT_HISTORY)
        }
    }

    private fun initViewModel() {
        viewModel = UpNextHistoryDetailsViewModel(
            upNextHistoryManager = upNextHistoryManager,
            episodeManager = episodeManager,
            playbackManager = playbackManager,
            settings = settings,
            savedStateHandle = savedStateHandle,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}
